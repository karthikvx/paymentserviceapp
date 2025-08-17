package org.example.paymentservice.service;

import org.example.paymentservice.client.FraudCheckClient;
import org.example.paymentservice.dto.PaymentEvent;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.SettlementEvent;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.mapper.PaymentMapper;
import org.example.paymentservice.model.FraudResponse;
import org.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private FraudCheckClient fraudCheckClient;

    @Autowired
    private PaymentMapper paymentMapper;

    // Virtual threads automatically used for Kafka listeners
    @KafkaListener(topics = "${kafka.topic.payments}", groupId = "${kafka.group-id}",
            concurrency = "10") // Higher concurrency with virtual threads
    @Transactional
    public void processPaymentEvent(PaymentEvent event) {
        try {
            log.debug("Processing payment on thread: {}", Thread.currentThread());

            // 1. Async fraud check - virtual threads handle I/O efficiently
            FraudResponse fraudResponse = fraudCheckClient.check(event);

            if (fraudResponse.isApproved()) {
                // 2. Process payment
                Payment payment = paymentMapper.toEntity(event);
                paymentRepo.save(payment);

                // 3. Async settlement publishing
                publishSettlementAsync(payment);
            }
        } catch (Exception e) {
            log.error("Payment failed: {}", event.paymentId(), e);
            kafkaTemplate.send("payment-dlq", event.paymentId(), event);
        }
    }

    public String initiatePayment(PaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();

        // Non-blocking with virtual threads
        kafkaTemplate.send("payments", paymentId,
                new PaymentEvent(paymentId, request.amount(), request.currency()));

        return paymentId;
    }

    @Async // Will use virtual threads automatically
    public CompletableFuture<Void> publishSettlementAsync(Payment payment) {
        return CompletableFuture.runAsync(() -> {
            kafkaTemplate.send("settlements", payment.getPaymentId(),
                    new SettlementEvent(payment.getPaymentId(), payment.getAmount()));
        });
    }
}