package dev.couturiaux.wexassessment.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TransactionResponseTest {

  @Test
  void from_ShouldMapEntityToResponse_WhenTransactionIsValid() {
    UUID fakeId = UUID.randomUUID();
    LocalDate transactionDate = LocalDate.of(2026, 5, 16);
    Transaction transaction =
        new Transaction("Legal Retainer", new BigDecimal("1000.50"), transactionDate);
    ReflectionTestUtils.setField(transaction, "id", fakeId);

    TransactionResponse response = TransactionResponse.from(transaction);

    assertThat(response.id()).isEqualTo(fakeId.toString());
    assertThat(response.description()).isEqualTo("Legal Retainer");
    assertThat(response.amount()).isEqualTo(new BigDecimal("1000.50"));
    assertThat(response.transactionDate()).isEqualTo(transactionDate);
  }

  @Test
  void from_ShouldThrowException_WhenTransactionIdIsNull() {
    Transaction transactionWithoutId =
        new Transaction("Legal Retainer", new BigDecimal("1000.50"), LocalDate.now());
    // ID is null because it hasn't been saved to DB and we didn't use ReflectionTestUtils

    assertThatThrownBy(() -> TransactionResponse.from(transactionWithoutId))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Cannot map an unsaved transaction without an ID");
  }
}
