package org.example.paymentservice.controller;

import jakarta.validation.Valid;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.service.PaymentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentProcessor paymentProcessor;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        String paymentId = paymentProcessor.initiatePayment(request);
        return ResponseEntity.accepted()
                .body(new PaymentResponse(paymentId, "Processing"));
    }
}