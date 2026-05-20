package dev.couturiaux.wexassessment.core.exception;

public abstract class ResourceNotFoundException extends RuntimeException {

  protected ResourceNotFoundException(String message) {
    super(message);
  }
}
