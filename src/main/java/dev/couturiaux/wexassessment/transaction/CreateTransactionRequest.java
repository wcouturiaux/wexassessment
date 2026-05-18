package dev.couturiaux.wexassessment.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
    @NotBlank(message = "Description cannot be empty or just spaces.")
        @Size(min = 3, max = 50, message = "Description must be between 3 and 50 characters.")
        String description,
    @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be positive.")
        @Digits(
            integer = 13,
            fraction = 2,
            message =
                "Amount must have a maximum of {integer} whole digits and {fraction} decimal"
                    + " places.")
        BigDecimal amount,
    @NotNull(message = "Transaction date is required.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @PastOrPresent(message = "Transaction date cannont be in the future.")
        LocalDate transactionDate) {}
