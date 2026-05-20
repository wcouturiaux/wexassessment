package dev.couturiaux.wexassessment.transaction;

import dev.couturiaux.wexassessment.core.exception.ResourceNotFoundException;
import java.util.UUID;

public class TransactionNotFoundException extends ResourceNotFoundException {
  public TransactionNotFoundException(UUID id) {
    super("Transaction with ID [%s] was not found.".formatted(id));
  }
}
