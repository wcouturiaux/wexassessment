package dev.couturiaux.wexassessment.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
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

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @PostMapping
  public ResponseEntity<TransactionResponse> createTransaction(
      @Valid @RequestBody CreateTransactionRequest request) {
    TransactionResponse response = transactionService.createNewTransaction(request);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
    List<TransactionResponse> responses = transactionService.getAllTransactions();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/conversions")
  public ResponseEntity<List<ConvertedTransactionResponse>> getAllConvertedTransactions(
      @RequestParam(name = "target_country")
          @Size(min = 2, max = 2, message = "Target country code must be 2 characters")
          String targetCountry,
      @RequestParam(name = "target_currency")
          @Size(min = 3, max = 3, message = "Target currency code must be 3 characters")
          String targetCurrency) {
    List<ConvertedTransactionResponse> responses =
        transactionService.getAllConvertedTransactions(targetCountry, targetCurrency);

    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID id) {
    TransactionResponse response = transactionService.getTransactionById(id);
    return ResponseEntity.ok(response);
  }
}
