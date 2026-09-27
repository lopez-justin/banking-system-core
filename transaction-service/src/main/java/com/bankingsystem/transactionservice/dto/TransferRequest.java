package com.bankingsystem.transactionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "Sender account number is required")
        String senderAccountNumber,

        @NotBlank(message = "Receiver account number is required")
        String receiverAccountNumber,

        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount

) {
}
