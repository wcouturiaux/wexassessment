package dev.couturiaux.wexassessment.core.currency;

import dev.couturiaux.wexassessment.core.exception.ResourceNotFoundException;

public class ExchangeRateNotFoundException extends ResourceNotFoundException {
  public ExchangeRateNotFoundException(String countryCurrencyKey) {
    super(
        "No active exchange rate record exists for currency key [%s] within the 6 months of the transaction date."
            .formatted(countryCurrencyKey));
  }
}
