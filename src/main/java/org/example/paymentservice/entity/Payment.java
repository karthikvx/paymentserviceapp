package org.example.paymentservice.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private String paymentId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors, getters, setters
    public Payment() {}

    public Payment(String paymentId, BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum PaymentStatus {
        PENDING, APPROVED, REJECTED, SETTLED
    }
}