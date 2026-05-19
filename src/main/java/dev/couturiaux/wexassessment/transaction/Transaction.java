package dev.couturiaux.wexassessment.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "transactions")
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.UUID)
  private UUID id;

  @Column(length = 50, nullable = false)
  private String description;

  @Column(precision = 15, scale = 2, nullable = false)
  private BigDecimal amount;

  private LocalDate transactionDate;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Transaction() {}

  public Transaction(String description, BigDecimal amount, LocalDate transactionDate) {
    this.description = Objects.requireNonNull(description, "Description is required");
    this.amount = Objects.requireNonNull(amount, "Financial amount is required");
    this.transactionDate = Objects.requireNonNull(transactionDate, "Transaction date is required");
  }

  public UUID getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getTransactionDate() {
    return transactionDate;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
