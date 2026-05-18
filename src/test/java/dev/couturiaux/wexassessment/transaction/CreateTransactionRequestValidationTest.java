package dev.couturiaux.wexassessment.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateTransactionRequestValidationTest {

  private static Validator validator;
  private static final String VALID_DESCRIPTION = "Pay";
  private static final BigDecimal VALID_AMOUNT = new BigDecimal("100.50");
  private static final LocalDate VALID_TX_DATE = LocalDate.of(2026, 05, 01);

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void should_Pass_When_PayloadIsValid() {
    CreateTransactionRequest request =
        new CreateTransactionRequest(VALID_DESCRIPTION, VALID_AMOUNT, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  void should_Fail_When_DescriptionIsBlank() {
    String invalidDescription = "    ";
    String blankDescMessage = "Description cannot be empty or just spaces.";
    CreateTransactionRequest request =
        new CreateTransactionRequest(invalidDescription, VALID_AMOUNT, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(blankDescMessage);
  }

  @Test
  void should_Fail_When_DescriptionIsTooShort() {
    String invalidDescription = "Tx";
    String invalidSizeDescMsg = "Description must be between 3 and 50 characters.";
    CreateTransactionRequest request =
        new CreateTransactionRequest(invalidDescription, VALID_AMOUNT, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidSizeDescMsg);
  }

  @Test
  void should_Fail_When_DescriptionIsTooLong() {
    String invalidDescription = "B2B software subscription renewal for the month May";
    String invalidSizeDescMsg = "Description must be between 3 and 50 characters.";
    CreateTransactionRequest request =
        new CreateTransactionRequest(invalidDescription, VALID_AMOUNT, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidSizeDescMsg);
  }

  @Test
  void should_Fail_When_AmountIsNull() {
    BigDecimal invalidAmount = null;
    String invalidMinDecimalMsg = "Amount is required.";
    CreateTransactionRequest request =
        new CreateTransactionRequest(VALID_DESCRIPTION, invalidAmount, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidMinDecimalMsg);
  }

  @Test
  void should_Fail_When_AmountIsNonPositive() {
    BigDecimal invalidAmount = new BigDecimal("0.00");
    String invalidMinDecimalMsg = "Amount must be positive.";
    CreateTransactionRequest request =
        new CreateTransactionRequest(VALID_DESCRIPTION, invalidAmount, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidMinDecimalMsg);
  }

  @Test
  void should_Fail_When_AmountHasMoreThanTwoDecimals() {
    BigDecimal invalidAmount = new BigDecimal("1.001");
    Integer integerPlaces = 13;
    Integer decimalPlaces = 2;
    String invalidDigitsMsg =
        "Amount must have a maximum of %d whole digits and %d decimal places."
            .formatted(integerPlaces, decimalPlaces);
    CreateTransactionRequest request =
        new CreateTransactionRequest(VALID_DESCRIPTION, invalidAmount, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidDigitsMsg);
  }

  @Test
  void should_Fail_When_AmountHasMoreThanThirteenWholeDigits() {
    BigDecimal invalidAmount = new BigDecimal("123456789101112.00");
    Integer integerPlaces = 13;
    Integer decimalPlaces = 2;
    String invalidDigitsMsg =
        "Amount must have a maximum of %d whole digits and %d decimal places."
            .formatted(integerPlaces, decimalPlaces);
    CreateTransactionRequest request =
        new CreateTransactionRequest(VALID_DESCRIPTION, invalidAmount, VALID_TX_DATE);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidDigitsMsg);
  }

  @Test
  void should_Fail_When_TransactionDateIsNull() {
    LocalDate invalidDate = null;
    String invalidDateMsg = "Transaction date is required.";
    CreateTransactionRequest request =
        new CreateTransactionRequest(VALID_DESCRIPTION, VALID_AMOUNT, invalidDate);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidDateMsg);
  }

  @Test
  void should_Fail_When_TransactionDateIsInTheFuture() {
    LocalDate invalidDate = LocalDate.now().plusDays(1);
    String invalidDateMsg = "Transaction date cannont be in the future.";
    CreateTransactionRequest request =
        new CreateTransactionRequest(VALID_DESCRIPTION, VALID_AMOUNT, invalidDate);

    Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
    assertThat(violations).hasSize(1);

    String errorMessage = violations.iterator().next().getMessage();
    assertThat(errorMessage).isEqualTo(invalidDateMsg);
  }
}
