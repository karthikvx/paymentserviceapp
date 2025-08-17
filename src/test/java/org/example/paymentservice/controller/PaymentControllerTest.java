package org.example.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.service.PaymentProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentProcessor paymentProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPayment_Success() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "USD");
        String paymentId = "test-payment-id";

        when(paymentProcessor.initiatePayment(any(PaymentRequest.class)))
                .thenReturn(paymentId);

        // When & Then
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.status").value("Processing"));
    }

    @Test
    void createPayment_InvalidAmount_BadRequest() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest(new BigDecimal("-10.00"), "USD");

        // When & Then
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_EmptyCurrency_BadRequest() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "");

        // When & Then
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}