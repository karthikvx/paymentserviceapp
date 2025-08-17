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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

    @KafkaListener(topics = "${kafka.topic.payments}", groupId = "${kafka.group-id}")
    @Transactional
    public void processPaymentEvent(PaymentEvent event) {
        try {
            // 1. Sync fraud check (low-latency)
            FraudResponse fraudResponse = fraudCheckClient.check(event);

            if (fraudResponse.isApproved()) {
                // 2. Process payment
                Payment payment = paymentMapper.toEntity(event);
                paymentRepo.save(payment);

                // 3. Publish settlement event
                kafkaTemplate.send("settlements", payment.getPaymentId(),
                        new SettlementEvent(payment.getPaymentId(), payment.getAmount()));
            }
        } catch (Exception e) {
            log.error("Payment failed: {}", event.paymentId(), e);
            kafkaTemplate.send("payment-dlq", event.paymentId(), event); // DLQ
        }
    }

    public String initiatePayment(PaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();
        kafkaTemplate.send("payments", paymentId,
                new PaymentEvent(paymentId, request.amount(), request.currency()));
        return paymentId;
    }
}