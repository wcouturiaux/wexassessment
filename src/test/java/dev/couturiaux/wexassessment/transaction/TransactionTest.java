package dev.couturiaux.wexassessment.transaction;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TransactionTest {

  String validDescription = "Valid Description";
  BigDecimal validAmount = new BigDecimal("100.00");
  LocalDate validDate = LocalDate.now();

  @Test
  void should_ThrowException_When_DescriptionIsNull() {

    assertThatThrownBy(() -> new Transaction(null, validAmount, validDate))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Description is required");
  }

  @Test
  void should_ThrowException_When_AmountIsNull() {
    assertThatThrownBy(() -> new Transaction(validDescription, null, validDate))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Financial amount is required");
  }

  @Test
  void should_ThrowException_When_TransactionDateIsNull() {
    assertThatThrownBy(() -> new Transaction(validDescription, validAmount, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Transaction date is required");
  }
}
