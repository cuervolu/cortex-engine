package com.cortex.engine.services;

import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.cortex.engine.controllers.dto.SubmissionRequest;
import com.cortex.engine.docker.AutoCloseableContainer;
import com.cortex.engine.entities.Language;
import com.cortex.engine.entities.Submission;
import com.cortex.engine.exceptions.*;
import com.cortex.engine.repositories.LanguageRepository;
import com.cortex.engine.repositories.SubmissionRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for executing code submissions in isolated Docker containers.
 *
 * <p>This service handles the creation of Docker containers, execution of code within these
 * containers, and processing of the execution results. It supports multiple programming languages
 * and provides a secure environment for code execution.
 *
 * @author Ángel Cuervo
 * @version 1.0
 * @since 2024-07-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

  private final DockerClient dockerClient;
  private final LanguageRepository languageRepository;
  private final SubmissionRepository submissionRepository;

  /**
   * Executes the submitted code in a Docker container.
   *
   * @param request The code submission request containing the code and execution parameters.
   * @return The execution result wrapped in an ExecutionResponse object.
   * @throws CodeExecutionException If an error occurs during code execution.
   * @throws UnsupportedLanguageException If the specified programming language is not supported.
   */
  public ExecutionResponse executeCode(SubmissionRequest request) throws CodeExecutionException {
    Language language =
        languageRepository
            .findByName(request.language())
            .orElseThrow(
                () ->
                    new UnsupportedLanguageException(
                        "Unsupported language: " + request.language()));

    Path codePath = null;
    Path stdinPath = null;
    try {
      codePath =
          createTempFile(
              "code" + language.getFileExtension(),
              new String(Base64.getDecoder().decode(request.code()), StandardCharsets.UTF_8));
      stdinPath = createTempFile("stdin.txt", request.stdin());

      try (AutoCloseableContainer container =
          new AutoCloseableContainer(
              createContainer(language, codePath, stdinPath), dockerClient)) {
        startContainer(container);

        ExecutionResult result =
            executeCodeInContainer(
                container,
                language,
                codePath.getFileName().toString(),
                stdinPath.getFileName().toString());
        saveSubmission(request, language);

        String stdout = encodeIfRequired(result.stdout, request.encodeOutputToBase64());
        String stderr =
            result.stderr.isEmpty()
                ? null
                : encodeIfRequired(result.stderr, request.encodeOutputToBase64());

        return new ExecutionResponse(stdout, result.statusId, stderr);
      }
    } catch (ContainerCreationException | ContainerStartException | ExecutionTimeoutException e) {
      throw e;
    } catch (IOException e) {
      throw new FileOperationException("Error in file operation: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new CodeExecutionException("Failed to execute code: " + e.getMessage(), e);
    } finally {
      deleteTemporaryFile(codePath);
      deleteTemporaryFile(stdinPath);
    }
  }

  /**
   * Creates a Docker container for code execution.
   *
   * @param language The programming language of the code to be executed.
   * @param codePath The path to the file containing the code.
   * @param stdinPath The path to the file containing standard input.
   * @return A CreateContainerResponse object representing the created container.
   * @throws ContainerCreationException If the container creation fails.
   */
  private CreateContainerResponse createContainer(
      Language language, Path codePath, Path stdinPath) {
    Volume codeVolume = new Volume("/code");
    Volume stdinVolume = new Volume("/stdin");

    try (CreateContainerCmd containerCmd =
        dockerClient.createContainerCmd(language.getDockerImage())) {
      return containerCmd
          .withHostConfig(
              HostConfig.newHostConfig()
                  .withBinds(
                      new Bind(codePath.getParent().toString(), codeVolume),
                      new Bind(stdinPath.getParent().toString(), stdinVolume))
                  .withMemory(language.getDefaultMemoryLimit())
                  .withCpuCount(language.getDefaultCpuLimit()))
          .withCmd("tail", "-f", "/dev/null") // Keep container running
          .withWorkingDir("/code")
          .withTty(true)
          .withAttachStderr(true)
          .withAttachStdout(true)
          .exec();
    } catch (Exception e) {
      throw new ContainerCreationException(
          "Failed to create Docker container: " + e.getMessage(), e);
    }
  }

  /**
   * Starts the Docker container.
   *
   * @param container The container to be started.
   * @throws ContainerStartException If the container fails to start.
   */
  private void startContainer(AutoCloseableContainer container) {
    try {
      dockerClient.startContainerCmd(container.getContainer().getId()).exec();
    } catch (Exception e) {
      throw new ContainerStartException("Failed to start Docker container: " + e.getMessage(), e);
    }
  }

  /**
   * Executes the code inside the Docker container.
   *
   * @param container The container in which to execute the code.
   * @param language The programming language of the code.
   * @param codeFileName The name of the file containing the code.
   * @param stdinFileName The name of the file containing standard input.
   * @return An ExecutionResult object containing the execution output and status.
   * @throws ExecutionTimeoutException If the code execution times out.
   */
  private ExecutionResult executeCodeInContainer(
      AutoCloseableContainer container,
      Language language,
      String codeFileName,
      String stdinFileName)
      throws ExecutionTimeoutException {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    try {
      String executeCommand = buildCommand(language, codeFileName, stdinFileName);
      ExecCreateCmdResponse execCreateCmdResponse =
          dockerClient
              .execCreateCmd(container.getContainer().getId())
              .withAttachStdout(true)
              .withAttachStderr(true)
              .withCmd("/bin/sh", "-c", executeCommand)
              .exec();

      dockerClient
          .execStartCmd(execCreateCmdResponse.getId())
          .exec(
              new Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                  byte[] payload = frame.getPayload();
                  if (payload != null) {
                    try {
                      if (frame.getStreamType() == StreamType.STDOUT) {
                        stdout.write(payload);
                      } else if (frame.getStreamType() == StreamType.STDERR) {
                        stderr.write(payload);
                      }
                    } catch (IOException e) {
                      log.error("Error writing to output stream", e);
                    }
                  }
                }
              })
          .awaitCompletion(language.getDefaultTimeout(), TimeUnit.MILLISECONDS);

      Long exitCode =
          dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCodeLong();
      int statusId = (exitCode != null && exitCode == 0) ? 3 : 4;

      return new ExecutionResult(
          stdout.toString(StandardCharsets.UTF_8),
          stderr.toString(StandardCharsets.UTF_8),
          statusId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ExecutionTimeoutException("Code execution timed out", e);
    } catch (Exception e) {
      log.error("Error executing code in container", e);
      return new ExecutionResult("", e.getMessage(), 4);
    }
  }

  /**
   * Encodes the input string to Base64 if required.
   *
   * @param input The input string to potentially encode.
   * @param shouldEncode Whether the input should be encoded.
   * @return The encoded string if shouldEncode is true, otherwise the original input.
   */
  private String encodeIfRequired(String input, boolean shouldEncode) {
    if (shouldEncode) {
      return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    return input;
  }

  /**
   * Saves the submission details to the database.
   *
   * @param request The submission request.
   * @param language The programming language of the submission.
   */
  private void saveSubmission(SubmissionRequest request, Language language) {
    Submission submission =
        Submission.builder()
            .code(request.code())
            .language(language)
            .stdin(request.stdin())
            .cpuTimeLimit(request.cpuTimeLimit())
            .cpuExtraTime(request.cpuExtraTime())
            .commandLineArguments(request.commandLineArguments())
            .compilerOptions(request.compilerOptions())
            .createdBy(1L)
            .build();
    submissionRepository.save(submission);
  }

  /**
   * Builds the command to execute the code in the container.
   *
   * @param language The programming language of the code.
   * @param codeFileName The name of the file containing the code.
   * @param stdinFileName The name of the file containing standard input.
   * @return The command string to execute the code.
   */
  private String buildCommand(Language language, String codeFileName, String stdinFileName) {
    String executeCommand = language.getExecuteCommand().replace("{fileName}", codeFileName);
    return "cat /stdin/" + stdinFileName + " | " + executeCommand;
  }

  /**
   * Creates a temporary file with the given content.
   *
   * @param fileName The name of the file to create.
   * @param content The content to write to the file.
   * @return The Path object representing the created file.
   * @throws IOException If an I/O error occurs.
   */
  private Path createTempFile(String fileName, String content) throws IOException {
    Path tempFile = Files.createTempFile(null, fileName);
    Files.writeString(tempFile, content, StandardCharsets.UTF_8);
    return tempFile;
  }

  /**
   * Deletes a temporary file.
   *
   * @param path The path of the file to delete.
   */
  private void deleteTemporaryFile(Path path) {
    if (path != null) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        log.error("Failed to delete temporary file: {}", e.getMessage());
      }
    }
  }

  /** Represents the result of code execution. */
  private record ExecutionResult(String stdout, String stderr, int statusId) {}
}
