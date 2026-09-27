package com.bankingsystem.frauddetectionservice.event;

public record FraudCheckPassedEvent(
        String transactionId,
        boolean isFraud,
        String reason
) {
}
