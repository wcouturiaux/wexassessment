package dev.couturiaux.wexassessment.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConvertedTransactionResponse(
    String id,
    String description,
    LocalDate transactionDate,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
    String targetCurrency,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal exchangeRate,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal convertedAmount) {}
