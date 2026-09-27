package com.bankingsystem.frauddetectionservice.event;

import java.math.BigDecimal;

public record VerificationRequiredEvent(
        String transactionId,
        String accountNumber,
        BigDecimal amount,
        String reason
) {
}
