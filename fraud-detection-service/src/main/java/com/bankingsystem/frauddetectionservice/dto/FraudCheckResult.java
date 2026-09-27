package com.bankingsystem.frauddetectionservice.dto;

public record FraudCheckResult(
        boolean fraud,
        String reason
) {
}
