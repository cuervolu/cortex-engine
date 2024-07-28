package com.cortex.engine.exceptions;

import static com.cortex.engine.exceptions.BusinessErrorCodes.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CodeExecutionException.class)
  public ResponseEntity<ExceptionResponse> handleException(CodeExecutionException exp) {
    return ResponseEntity.status(CODE_EXECUTION_ERROR.getHttpStatus())
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(CODE_EXECUTION_ERROR.getCode())
                .businessErrorDescription(CODE_EXECUTION_ERROR.getDescription())
                .error(exp.getMessage())
                .build());
  }

  @ExceptionHandler(UnsupportedLanguageException.class)
  public ResponseEntity<ExceptionResponse> handleException(UnsupportedLanguageException exp) {
    return ResponseEntity.status(UNSUPPORTED_LANGUAGE.getHttpStatus())
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(UNSUPPORTED_LANGUAGE.getCode())
                .businessErrorDescription(UNSUPPORTED_LANGUAGE.getDescription())
                .error(exp.getMessage())
                .build());
  }

  @ExceptionHandler(ContainerCreationException.class)
  public ResponseEntity<ExceptionResponse> handleException(ContainerCreationException exp) {
    return ResponseEntity.status(CONTAINER_CREATION_ERROR.getHttpStatus())
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(CONTAINER_CREATION_ERROR.getCode())
                .businessErrorDescription(CONTAINER_CREATION_ERROR.getDescription())
                .error(exp.getMessage())
                .build());
  }

  @ExceptionHandler(ContainerStartException.class)
  public ResponseEntity<ExceptionResponse> handleException(ContainerStartException exp) {
    return ResponseEntity.status(CONTAINER_START_ERROR.getHttpStatus())
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(CONTAINER_START_ERROR.getCode())
                .businessErrorDescription(CONTAINER_START_ERROR.getDescription())
                .error(exp.getMessage())
                .build());
  }

  @ExceptionHandler(CodeCompilationException.class)
  public ResponseEntity<ExceptionResponse> handleException(CodeCompilationException exp) {
    return ResponseEntity.status(CODE_COMPILATION_ERROR.getHttpStatus())
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(CODE_COMPILATION_ERROR.getCode())
                .businessErrorDescription(CODE_COMPILATION_ERROR.getDescription())
                .error(exp.getMessage())
                .build());
  }

  @ExceptionHandler(ExecutionTimeoutException.class)
  public ResponseEntity<ExceptionResponse> handleException(ExecutionTimeoutException exp) {
    return ResponseEntity.status(EXECUTION_TIMEOUT.getHttpStatus())
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(EXECUTION_TIMEOUT.getCode())
                .businessErrorDescription(EXECUTION_TIMEOUT.getDescription())
                .error(exp.getMessage())
                .build());
  }

  @ExceptionHandler(FileOperationException.class)
  public ResponseEntity<ExceptionResponse> handleException(FileOperationException exp) {
    return ResponseEntity.status(FILE_OPERATION_ERROR.getHttpStatus())
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(FILE_OPERATION_ERROR.getCode())
                .businessErrorDescription(FILE_OPERATION_ERROR.getDescription())
                .error(exp.getMessage())
                .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exp) {
    Set<String> errors = new HashSet<>();
    exp.getBindingResult().getAllErrors().forEach(error -> errors.add(error.getDefaultMessage()));

    return ResponseEntity.status(BAD_REQUEST)
        .body(
            ExceptionResponse.builder()
                .businessErrorCode(VALIDATION_ERROR.getCode())
                .businessErrorDescription(VALIDATION_ERROR.getDescription())
                .validationErrors(errors)
                .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionResponse> handleException(Exception exp) {
    log.error("Internal error", exp);
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body(
            ExceptionResponse.builder()
                .businessErrorDescription("Internal error, please contact support")
                .error(exp.getMessage())
                .build());
  }
}
