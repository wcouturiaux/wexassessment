package dev.couturiaux.wexassessment.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record TransactionResponse(
    String id,
    String description,
    LocalDate transactionDate,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount) {
  public static TransactionResponse from(Transaction transaction) {
    UUID transactionId =
        Objects.requireNonNull(
            transaction.getId(), "Cannot map an unsaved transaction without an ID");
    return new TransactionResponse(
        transactionId.toString(),
        transaction.getDescription(),
        transaction.getTransactionDate(),
        transaction.getAmount());
  }
}
