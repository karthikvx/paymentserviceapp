package org.example.paymentservice.dto;

import java.math.BigDecimal;

public record PaymentEvent(String paymentId, BigDecimal amount, String currency) {}