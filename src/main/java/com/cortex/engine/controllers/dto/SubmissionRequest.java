package com.cortex.engine.controllers.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmissionRequest(
    @NotBlank(message = "Code cannot be blank") String code,
    @NotBlank(message = "Language cannot be blank") String language,
    String stdin,
    Float cpuTimeLimit,
    Float cpuExtraTime,
    String commandLineArguments,
    String compilerOptions,
    Boolean encodeOutputToBase64
) {
  public SubmissionRequest {
    encodeOutputToBase64 = encodeOutputToBase64 == null || encodeOutputToBase64;
  }
}