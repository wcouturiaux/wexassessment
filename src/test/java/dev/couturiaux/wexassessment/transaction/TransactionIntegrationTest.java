package dev.couturiaux.wexassessment.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.couturiaux.wexassessment.core.currency.ExchangeRateNotFoundException;
import dev.couturiaux.wexassessment.core.currency.TreasuryExchangeClient;
import dev.couturiaux.wexassessment.core.exception.TreasuryApiUnavailableException;
import dev.couturiaux.wexassessment.core.exception.UnsupportedCountryCurrencyException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TransactionRepository transactionRepository;

  @MockitoBean private TreasuryExchangeClient treasuryExchangeClient;

  @BeforeEach
  void setUp() {
    transactionRepository.deleteAll();
  }

  @Test
  void should_PersistAndRetrieveAndConvertTransaction_ThroughFullLifecycle() throws Exception {
    String description = "Integration Test Headset";
    BigDecimal usdAmount = new BigDecimal("100.00");
    LocalDate date = LocalDate.of(2026, 5, 15);

    CreateTransactionRequest createRequest =
        new CreateTransactionRequest(description, usdAmount, date);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType("application/json")
                    .content(
                        Objects.requireNonNull(objectMapper.writeValueAsString(createRequest))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.description").value(description))
            .andExpect(jsonPath("$.amount").value("100.00"))
            .andExpect(jsonPath("$.transaction_date").value("2026-05-15"))
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    TransactionResponse createResponse =
        objectMapper.readValue(responseBody, TransactionResponse.class);
    UUID transactionId = UUID.fromString(createResponse.id());

    List<Transaction> dbTransactions = transactionRepository.findAll();
    assertThat(dbTransactions).hasSize(1);
    assertThat(dbTransactions.get(0).getId()).isEqualTo(transactionId);
    assertThat(dbTransactions.get(0).getDescription()).isEqualTo(description);
    assertThat(dbTransactions.get(0).getAmount()).isEqualByComparingTo(usdAmount);

    mockMvc
        .perform(get("/api/v1/transactions/%s".formatted(transactionId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(transactionId.toString()))
        .andExpect(jsonPath("$.description").value(description))
        .andExpect(jsonPath("$.amount").value("100.00"));

    BigDecimal mockRate = new BigDecimal("1.365");
    when(treasuryExchangeClient.getFxRate(eq("CA-CAD"), any(LocalDate.class), eq(date)))
        .thenReturn(mockRate);

    mockMvc
        .perform(
            get("/api/v1/transactions/conversions")
                .param("target_country", "CA")
                .param("target_currency", "CAD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].id").value(transactionId.toString()))
        .andExpect(jsonPath("$[0].description").value(description))
        .andExpect(jsonPath("$[0].amount").value("100.00"))
        .andExpect(jsonPath("$[0].target_currency").value("CAD"))
        .andExpect(jsonPath("$[0].exchange_rate").value("1.365"))
        .andExpect(jsonPath("$[0].converted_amount").value("136.50"))
        .andExpect(jsonPath("$[0].error_message").doesNotExist());
  }

  @Test
  void should_ReturnNotFound_When_TransactionIdDoesNotExist() throws Exception {
    UUID randomId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/v1/transactions/%s".formatted(randomId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(
            jsonPath("$.detail")
                .value("Transaction with ID [%s] was not found.".formatted(randomId)))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void should_ReturnBadRequest_When_PayloadIsValidatedWithErrors() throws Exception {
    CreateTransactionRequest invalidRequest =
        new CreateTransactionRequest("", new BigDecimal("-10.00"), LocalDate.now().plusDays(1));

    mockMvc
        .perform(
            post("/api/v1/transactions")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation Failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.invalid_params").isArray());
  }

  @Test
  void should_ReturnUnprocessableEntity_When_CurrencyConfigIsUnsupported() throws Exception {
    String description = "Unprocessable Device";
    BigDecimal amount = new BigDecimal("50.00");
    LocalDate date = LocalDate.of(2026, 5, 15);
    Transaction tx = new Transaction(description, amount, date);
    transactionRepository.save(tx);

    when(treasuryExchangeClient.getFxRate(eq("XX-XYZ"), any(LocalDate.class), eq(date)))
        .thenThrow(
            new UnsupportedCountryCurrencyException(
                "The country currency combination [XX-XYZ] is not supported"));

    mockMvc
        .perform(
            get("/api/v1/transactions/conversions")
                .param("target_country", "XX")
                .param("target_currency", "XYZ"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.title").value("Unsupported Currency Configuration"))
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(
            jsonPath("$.detail")
                .value("The country currency combination [XX-XYZ] is not supported"));
  }

  @Test
  void should_GracefullyReturnEmptyConversionFields_When_ExchangeRateIsNotFound() throws Exception {
    String description = "No Rate Device";
    BigDecimal amount = new BigDecimal("50.00");
    LocalDate date = LocalDate.of(2026, 5, 15);
    Transaction tx = new Transaction(description, amount, date);
    tx = transactionRepository.save(tx);
    UUID transactionId = tx.getId();

    when(treasuryExchangeClient.getFxRate(eq("CA-CAD"), any(LocalDate.class), eq(date)))
        .thenThrow(new ExchangeRateNotFoundException("CA-CAD"));

    mockMvc
        .perform(
            get("/api/v1/transactions/conversions")
                .param("target_country", "CA")
                .param("target_currency", "CAD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].id").value(transactionId.toString()))
        .andExpect(jsonPath("$[0].description").value(description))
        .andExpect(jsonPath("$[0].amount").value("50.00"))
        .andExpect(jsonPath("$[0].target_currency").value("CAD"))
        .andExpect(jsonPath("$[0].exchange_rate").value((Object) null))
        .andExpect(jsonPath("$[0].converted_amount").value((Object) null))
        .andExpect(
            jsonPath("$[0].error_message")
                .value(
                    "No active exchange rate record exists for currency key [CA-CAD] within the 6"
                        + " months of the transaction date."));
  }

  @Test
  void should_GracefullyReturnEmptyConversionFields_When_TreasuryApiIsUnavailable()
      throws Exception {
    String description = "Unavailable Device";
    BigDecimal amount = new BigDecimal("50.00");
    LocalDate date = LocalDate.of(2026, 5, 15);
    Transaction tx = new Transaction(description, amount, date);
    tx = transactionRepository.save(tx);
    UUID transactionId = tx.getId();

    when(treasuryExchangeClient.getFxRate(eq("CA-CAD"), any(LocalDate.class), eq(date)))
        .thenThrow(
            new TreasuryApiUnavailableException(
                "The US Treasury Fiscal Data API is currently unreachable or returned an invalid"
                    + " response.",
                new RuntimeException("Timeout")));

    mockMvc
        .perform(
            get("/api/v1/transactions/conversions")
                .param("target_country", "CA")
                .param("target_currency", "CAD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].id").value(transactionId.toString()))
        .andExpect(jsonPath("$[0].description").value(description))
        .andExpect(jsonPath("$[0].amount").value("50.00"))
        .andExpect(jsonPath("$[0].target_currency").value("CAD"))
        .andExpect(jsonPath("$[0].exchange_rate").value((Object) null))
        .andExpect(jsonPath("$[0].converted_amount").value((Object) null))
        .andExpect(
            jsonPath("$[0].error_message")
                .value(
                    "The US Treasury Fiscal Data API is currently unreachable or returned an"
                        + " invalid response."));
  }
}
