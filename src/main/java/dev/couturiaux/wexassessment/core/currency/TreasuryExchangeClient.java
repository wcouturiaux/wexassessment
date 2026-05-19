package dev.couturiaux.wexassessment.core.currency;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
                () -> new IllegalArgumentException("Unsupported country or currency code."));

    String formattedStart = startDate.format(TREASURY_DATE_FORMATTER);
    String formattedEnd = endDate.format(TREASURY_DATE_FORMATTER);

    String filterValue =
        "country_currency_desc:eq:%s,effective_date:gte:%s,effective_date:lte:%s&sort=-effective_date"
            .formatted(treasuryDesc, formattedStart, formattedEnd);

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

    throw new RuntimeException("Purchases cannot be converted to the target currency.");
  }

  private record TreasuryApiResponse(List<TreasuryData> data) {}

  private record TreasuryData(@JsonProperty("exchange_rate") String exchangeRate) {}
}
