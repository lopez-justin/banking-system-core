package com.bankingsystem.transactionservice.entity.enums;

/**
 * Enum representing the status of a transaction.
 * Transaction lifecycle flow:
 * PENDING -> PROCESSING -> COMPLETED (Clean transaction)
 * PENDING -> PROCESSING -> PENDING_VERIFICATION (suspicious transaction) -> COMPLETED (after verification)
 * PENDING -> PROCESSING -> PENDING_VERIFICATION (suspicious transaction) -> FLAGGED (pattern saga)
 * PENDING -> PROCESSING -> FAILED (insufficient funds, invalid account, etc.)
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED
}
