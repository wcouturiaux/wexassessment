package dev.couturiaux.wexassessment.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

  private final TransactionService transactionService;
  private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @PostMapping
  public ResponseEntity<TransactionResponse> createTransaction(
      @Valid @RequestBody CreateTransactionRequest request) {
    logger.info("API: [CREATE] Request received to create a new transaction.");
    TransactionResponse response = transactionService.createNewTransaction(request);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
    List<TransactionResponse> response = transactionService.getAllTransactions();
    logger.info(
        "API: [FETCH_ALL] Successfully received bulk transaction list. Total records"
            + " found: {}",
        response.size());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/conversions")
  public ResponseEntity<List<ConvertedTransactionResponse>> getAllConvertedTransactions(
      @RequestParam(name = "target_country")
          @Size(min = 2, max = 2, message = "Target country code must be 2 characters")
          String targetCountry,
      @RequestParam(name = "target_currency")
          @Size(min = 3, max = 3, message = "Target currency code must be 3 characters")
          String targetCurrency) {
    List<ConvertedTransactionResponse> response =
        transactionService.getAllConvertedTransactions(targetCountry, targetCurrency);
    logger.info(
        "API: [FETCH_ALL_CONVERTED] Bulk exchange conversion completed. Total records"
            + " processed: {}",
        response.size());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID id) {
    logger.info("API: [FETCH_BY_ID] database transaction for ID: [{}]", id);
    TransactionResponse response = transactionService.getTransactionById(id);
    return ResponseEntity.ok(response);
  }
}
