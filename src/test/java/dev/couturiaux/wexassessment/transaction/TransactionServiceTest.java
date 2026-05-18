package dev.couturiaux.wexassessment.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @Mock private TransactionRepository transactionRepository;

  @InjectMocks private TransactionService transactionService;

  private Transaction mockTransaction;
  private final UUID fakeTransactionId = UUID.randomUUID();
  private LocalDate transactionDate;

  @BeforeEach
  void setUp() {
    transactionDate = LocalDate.of(2026, 5, 16);

    mockTransaction = new Transaction("Test Purchase", new BigDecimal("100.00"), transactionDate);
    ReflectionTestUtils.setField(mockTransaction, "id", fakeTransactionId);
  }

  @Test
  void should_SaveAndReturnResponseWithId_When_PayloadIsValid() {
    CreateTransactionRequest requestDto =
        new CreateTransactionRequest(
            "Legal Retainer", new BigDecimal("1144.30"), LocalDate.of(2026, 5, 16));

    Transaction mockSavedEntity =
        new Transaction(
            requestDto.description(), requestDto.amount(), requestDto.transactionDate());

    UUID fakeId = UUID.randomUUID();
    ReflectionTestUtils.setField(mockSavedEntity, "id", fakeId);

    when(transactionRepository.save(any(Transaction.class))).thenReturn(mockSavedEntity);

    Transaction response = transactionService.createNewTransaction(requestDto);

    assertThat(response.getId()).isNotNull();
    assertThat(response.getDescription()).isEqualTo("Legal Retainer");
    assertThat(response.getAmount()).isEqualTo(new BigDecimal("1144.30"));
    assertThat(response.getTransactionDate()).isEqualTo(LocalDate.of(2026, 5, 16));

    verify(transactionRepository, times(1)).save(any(Transaction.class));
  }

  @Test
  void should_ReturnConvertedTransactions_When_TransactionsExist() {
    when(transactionRepository.findAll()).thenReturn(List.of(mockTransaction));

    List<ConvertedTransactionResponse> result =
        transactionService.getAllConvertedTransactions("EUR");

    assertThat(result).hasSize(1);

    ConvertedTransactionResponse response = result.get(0);
    assertThat(response.id()).isEqualTo(fakeTransactionId.toString());
    assertThat(response.description()).isEqualTo("Test Purchase");
    assertThat(response.targetCurrency()).isEqualTo("EUR");
    assertThat(response.amount()).isEqualTo(new BigDecimal("100.00"));

    assertThat(response.exchangeRate()).isEqualTo(new BigDecimal("1.5"));
    assertThat(response.convertedAmount())
        .isEqualTo(new BigDecimal("150.00").setScale(2, RoundingMode.HALF_UP));

    verify(transactionRepository, times(1)).findAll();
  }

  @Test
  void getAllTransactions_ShouldReturnMultipleMappedResponses_WhenMultipleExist() {
    Transaction mockTransaction2 =
        new Transaction("Test 2", new BigDecimal("250.50"), transactionDate);
    Transaction mockTransaction3 =
        new Transaction("Test 3", new BigDecimal("200.00"), LocalDate.of(2026, 4, 15));
    ReflectionTestUtils.setField(mockTransaction2, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(mockTransaction3, "id", UUID.randomUUID());

    List<Transaction> fakeTransactions =
        List.of(mockTransaction, mockTransaction2, mockTransaction3);

    when(transactionRepository.findAll()).thenReturn(fakeTransactions);

    List<TransactionResponse> responses = transactionService.getAllTransactions();

    assertThat(responses).hasSize(3);
    assertThat(responses.get(0).amount()).isEqualTo(new BigDecimal("100.00"));
    assertThat(responses.get(1).amount()).isEqualTo(new BigDecimal("250.50"));
    assertThat(responses.get(2).amount()).isEqualTo(new BigDecimal("200.00"));
  }

  @Test
  void should_ReturnAllUnconvertedTransactions_When_TransactionsExist() {
    when(transactionRepository.findAll()).thenReturn(List.of(mockTransaction));

    List<TransactionResponse> result = transactionService.getAllTransactions();
    assertThat(result).hasSize(1);

    TransactionResponse response = result.get(0);
    assertThat(response.id()).isEqualTo(fakeTransactionId.toString());
    assertThat(response.description()).isEqualTo("Test Purchase");
    assertThat(response.amount()).isEqualTo(new BigDecimal("100.00"));
    assertThat(response.transactionDate()).isEqualTo(LocalDate.of(2026, 5, 16));

    verify(transactionRepository, times(1)).findAll();
  }

  @Test
  void should_ReturnEmptyList_When_ZeroTransactionsExist() {
    when(transactionRepository.findAll()).thenReturn(Collections.emptyList());

    List<TransactionResponse> result = transactionService.getAllTransactions();
    assertThat(result).isEmpty();

    verify(transactionRepository, times(1)).findAll();
  }

  @Test
  void should_ReturnTransactionWithId_When_IdExist() {

    when(transactionRepository.findById(fakeTransactionId))
        .thenReturn(Optional.of(mockTransaction));

    Optional<TransactionResponse> response =
        transactionService.getTransactionById(fakeTransactionId);

    assertThat(response).isPresent();
    assertThat(response.get().id()).isEqualTo(fakeTransactionId.toString());
    assertThat(response.get().description()).isEqualTo("Test Purchase");
    assertThat(response.get().amount()).isEqualTo(new BigDecimal("100.00"));
    assertThat(response.get().transactionDate()).isEqualTo(LocalDate.of(2026, 5, 16));

    verify(transactionRepository, times(1)).findById(fakeTransactionId);
  }

  @Test
  void should_ReturnEmptyOptional_When_IdDoesNotExist() {
    when(transactionRepository.findById(fakeTransactionId)).thenReturn(Optional.empty());

    Optional<TransactionResponse> response =
        transactionService.getTransactionById(fakeTransactionId);

    assertThat(response).isEmpty();

    verify(transactionRepository, times(1)).findById(fakeTransactionId);
  }
}
