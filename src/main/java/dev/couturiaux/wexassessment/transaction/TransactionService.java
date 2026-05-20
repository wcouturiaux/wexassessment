package dev.couturiaux.wexassessment.transaction;

import dev.couturiaux.wexassessment.core.currency.TreasuryExchangeClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {
  private final TransactionRepository transactionRepository;
  private final TreasuryExchangeClient treasuryExchangeClient;
  private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

  public TransactionService(
      TransactionRepository transactionRepository, TreasuryExchangeClient treasuryExchangeClient) {
    this.transactionRepository = transactionRepository;
    this.treasuryExchangeClient = treasuryExchangeClient;
  }

  @Transactional
  public TransactionResponse createNewTransaction(CreateTransactionRequest transactionDto) {
    Transaction newTransaction =
        new Transaction(
            transactionDto.description(),
            transactionDto.amount(),
            transactionDto.transactionDate());
    Transaction transaction = transactionRepository.save(newTransaction);
    logger.info("SERVICE: Created transaction with ID: {}", transaction.getId());

    return mapToResponse(transaction);
  }

  public TransactionResponse getTransactionById(@NonNull UUID id) {

    return transactionRepository
        .findById(id)
        .map(TransactionResponse::from)
        .orElseThrow(() -> new TransactionNotFoundException(id));
  }

  public List<TransactionResponse> getAllTransactions() {
    return transactionRepository.findAll().stream().map(TransactionResponse::from).toList();
  }

  public List<ConvertedTransactionResponse> getAllConvertedTransactions(
      String targetCountry, String targetCurrency) {
    List<Transaction> rawTransactions = transactionRepository.findAll();

    Set<LocalDate> uniqueDates = extractUniqueDates(rawTransactions);

    String countryCurrencyKey = targetCountry + "-" + targetCurrency;

    Map<LocalDate, BigDecimal> fxRateCache = buildFxRateCache(uniqueDates, countryCurrencyKey);

    return rawTransactions.stream()
        .map(transaction -> convertToFxResponse(transaction, fxRateCache, targetCurrency))
        .toList();
  }

  private Set<LocalDate> extractUniqueDates(List<Transaction> transactions) {
    return transactions.stream().map(Transaction::getTransactionDate).collect(Collectors.toSet());
  }

  private Map<LocalDate, BigDecimal> buildFxRateCache(
      Set<LocalDate> dates, String countryCurrencyKey) {
    return dates.stream()
        .collect(
            Collectors.toMap(
                date -> date,
                date ->
                    treasuryExchangeClient.getFxRate(
                        countryCurrencyKey, date.minusMonths(6), date)));
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
    logger.debug(
        "SERVICE: [CONVERSION_SUCCESS] Calculated rate for transaction ID [{}]. Rate: {},"
            + " Result: {} {}",
        transactionId,
        fxRate,
        convertedAmount,
        targetCurrency);

    return new ConvertedTransactionResponse(
        id,
        transaction.getDescription(),
        transaction.getTransactionDate(),
        amount,
        targetCurrency,
        fxRate,
        convertedAmount);
  }

  private TransactionResponse mapToResponse(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId().toString(),
        transaction.getDescription(),
        transaction.getTransactionDate(),
        transaction.getAmount());
  }
}
