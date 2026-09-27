package com.bankingsystem.transactionservice.consumer;

import com.bankingsystem.transactionservice.entity.TransactionEntity;
import com.bankingsystem.transactionservice.entity.enums.TransactionStatus;
import com.bankingsystem.transactionservice.event.OtpNotificacionEvent;
import com.bankingsystem.transactionservice.repository.JpaTransactionRepository;
import com.bankingsystem.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final JpaTransactionRepository transactionRepository;
    private final TransactionService transactionService;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private final static String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";

    private final static long OTP_EXPIRATION_TIME_MINUTES = 5;

    @KafkaListener(topics = "verification.required", groupId = "transaction-service")
    public void consumeVerificationRequired(@Payload Map<String, Object> payload) {
        log.info("Received verification required event: {}", payload);

        try {

            String transactionId = (String) payload.get("transactionId");
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            log.info("Transaction ID: {}, Account Number: {}, Reason: {}", transactionId, accountNumber, reason);

            TransactionEntity transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

            if (transaction.getStatus() != TransactionStatus.PROCESSING) {
                log.warn("Transaction {} is not in PROCESSING state. Current state: {}", transactionId, transaction.getStatus());
                return;
            }

            // Generate OTP - 6 digit random number
            String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);

            // Store OTP in Redis
            this.redisTemplate.opsForValue().set("otp:" + transactionId, otp, Duration.ofMinutes(OTP_EXPIRATION_TIME_MINUTES));

            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);

            log.info("OTP generated and stored for transaction {} expires in {} minutes", transactionId, OTP_EXPIRATION_TIME_MINUTES);

            // Notify the user via email/SMS with the OTP
            OtpNotificacionEvent otpNotificacionEvent = new OtpNotificacionEvent(
                    transactionId,
                    accountNumber,
                    reason,
                    otp,
                    String.valueOf(OTP_EXPIRATION_TIME_MINUTES),
                    payload.get("amount").toString()
            );

            // Publish the OTP notification event
            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId, otpNotificacionEvent);

        } catch (Exception e) {
            log.error("Error occurred while processing verification required event", e);
        }
    }

    @KafkaListener(topics = "fraud.check.passed", groupId = "transaction-service")
    public void consumeFraudCheckPassed(@Payload Map<String, Object> payload) {
        log.info("Received fraud check passed event: {}", payload);

        try {
            String transactionId = (String) payload.get("transactionId");
            this.transactionService.processFraudCheckPassed(transactionId);
        } catch (Exception e) {
            log.error("Error occurred while processing fraud check passed event", e);
        }
    }

}
