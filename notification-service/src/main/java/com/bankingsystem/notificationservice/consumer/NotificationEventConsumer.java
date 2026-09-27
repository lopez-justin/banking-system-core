package com.bankingsystem.notificationservice.consumer;

import com.bankingsystem.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "transaction.otp.generated", groupId = "notification-service")
    public void consumeOtpGenerated(@Payload Map<String, Object> payload) {
        log.info("Received OTP generated event: {}", payload);

        try {
            String transactionId = (String) payload.get("transactionId");
            String accountNumber = (String) payload.get("accountNumber");
            String otp = (String) payload.get("otp");
            String amount = (String) payload.get("amount");
            String reason = (String) payload.get("reason");

            this.notificationService.sendNotification(
                    accountNumber,
                    "TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "Suspicious transaction detected for your account." +
                                    "Reason: %s." +
                                    "A transaction of amount %s requires your verification." +
                                    "Please use the following OTP to verify the transaction: %s." +
                                    "Transaction ID: %s.",
                            reason, amount, otp, transactionId
                    )
            );

        } catch (Exception e) {
            log.error("Error occurred while processing OTP generated event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "transaction.completed", groupId = "notification-service")
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload) {
        log.info("Received transaction completed event: {}", payload);

        try {
            String senderAccount = (String) payload.get("senderAccountNumber");
            String receiverAccount = (String) payload.get("receiverAccountNumber");
            String amount = payload.get("amount").toString();

            this.notificationService.sendNotification(
                    senderAccount,
                    "TRANSACTION COMPLETED",
                    String.format(
                            "Your transaction of amount %s to account %s has been completed successfully.",
                            amount, receiverAccount
                    )
            );
            this.notificationService.sendNotification(
                    receiverAccount,
                    "TRANSACTION RECEIVED",
                    String.format(
                            "You have received a transaction of amount %s from account %s.",
                            amount, senderAccount
                    )
            );
        } catch (Exception e) {
            log.error("Error occurred while processing transaction completed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "fraud.detected", groupId = "notification-service")
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {
        log.info("Received fraud detected event: {}", payload);

        try {
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            this.notificationService.sendNotification(
                    accountNumber,
                    "FRAUD ALERT",
                    String.format(
                            "Suspicious activity has been detected on your account. Reason: %s. Please contact support immediately.",
                            reason
                    )
            );
        } catch (Exception e) {
            log.error("Error occurred while processing fraud detected event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "transaction.refunded", groupId = "notification-service")
    public void consumeTransactionRefunded(@Payload Map<String, Object> payload) {
        log.info("Received transaction refunded event: {}", payload);

        try {
            String accountNumber = (String) payload.get("accountNumber");
            String transactionId = (String) payload.get("transactionId");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            this.notificationService.sendNotification(
                    accountNumber,
                    "TRANSACTION REFUNDED",
                    String.format(
                            "Your transaction with ID %s has been refunded. Amount: %s. Reason: %s.",
                            transactionId, amount, reason
                    )
            );
        } catch (Exception e) {
            log.error("Error occurred while processing transaction refunded event: {}", e.getMessage(), e);
        }
    }

}
