package dev.couturiaux.wexassessment.transaction;

import dev.couturiaux.wexassessment.core.currency.ExchangeRateNotFoundException;
import dev.couturiaux.wexassessment.core.currency.TreasuryExchangeClient;
import dev.couturiaux.wexassessment.core.exception.TreasuryApiUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

  public ConvertedTransactionResponse getConvertedTransactionById(
      @NonNull UUID id, String targetCountry, String targetCurrency) {
    Transaction transaction =
        transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));

    String countryCurrencyKey = targetCountry + "-" + targetCurrency;
    LocalDate date = transaction.getTransactionDate();

    try {
      BigDecimal rate =
          treasuryExchangeClient.getFxRate(countryCurrencyKey, date.minusMonths(6), date);

      logger.info(
          "SERVICE: [CONVERSION_SUCCESS] Single calculated rate for transaction ID [{}]. Rate: {}",
          transaction.getId(),
          rate);

      return buildConvertedResponse(transaction, targetCurrency, rate, null);
    } catch (ExchangeRateNotFoundException | TreasuryApiUnavailableException ex) {
      logger.warn(
          "SERVICE: [CONVERSION_FAILED] Single transaction ID [{}] failed conversion: {}",
          transaction.getId(),
          ex.getMessage());

      return buildConvertedResponse(transaction, targetCurrency, null, ex.getMessage());
    }
  }

  public List<TransactionResponse> getAllTransactions() {
    return transactionRepository.findAll().stream().map(TransactionResponse::from).toList();
  }

  public List<ConvertedTransactionResponse> getAllConvertedTransactions(
      String targetCountry, String targetCurrency) {
    List<Transaction> rawTransactions = transactionRepository.findAll();

    Set<LocalDate> uniqueDates = extractUniqueDates(rawTransactions);

    String countryCurrencyKey = targetCountry + "-" + targetCurrency;

    Map<LocalDate, FxRateResult> fxRateCache = buildFxRateCache(uniqueDates, countryCurrencyKey);

    return rawTransactions.stream()
        .map(transaction -> convertToFxResponse(transaction, fxRateCache, targetCurrency))
        .toList();
  }

  private Set<LocalDate> extractUniqueDates(List<Transaction> transactions) {
    return transactions.stream().map(Transaction::getTransactionDate).collect(Collectors.toSet());
  }

  private Map<LocalDate, FxRateResult> buildFxRateCache(
      Set<LocalDate> dates, String countryCurrencyKey) {
    return dates.stream()
        .collect(
            Collectors.toMap(
                date -> date,
                date -> {
                  try {
                    BigDecimal rate =
                        treasuryExchangeClient.getFxRate(
                            countryCurrencyKey, date.minusMonths(6), date);
                    return new FxRateResult(rate, null);
                  } catch (ExchangeRateNotFoundException | TreasuryApiUnavailableException ex) {
                    return new FxRateResult(null, ex.getMessage());
                  }
                }));
  }

  private ConvertedTransactionResponse convertToFxResponse(
      Transaction transaction, Map<LocalDate, FxRateResult> fxRateCache, String targetCurrency) {
    FxRateResult fxResult = fxRateCache.get(transaction.getTransactionDate());

    if (fxResult == null) {
      return buildConvertedResponse(
          transaction, targetCurrency, null, "Critical error: No cache entry found for date.");
    }

    if (fxResult.errorMessage() != null) {
      logger.warn(
          "SERVICE: [CONVERSION_FAILED] Transaction ID [{}] failed conversion: {}",
          transaction.getId(),
          fxResult.errorMessage());
      return buildConvertedResponse(transaction, targetCurrency, null, fxResult.errorMessage());
    }

    BigDecimal fxRate = fxResult.rate();
    logger.debug(
        "SERVICE: [CONVERSION_SUCCESS] Calculated rate for transaction ID [{}]. Rate: {}",
        transaction.getId(),
        fxRate);

    return buildConvertedResponse(transaction, targetCurrency, fxRate, null);
  }

  private ConvertedTransactionResponse buildConvertedResponse(
      Transaction transaction, String targetCurrency, BigDecimal rate, String errorMessage) {
    BigDecimal convertedAmount = null;
    if (rate != null) {
      convertedAmount = transaction.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    return new ConvertedTransactionResponse(
        transaction.getId().toString(),
        transaction.getDescription(),
        transaction.getTransactionDate(),
        transaction.getAmount(),
        targetCurrency,
        rate,
        convertedAmount,
        errorMessage);
  }

  private TransactionResponse mapToResponse(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId().toString(),
        transaction.getDescription(),
        transaction.getTransactionDate(),
        transaction.getAmount());
  }

  private record FxRateResult(BigDecimal rate, String errorMessage) {}
}
