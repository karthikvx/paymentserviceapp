package org.example.paymentservice.dto;

import java.math.BigDecimal;

public record SettlementEvent(String paymentId, BigDecimal amount) {}