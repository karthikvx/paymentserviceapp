package org.example.paymentservice.model;

public class FraudResponse {
    private boolean approved;
    private String riskLevel;

    public FraudResponse(boolean approved, String riskLevel) {
        this.approved = approved;
        this.riskLevel = riskLevel;
    }

    public boolean isApproved() { return approved; }
    public String getRiskLevel() { return riskLevel; }
}