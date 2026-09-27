package com.bankingsystem.transactionservice.event;

public record FraudDetectedEvent(
        String transactionId,
        String accountNumber,
        String reason
) {
}
