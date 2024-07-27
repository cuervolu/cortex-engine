package com.cortex.engine.exceptions;

public class ContainerStartException extends RuntimeException {
  public ContainerStartException(String message) {
    super(message);
  }

  public ContainerStartException(String message, Throwable cause) {
    super(message, cause);
  }
}