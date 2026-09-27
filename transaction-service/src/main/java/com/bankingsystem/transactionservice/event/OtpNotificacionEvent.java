package com.bankingsystem.transactionservice.event;

public record OtpNotificacionEvent(
        String transactionId,
        String accountNumber,
        String reason,
        String otpCode,
        String otpExpiration,
        String amount
) {
}
