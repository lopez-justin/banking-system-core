package com.bankingsystem.transactionservice.dto;

import com.bankingsystem.transactionservice.entity.enums.TransactionStatus;
import com.bankingsystem.transactionservice.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String id,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String description,
        String failureReason,
        String referenceNumber,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
