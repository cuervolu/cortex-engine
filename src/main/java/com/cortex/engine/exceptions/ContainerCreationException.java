package com.cortex.engine.exceptions;

public class ContainerCreationException extends RuntimeException {
  public ContainerCreationException(String message) {
    super(message);
  }

  public ContainerCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}