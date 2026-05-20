package dev.couturiaux.wexassessment.core.currency;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.couturiaux.wexassessment.core.exception.TreasuryApiUnavailableException;
import dev.couturiaux.wexassessment.core.exception.UnsupportedCountryCurrencyException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TreasuryExchangeClient {

  private final RestClient restClient;
  private final TreasuryExchangeRateProvider exchangeRateProvider;
  private final String exchangeRatesPath;
  private static final DateTimeFormatter TREASURY_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("M/dd/yyyy");

  public TreasuryExchangeClient(
      TreasuryExchangeRateProvider exchangeRateProvider,
      @Value("${app.treasury-api.base-url}") String baseUrl,
      @Value("${app.treasury-api.endpoints.exchange-rates}") String exchangeRatesPath) {
    this.exchangeRateProvider = exchangeRateProvider;
    this.exchangeRatesPath = exchangeRatesPath;
    this.restClient = RestClient.builder().baseUrl(Objects.requireNonNull(baseUrl)).build();
  }

  public BigDecimal getFxRate(String countryCurrencyKey, LocalDate startDate, LocalDate endDate) {
    String treasuryDesc =
        exchangeRateProvider
            .getTreasuryDescription(countryCurrencyKey)
            .orElseThrow(
                () ->
                    new UnsupportedCountryCurrencyException(
                        "The country currency combination [%s] is not supported"
                            .formatted(countryCurrencyKey)));

    String formattedStart = startDate.format(TREASURY_DATE_FORMATTER);
    String formattedEnd = endDate.format(TREASURY_DATE_FORMATTER);

    String filterValue =
        "country_currency_desc:eq:%s,effective_date:gte:%s,effective_date:lte:%s&sort=-effective_date"
            .formatted(treasuryDesc, formattedStart, formattedEnd);

    try {
      TreasuryApiResponse response =
          restClient
              .get()
              .uri(
                  builder ->
                      builder
                          .path(Objects.requireNonNull(exchangeRatesPath))
                          .queryParam("fields", "exchange_rate,effective_date")
                          .queryParam("filter", filterValue)
                          .build())
              .retrieve()
              .body(TreasuryApiResponse.class);

      if (response != null && response.data() != null && !response.data().isEmpty()) {
        return new BigDecimal(response.data().get(0).exchangeRate);
      }
    } catch (RestClientException ex) {
      throw new TreasuryApiUnavailableException(
          "The US Treasury Fiscal Data API is currently unreachable or returned an invalid"
              + " response.",
          ex);
    }

    throw new ExchangeRateNotFoundException(
        "Purchases cannot be converted because no active exchange rate was found for key [%s] within the required 6-month window."
            .formatted(countryCurrencyKey));
  }

  private record TreasuryApiResponse(List<TreasuryData> data) {}

  private record TreasuryData(@JsonProperty("exchange_rate") String exchangeRate) {}
}
