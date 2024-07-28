package com.cortex.engine.services.impl;

import com.cortex.engine.controllers.dto.CodeExecutionTask;
import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.cortex.engine.controllers.dto.SubmissionRequest;
import com.cortex.engine.docker.AutoCloseableContainer;
import com.cortex.engine.entities.Language;
import com.cortex.engine.entities.Submission;
import com.cortex.engine.exceptions.*;
import com.cortex.engine.repositories.LanguageRepository;
import com.cortex.engine.repositories.SubmissionRepository;
import com.cortex.engine.services.ICodeExecutionService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Service implementation responsible for executing code submissions in isolated Docker containers.
 *
 * <p>This service handles the creation of Docker containers, execution of code within these
 * containers, and processing of the execution results. It supports multiple programming languages
 * and provides a secure environment for code execution.
 *
 * <p>Key features include:
 *
 * <ul>
 *   <li>Submitting code execution tasks to a message queue
 *   <li>Retrieving execution results from a Redis cache
 *   <li>Creating and managing Docker containers for code execution
 *   <li>Supporting multiple programming languages
 *   <li>Handling file operations for code and input
 *   <li>Implementing error handling and logging
 * </ul>
 *
 * @author Ángel Cuervo
 * @version 1.1
 * @since 2024-07-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionServiceImpl implements ICodeExecutionService {

  private static final String CODE_EXECUTION_QUEUE = "codeExecution";
  private static final String RESULT_KEY_PREFIX = "result:";
  private static final long RESULT_EXPIRATION_HOURS = 1;

  private final RabbitTemplate rabbitTemplate;
  private final RedisTemplate<String, ExecutionResponse> redisTemplate;
  private final DockerClient dockerClient;
  private final LanguageRepository languageRepository;
  private final SubmissionRepository submissionRepository;

  @Override
  public String submitCodeExecution(SubmissionRequest request) throws UnsupportedLanguageException {
    // Verificamos si el lenguaje es soportado
    if (!languageRepository.existsByName(request.language())) {
      throw new UnsupportedLanguageException("Unsupported language: " + request.language());
    }

    String taskId = UUID.randomUUID().toString();
    CodeExecutionTask task = new CodeExecutionTask();
    task.setTaskId(taskId);
    task.setSubmissionRequest(request);

    rabbitTemplate.convertAndSend(CODE_EXECUTION_QUEUE, task);

    return taskId;
  }

  @Override
  public ExecutionResponse getExecutionResult(String taskId) throws CodeExecutionException {
    ExecutionResponse result = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + taskId);

    if (result == null) {
      throw new CodeExecutionException("Execution result not available yet");
    }

    return result;
  }

  @Override
  public void processCodeExecution(CodeExecutionTask task) {
    try {
      ExecutionResponse result = executeCode(task.getSubmissionRequest());
      redisTemplate
          .opsForValue()
          .set(
              RESULT_KEY_PREFIX + task.getTaskId(),
              result,
              RESULT_EXPIRATION_HOURS,
              TimeUnit.HOURS);
    } catch (Exception e) {
      log.error("Error processing code execution task", e);
      ExecutionResponse errorResponse = new ExecutionResponse(null, 4, e.getMessage());
      redisTemplate
          .opsForValue()
          .set(
              RESULT_KEY_PREFIX + task.getTaskId(),
              errorResponse,
              RESULT_EXPIRATION_HOURS,
              TimeUnit.HOURS);
    }
  }

  /**
   * Executes the submitted code in a Docker container.
   *
   * @param request The code submission request containing the code and execution parameters.
   * @return The execution result wrapped in an ExecutionResponse object.
   * @throws CodeExecutionException If an error occurs during code execution.
   * @throws UnsupportedLanguageException If the specified programming language is not supported.
   */
  private ExecutionResponse executeCode(SubmissionRequest request) throws CodeExecutionException {
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
              createAndStartContainer(language, codePath, stdinPath), dockerClient)) {
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
  private CreateContainerResponse createAndStartContainer(
      Language language, Path codePath, Path stdinPath) {
    String containerName = "cortex-" + UUID.randomUUID();
    Volume codeVolume = new Volume("/code");
    Volume stdinVolume = new Volume("/stdin");

    try {
      // Verificar si existe un contenedor con el mismo nombre
      List<Container> existingContainers =
          dockerClient
              .listContainersCmd()
              .withShowAll(true)
              .withNameFilter(Collections.singletonList(containerName))
              .exec();

      if (!existingContainers.isEmpty()) {
        String existingContainerId = existingContainers.getFirst().getId();
        log.info("Container with name {} already exists. Removing it.", containerName);
        dockerClient.removeContainerCmd(existingContainerId).withForce(true).exec();
      }

      // Crear un nuevo contenedor
      CreateContainerResponse container =
          dockerClient
              .createContainerCmd(language.getDockerImage())
              .withName(containerName)
              .withHostConfig(
                  HostConfig.newHostConfig()
                      .withBinds(
                          new Bind(codePath.getParent().toString(), codeVolume),
                          new Bind(stdinPath.getParent().toString(), stdinVolume))
                      .withMemory(language.getDefaultMemoryLimit())
                      .withCpuCount(language.getDefaultCpuLimit()))
              .withCmd("tail", "-f", "/dev/null")
              .withWorkingDir("/code")
              .withTty(true)
              .withAttachStderr(true)
              .withAttachStdout(true)
              .exec();

      // Start the container
      dockerClient.startContainerCmd(container.getId()).exec();
      log.info("Container started successfully: {}", container.getId());

      // Install dotnet-script if the language is C#
      if ("csharp".equals(language.getName())) {
        installDotnetScript(container.getId());
      }

      return container;
    } catch (Exception e) {
      throw new ContainerCreationException(
          "Failed to create or start Docker container: " + e.getMessage(), e);
    }
  }

  /**
   * Starts the Docker container.
   *
   * @param container The container to be started.
   * @throws ContainerStartException If the container fails to start.
   */
  private void startContainer(AutoCloseableContainer container) {
    String containerId = container.getContainer().getId();
    try {
      // Check if the container exists and get its state
      InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();

      if (Boolean.TRUE.equals(containerInfo.getState().getRunning())) {
        log.info("Container {} is already in a running state. No action needed.", containerId);
        return;
      }

      // Attempt to start the container
      dockerClient.startContainerCmd(containerId).exec();
      log.info("Container {} started successfully", containerId);
    } catch (NotModifiedException e) {
      // This might occur if the container started between our check and the start command
      log.warn(
          "Attempted to start container {} but it was already running. This might indicate a race condition.",
          containerId);
    } catch (Exception e) {
      log.error("Failed to start or inspect container {}. Error: {}", containerId, e.getMessage());
      throw new ContainerStartException(
          "Failed to start or inspect Docker container: " + e.getMessage(), e);
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

  /**
   * Installs the dotnet-script tool in a Docker container. This method is specifically used for C#
   * code execution.
   *
   * @param containerId The ID of the Docker container where dotnet-script will be installed
   * @throws ContainerCreationException if the installation process fails or is interrupted
   */
  private void installDotnetScript(String containerId) {
    String[] installCommand = {
      "/bin/sh",
      "-c",
      "dotnet tool install -g dotnet-script && export PATH=\"$PATH:/root/.dotnet/tools\""
    };
    try {
      ExecCreateCmdResponse execCreateCmdResponse =
          dockerClient.execCreateCmd(containerId).withCmd(installCommand).exec();

      dockerClient
          .execStartCmd(execCreateCmdResponse.getId())
          .exec(
              new Adapter<Frame>() {
                @Override
                public void onNext(Frame object) {
                  log.info("Installing dotnet-script: {}", object.toString());
                }
              })
          .awaitCompletion(60, TimeUnit.SECONDS);

      log.info("dotnet-script installed successfully in container: {}", containerId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ContainerCreationException("Container creation was interrupted", e);
    } catch (Exception e) {
      throw new ContainerCreationException("Failed to install dotnet-script: " + e.getMessage(), e);
    }
  }

  /** Represents the result of code execution. */
  private record ExecutionResult(String stdout, String stderr, int statusId) {}
}
