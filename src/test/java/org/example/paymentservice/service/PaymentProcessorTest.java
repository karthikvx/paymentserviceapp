package org.example.paymentservice.service;

import org.example.paymentservice.client.FraudCheckClient;
import org.example.paymentservice.dto.PaymentEvent;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.mapper.PaymentMapper;
import org.example.paymentservice.model.FraudResponse;
import org.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FraudCheckClient fraudCheckClient;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentProcessor paymentProcessor;

    private PaymentEvent paymentEvent;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentEvent = new PaymentEvent("test-payment-id", new BigDecimal("100.00"), "USD");
        payment = new Payment("test-payment-id", new BigDecimal("100.00"), "USD");
    }

    @Test
    void processPaymentEvent_FraudApproved_ProcessesSuccessfully() {
        // Given
        FraudResponse fraudResponse = new FraudResponse(true, "LOW_RISK");
        when(fraudCheckClient.check(paymentEvent)).thenReturn(fraudResponse);
        when(paymentMapper.toEntity(paymentEvent)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);

        // When
        paymentProcessor.processPaymentEvent(paymentEvent);

        // Then
        verify(fraudCheckClient).check(paymentEvent);
        verify(paymentMapper).toEntity(paymentEvent);
        verify(paymentRepository).save(payment);
        verify(kafkaTemplate).send(eq("settlements"), eq("test-payment-id"), any());
    }

    @Test
    void processPaymentEvent_FraudRejected_DoesNotProcess() {
        // Given
        FraudResponse fraudResponse = new FraudResponse(false, "HIGH_RISK");
        when(fraudCheckClient.check(paymentEvent)).thenReturn(fraudResponse);

        // When
        paymentProcessor.processPaymentEvent(paymentEvent);

        // Then
        verify(fraudCheckClient).check(paymentEvent);
        verify(paymentMapper, never()).toEntity(any());
        verify(paymentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(eq("settlements"), any(), any());
    }

    @Test
    void processPaymentEvent_ExceptionThrown_SendsToDLQ() {
        // Given
        when(fraudCheckClient.check(paymentEvent)).thenThrow(new RuntimeException("Fraud check failed"));

        // When
        paymentProcessor.processPaymentEvent(paymentEvent);

        // Then
        verify(kafkaTemplate).send("payment-dlq", "test-payment-id", paymentEvent);
    }

    @Test
    void initiatePayment_CreatesPaymentEvent() {
        // Given
        PaymentRequest request = new PaymentRequest(new BigDecimal("50.00"), "EUR");

        // When
        String paymentId = paymentProcessor.initiatePayment(request);

        // Then
        assertNotNull(paymentId);
        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(kafkaTemplate).send(eq("payments"), eq(paymentId), eventCaptor.capture());

        PaymentEvent capturedEvent = eventCaptor.getValue();
        assertEquals(paymentId, capturedEvent.paymentId());
        assertEquals(new BigDecimal("50.00"), capturedEvent.amount());
        assertEquals("EUR", capturedEvent.currency());
    }
}