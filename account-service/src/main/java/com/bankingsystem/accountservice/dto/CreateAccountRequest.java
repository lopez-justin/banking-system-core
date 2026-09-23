package com.bankingsystem.accountservice.dto;

import com.bankingsystem.accountservice.entity.enums.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "account holder name is required")
        String accountOwnerName,

        @NotBlank(message = "email is required")
        @Email(message = "email should be valid")
        String email,

        @NotBlank(message = "phone number is required")
        String phone,

        @NotBlank(message = "account type is required")
        AccountType accountType,

        @Positive(message = "initial deposit must be positive")
        BigDecimal initialDeposit

) {
}
