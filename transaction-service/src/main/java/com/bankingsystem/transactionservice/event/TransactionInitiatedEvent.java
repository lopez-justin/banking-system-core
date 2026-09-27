package com.bankingsystem.transactionservice.event;

import java.math.BigDecimal;

public record TransactionInitiatedEvent(
        String transactionId,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount,
        String description
) {
}
