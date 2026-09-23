package com.bankingsystem.accountservice.dto;

import java.time.LocalDateTime;

public record AccountResponse(
        String id,
        String accountNumber,
        String accountOwnerName,
        String email,
        String phone,
        String accountType,
        String status,
        String balance,
        String dailyTransactionLimit,
        LocalDateTime createdAt
) {
}
