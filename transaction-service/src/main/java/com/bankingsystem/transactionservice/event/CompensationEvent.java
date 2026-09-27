package com.bankingsystem.transactionservice.event;

import java.math.BigDecimal;

public record CompensationEvent (
        String transactionId,
        String accountNumber,
        BigDecimal amount,
        String reason
) {
}
