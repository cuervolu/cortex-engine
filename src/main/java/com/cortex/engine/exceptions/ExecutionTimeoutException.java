package com.cortex.engine.exceptions;

public class ExecutionTimeoutException extends RuntimeException {
  public ExecutionTimeoutException(String message) {
    super(message);
  }

  public ExecutionTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
