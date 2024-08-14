package com.cortex.engine.config;

import com.cortex.engine.entities.Language;
import com.cortex.engine.repositories.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LanguageInitializer implements CommandLineRunner {

  private final LanguageRepository languageRepository;

  @Override
  public void run(String... args) {
    if (languageRepository.count() == 0) {
      List<Language> predefinedLanguages =
          Arrays.asList(
              Language.builder()
                  .name("python")
                  .dockerImage("python:3.12-slim")
                  .executeCommand("python {fileName}")
                  .fileExtension(".py")
                  .defaultMemoryLimit(128 * 1024 * 1024L) // 128 MB
                  .defaultCpuLimit(1L)
                  .defaultTimeout(5000L) // 5 seconds
                  .createdBy(1L)
                  .build(),
              Language.builder()
                  .name("java")
                  .dockerImage("eclipse-temurin:21")
                  .executeCommand("java {fileName}")
                  .compileCommand("javac {fileName}")
                  .fileExtension(".java")
                  .defaultMemoryLimit(256 * 1024 * 1024L) // 256 MB
                  .defaultCpuLimit(1L)
                  .defaultTimeout(10000L) // 10 seconds
                  .createdBy(1L)
                  .build(),
              Language.builder()
                  .name("javascript")
                  .dockerImage("node:20-alpine3.19")
                  .executeCommand("node {fileName}")
                  .fileExtension(".js")
                  .defaultMemoryLimit(128 * 1024 * 1024L) // 128 MB
                  .defaultCpuLimit(1L)
                  .defaultTimeout(5000L) // 5 seconds
                  .createdBy(1L)
                  .build(),
              Language.builder()
                  .name("rust")
                  .dockerImage("rust:1.80-slim")
                  .executeCommand("rustc {fileName} && ./{fileNameWithoutExtension}")
                  .fileExtension(".rs")
                  .defaultMemoryLimit(256 * 1024 * 1024L) // 256 MB
                  .defaultCpuLimit(1L)
                  .defaultTimeout(15000L) // 15 seconds
                  .createdBy(1L)
                  .build(),
              Language.builder()
                  .name("csharp")
                  .dockerImage("mcr.microsoft.com/dotnet/sdk:8.0")
                  .executeCommand(
                      "dotnet new console -o . && mv {fileName} Program.cs && dotnet run")
                  .fileExtension(".cs")
                  .defaultMemoryLimit(512 * 1024 * 1024L) // 512 MB
                  .defaultCpuLimit(2L)
                  .defaultTimeout(30000L) // 30 seconds
                  .createdBy(1L)
                  .build(),
              Language.builder()
                  .name("go")
                  .dockerImage("golang:1.22-bookworm")
                  .executeCommand("go run {fileName}")
                  .fileExtension(".go")
                  .defaultMemoryLimit(256 * 1024 * 1024L) // 256 MB
                  .defaultCpuLimit(1L)
                  .defaultTimeout(10000L) // 10 seconds
                  .createdBy(1L)
                  .build());

      languageRepository.saveAll(predefinedLanguages);
    }
  }
}
