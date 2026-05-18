package dev.couturiaux.wexassessment.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {
  private final TransactionRepository transactionRepository;

  public TransactionService(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  @Transactional
  public Transaction createNewTransaction(CreateTransactionRequest transactionDto) {
    Transaction newTransaction =
        new Transaction(
            transactionDto.description(),
            transactionDto.amount(),
            transactionDto.transactionDate());

    return transactionRepository.save(newTransaction);
  }

  public Optional<TransactionResponse> getTransactionById(@NonNull UUID id) {
    return transactionRepository.findById(id).map(TransactionResponse::from);
  }

  public List<TransactionResponse> getAllTransactions() {
    return transactionRepository.findAll().stream().map(TransactionResponse::from).toList();
  }

  public List<ConvertedTransactionResponse> getAllConvertedTransactions(String targetCurrency) {
    List<Transaction> rawTransactions = transactionRepository.findAll();

    Set<LocalDate> uniqueDates = extractUniqueDates(rawTransactions);

    Map<LocalDate, BigDecimal> fxRateCache = buildFxRateCache(uniqueDates);

    return rawTransactions.stream()
        .map(transaction -> convertToFxResponse(transaction, fxRateCache, targetCurrency))
        .toList();
  }

  private Set<LocalDate> extractUniqueDates(List<Transaction> transactions) {
    return transactions.stream().map(Transaction::getTransactionDate).collect(Collectors.toSet());
  }

  private Map<LocalDate, BigDecimal> buildFxRateCache(Set<LocalDate> dates) {
    // TODO: Replace constant rate with api calls
    return dates.stream().collect(Collectors.toMap(date -> date, date -> new BigDecimal("1.5")));
  }

  private ConvertedTransactionResponse convertToFxResponse(
      Transaction transaction, Map<LocalDate, BigDecimal> fxRateCache, String targetCurrency) {
    UUID transactionId =
        Objects.requireNonNull(transaction.getId(), "Persisted Transaction must have an ID.");
    String id = transactionId.toString();
    BigDecimal amount = transaction.getAmount();
    BigDecimal fxRate =
        Objects.requireNonNull(
            fxRateCache.get(transaction.getTransactionDate()),
            () ->
                "CRITICAL: Missing FX rate for date %s"
                    .formatted(transaction.getTransactionDate()));
    BigDecimal convertedAmount = amount.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);

    return new ConvertedTransactionResponse(
        id,
        transaction.getDescription(),
        transaction.getTransactionDate(),
        amount,
        targetCurrency,
        fxRate,
        convertedAmount);
  }
}
