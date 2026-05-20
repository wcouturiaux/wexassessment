package dev.couturiaux.wexassessment.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TransactionService transactionService;

  @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

  private UUID transactionId;
  private String transactionIdResponse;
  private BigDecimal transactionAmount;
  private String description;
  private LocalDate transactionDate;
  private TransactionResponse mockResponse;
  private static final String APPLICATION_JSON = "application/json";

  @BeforeEach
  void setUp() {
    transactionId = UUID.randomUUID();
    transactionIdResponse = transactionId.toString();
    transactionAmount = new BigDecimal("1000.00");
    description = "Test Description";
    transactionDate = LocalDate.of(2026, 5, 16);

    mockResponse =
        new TransactionResponse(
            transactionIdResponse, description, transactionDate, transactionAmount);
  }

  @Test
  void should_CreateTransaction_When_ValidRequest() throws Exception {
    CreateTransactionRequest request =
        new CreateTransactionRequest(description, transactionAmount, transactionDate);

    when(transactionService.createNewTransaction(any(CreateTransactionRequest.class)))
        .thenReturn(mockResponse);

    mockMvc
        .perform(
            post("/api/v1/transactions")
                .contentType(APPLICATION_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(transactionIdResponse))
        .andExpect(jsonPath("$.description").value(description));
  }

  @Test
  void should_ReturnAllConvertedTransactions_When_TargetCurrencyProvided() throws Exception {
    BigDecimal exchangeRate = new BigDecimal("0.85");
    BigDecimal convertedAmount = new BigDecimal("850.00");

    ConvertedTransactionResponse mockConvertedResponse =
        new ConvertedTransactionResponse(
            transactionIdResponse,
            description,
            transactionDate,
            transactionAmount,
            "EUR",
            exchangeRate,
            convertedAmount,
            null);

    ConvertedTransactionResponse mockConvertedResponse2 =
        new ConvertedTransactionResponse(
            UUID.randomUUID().toString(),
            "Second Description",
            transactionDate,
            transactionAmount,
            "EUR",
            exchangeRate,
            convertedAmount,
            null);

    when(transactionService.getAllConvertedTransactions("DE", "EUR"))
        .thenReturn(List.of(mockConvertedResponse, mockConvertedResponse2));

    mockMvc
        .perform(
            get("/api/v1/transactions/conversions")
                .param("target_country", "DE")
                .param("target_currency", "EUR")
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(2))
        .andExpect(jsonPath("$[0].id").value(transactionIdResponse))
        .andExpect(jsonPath("$[0].target_currency").value("EUR"))
        .andExpect(jsonPath("$[1].id").value(mockConvertedResponse2.id()))
        .andExpect(jsonPath("$[1].target_currency").value("EUR"));
  }

  @Test
  void should_ReturnAllTransactions_When_Requested() throws Exception {
    TransactionResponse mockResponse2 =
        new TransactionResponse(
            UUID.randomUUID().toString(), "Second Description", transactionDate, transactionAmount);

    when(transactionService.getAllTransactions()).thenReturn(List.of(mockResponse, mockResponse2));

    mockMvc
        .perform(get("/api/v1/transactions").contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(2))
        .andExpect(jsonPath("$[0].id").value(transactionIdResponse))
        .andExpect(jsonPath("$[0].description").value(description))
        .andExpect(jsonPath("$[1].id").value(mockResponse2.id()))
        .andExpect(jsonPath("$[1].description").value("Second Description"));
  }

  @Test
  void should_ReturnTransaction_When_IdExists() throws Exception {
    when(transactionService.getTransactionById(transactionId)).thenReturn(mockResponse);

    mockMvc
        .perform(
            get("/api/v1/transactions/%s".formatted(transactionId)).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(transactionIdResponse));
  }

  @Test
  void should_ReturnNotFound_When_IdDoesNotExist() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    when(transactionService.getTransactionById(nonExistentId))
        .thenThrow(new TransactionNotFoundException(nonExistentId));

    mockMvc
        .perform(
            get("/api/v1/transactions/%s".formatted(nonExistentId)).contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_ReturnBadRequest_When_DescriptionIsBlank() throws Exception {
    CreateTransactionRequest request =
        new CreateTransactionRequest("", transactionAmount, transactionDate);

    mockMvc
        .perform(
            post("/api/v1/transactions")
                .contentType(APPLICATION_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_ReturnBadRequest_When_AmountIsNegativeOrZero() throws Exception {
    CreateTransactionRequest request =
        new CreateTransactionRequest(description, new BigDecimal("0.00"), transactionDate);

    mockMvc
        .perform(
            post("/api/v1/transactions")
                .contentType(APPLICATION_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_ReturnBadRequest_When_TransactionDateIsInFuture() throws Exception {
    CreateTransactionRequest request =
        new CreateTransactionRequest(description, transactionAmount, LocalDate.now().plusDays(1));

    mockMvc
        .perform(
            post("/api/v1/transactions")
                .contentType(APPLICATION_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_ReturnBadRequest_When_TargetCurrencyLengthIsInvalid() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/transactions/conversions")
                .param("target_country", "DE")
                .param("target_currency", "EU")
                .contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_ReturnBadRequest_When_TargetCountryLengthIsInvalid() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/transactions/conversions")
                .param("target_country", "D")
                .param("target_currency", "EUR")
                .contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_ReturnBadRequest_When_IdIsInvalidFormat() throws Exception {
    mockMvc
        .perform(get("/api/v1/transactions/12345-invalid-uuid").contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
