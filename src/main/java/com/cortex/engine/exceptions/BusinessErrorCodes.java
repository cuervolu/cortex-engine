package com.cortex.engine.exceptions;

import static org.springframework.http.HttpStatus.*;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessErrorCodes {
  CODE_EXECUTION_ERROR(305, INTERNAL_SERVER_ERROR, "Error occurred during code execution"),
  UNSUPPORTED_LANGUAGE(306, BAD_REQUEST, "Unsupported programming language"),
  CONTAINER_CREATION_ERROR(307, INTERNAL_SERVER_ERROR, "Failed to create Docker container"),
  CONTAINER_START_ERROR(308, INTERNAL_SERVER_ERROR, "Failed to start Docker container"),
  CODE_COMPILATION_ERROR(309, BAD_REQUEST, "Code compilation failed"),
  EXECUTION_TIMEOUT(310, REQUEST_TIMEOUT, "Code execution timed out"),
  FILE_OPERATION_ERROR(311, INTERNAL_SERVER_ERROR, "Error in file operation"),
  VALIDATION_ERROR(400, BAD_REQUEST, "Validation error occurred");

  private final int code;
  private final String description;
  private final HttpStatus httpStatus;

  BusinessErrorCodes(int code, HttpStatus status, String description) {
    this.code = code;
    this.description = description;
    this.httpStatus = status;
  }
}
