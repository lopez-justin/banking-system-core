package com.bankingsystem.transactionservice.service;

import com.bankingsystem.transactionservice.client.AccountServiceClient;
import com.bankingsystem.transactionservice.dto.TransactionResponse;
import com.bankingsystem.transactionservice.dto.TransferRequest;
import com.bankingsystem.transactionservice.entity.TransactionEntity;
import com.bankingsystem.transactionservice.entity.enums.TransactionStatus;
import com.bankingsystem.transactionservice.entity.enums.TransactionType;
import com.bankingsystem.transactionservice.event.CompensationEvent;
import com.bankingsystem.transactionservice.event.FraudDetectedEvent;
import com.bankingsystem.transactionservice.event.TransactionCompletedEvent;
import com.bankingsystem.transactionservice.event.TransactionInitiatedEvent;
import com.bankingsystem.transactionservice.repository.JpaTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final JpaTransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";

    public TransactionResponse transfer(TransferRequest request) {
        log.info("Transfer request received for account: {} to account: {} with amount: {}",
                request.senderAccountNumber(),
                request.receiverAccountNumber(),
                request.amount()
        );

        log.info("Deducting balance from sender account: {} for amount: {}", request.senderAccountNumber(), request.amount());
        this.accountServiceClient.deductAmount(request.senderAccountNumber(), request.amount());

        TransactionEntity transaction = new TransactionEntity();
        transaction.setSenderAccountNumber(request.senderAccountNumber());
        transaction.setReceiverAccountNumber(request.receiverAccountNumber());
        transaction.setAmount(request.amount());
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setReferenceNumber("REF-" + System.currentTimeMillis());
        transaction.setDescription(request.description());

        TransactionEntity savedTransaction = this.transactionRepository.save(transaction);
        log.info("Transaction saved with ID: {}", savedTransaction.getId());

        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getReceiverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription()
        );

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);
        log.info("TransactionInitiatedEvent published to Kafka for transaction ID: {}", savedTransaction.getId());

        return this.mapToResponse(savedTransaction);

    }

    public TransactionResponse getTransaction(String transactionId) {
        TransactionEntity transaction = this.transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));
        return this.mapToResponse(transaction);
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        return this.transactionRepository
                .findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TransactionResponse verifyOTP(String transactionId, String otp) {
        log.info("Verifying OTP for transaction ID: {}", transactionId);

        TransactionEntity transaction = this.transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

        String otpKey = "otp:" + transactionId;
        String storedOtp = this.redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            log.warn("OTP expired or not found for transaction ID: {}", transactionId);
            // compensate transaction method
            this.compensateTransaction(transaction, "OTP expired. The transaction has been canceled and the amount has been refunded.");
            return this.mapToResponse(transaction);
            /*transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("OTP expired or not found");
            this.transactionRepository.save(transaction);
            throw new RuntimeException("OTP expired or not found for transaction ID: " + transactionId);*/
        }

        if (!storedOtp.equals(otp)) {
            log.warn("Invalid OTP provided for transaction ID: {}", transactionId);
            this.redisTemplate.delete(otpKey);
            this.blockAccountAndCompensate(transaction, "Invalid OTP provided. The transaction has been canceled and the amount has been refunded. The account has been blocked for security reasons.");
            return this.mapToResponse(transaction);
        }

        log.info("OTP verified successfully for transaction ID: {}", transactionId);
        this.redisTemplate.delete(otpKey);
        this.completeTransaction(transaction);
        return this.mapToResponse(transaction);
    }

    public void processFraudCheckPassed(String transactionId) {
        log.info("Processing fraud check passed for transaction ID: {}", transactionId);
        TransactionEntity transaction = this.transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            log.warn("Transaction {} is not in PROCESSING state. Current state: {}", transactionId, transaction.getStatus());
            return;
        }

        this.completeTransaction(transaction);
    }

    private void completeTransaction(TransactionEntity transaction) {
        log.info("Completing transaction ID: {}", transaction.getId());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        this.transactionRepository.save(transaction);

        // Publish an event to notify the receiver about the completed transaction
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount(),
                transaction.getDescription()
        );
        this.kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transaction.getId(), event);
    }

    private void blockAccountAndCompensate(TransactionEntity transaction, String message) {
        // Publish fraud detected event to block the account
        log.info("Publishing fraud detected event for account: {}", transaction.getSenderAccountNumber());

        FraudDetectedEvent fraudDetectedEvent = new FraudDetectedEvent(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                message
        );
        this.kafkaTemplate.send(FRAUD_DETECTED_TOPIC, transaction.getSenderAccountNumber(), fraudDetectedEvent);
        // Compensate the transaction
        this.compensateTransaction(transaction, message);
    }

    private void compensateTransaction(TransactionEntity transaction, String message) {
        log.info("Compensating transaction ID: {}. Reason: {}", transaction.getId(), message);
        this.accountServiceClient.creditBalance(transaction.getSenderAccountNumber(), transaction.getAmount());
        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(message + " The amount has been refunded to the sender's account at " + LocalDateTime.now());
        this.transactionRepository.save(transaction);

        // Publish an event to notify the user about the compensation
        // You can create a CompensationEvent class and publish it to a Kafka topic if needed
        CompensationEvent event = new CompensationEvent(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getAmount(),
                message
        );
        this.kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(), event);
    }

    private TransactionResponse mapToResponse(TransactionEntity savedTransaction) {
        return new TransactionResponse(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getReceiverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getTransactionType(),
                savedTransaction.getStatus(),
                savedTransaction.getDescription(),
                savedTransaction.getFailureReason(),
                savedTransaction.getReferenceNumber(),
                savedTransaction.getCreatedAt(),
                savedTransaction.getCompletedAt()
        );
    }
}
