package org.example.paymentservice.client;

import org.example.paymentservice.dto.PaymentEvent;
import org.example.paymentservice.model.FraudResponse;
import org.springframework.stereotype.Component;

@Component
public class FraudCheckClient {

    // TODO: Implement actual fraud check logic (gRPC/REST)
    public FraudResponse check(PaymentEvent event) {
        // Placeholder implementation
        return new FraudResponse(true, "LOW_RISK");
    }
}