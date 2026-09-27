package com.bankingsystem.frauddetectionservice.service;

import com.bankingsystem.frauddetectionservice.client.AccountServiceClient;
import com.bankingsystem.frauddetectionservice.dto.FraudCheckResult;
import com.bankingsystem.frauddetectionservice.event.FraudCheckPassedEvent;
import com.bankingsystem.frauddetectionservice.event.VerificationRequiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_PASSED_TOPIC = "fraud.check.passed";

    @Value("${fraud.detection.max-transactions-per-minute:5}")
    private int maxTransactionPerMinute;

    @Value("${fraud.detection.suspicious-amount-multiplier:5.0}")
    private double suspiciousAmountMultiplier;

    @Value("${fraud.detection.max-balance-percentage:0.90}")
    private double maxBalancePercentage;

    public void checkTransaction(Map<String, Object> payload) {
        log.info("Checking transaction for fraud detection: {}", payload);
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("senderAccountNumber");
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        // Fetch the account balance from the Account Service
        BigDecimal accountBalance = this.accountServiceClient.getAccountBalance(accountNumber);

        log.info("Account balance for account {}: {}", accountNumber, accountBalance);

        FraudCheckResult result = performFraudChecks(accountNumber, amount, accountBalance);

        if (result.fraud()) {
            log.warn("Suspicious transaction detected! Transaction ID: {}, Reason: {} - requesting OTP verification", transactionId, result.reason());
            // Here you can implement logic to handle the fraudulent transaction, e.g., notify, block, etc.

            VerificationRequiredEvent event = new VerificationRequiredEvent(
                    transactionId,
                    accountNumber,
                    amount,
                    result.reason()
            );

            this.kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId, event);
        } else {
            log.info("Transaction is not fraudulent. Transaction ID: {}", transactionId);

            // Create and send event to indicate that the fraud check passed
            // For example, you could create a FraudCheckPassedEvent and send it to the FRAUD_CHECK_PASSED_TOPIC
            FraudCheckPassedEvent event = new FraudCheckPassedEvent(
                    transactionId,
                    false,
                    null
            );

            this.kafkaTemplate.send(FRAUD_CHECK_PASSED_TOPIC, transactionId, event);
        }

    }

    private FraudCheckResult performFraudChecks(String accountNumber, BigDecimal amount, BigDecimal accountBalance) {

        // CHECK 1: Velocity check
        log.info("Performing velocity check for account {}: amount {}", accountNumber, amount);
        if (isVelocityExceeded(accountNumber)) {
            return new FraudCheckResult(true, "Too many transactions in 60 seconds, velocity check failed");
        }

        // CHECK 2: Amount check
        log.info("Performing amount check for account {}: amount {}", accountNumber, amount);
        if (isAmountSuspicious(accountNumber, amount)) {
            return new FraudCheckResult(true, "Unusual transaction amount, amount check failed");
        }

        // CHECK 3: Balance check
        log.info("Performing balance check for account {}: amount {}, account balance {}", accountNumber, amount, accountBalance);
        if (accountBalance.compareTo(BigDecimal.ZERO) > 0 && !isBalanceCheckPassed(accountBalance, amount)) {
            return new FraudCheckResult(true, "Insufficient account balance, balance check failed");
        }

        // If all checks pass, return a successful result
        return new FraudCheckResult(false, "All checks passed");
    }

    /**
     * Check if the number of transactions for the account exceeds the maximum allowed transactions per minute.
     *
     * @param accountNumber The account number associated with the transaction.
     * @return true if the transaction count exceeds the limit, false otherwise.
     */
    private boolean isVelocityExceeded(String accountNumber) {
        // Key to store the transaction count for the account in Redis
        String key = "fraud:velocity:" + accountNumber;

        // Increment the transaction count for the account in Redis
        Long transactionCount = this.redisTemplate.opsForValue().increment(key);

        // If this is the first transaction, set an expiration for the key to 60 seconds
        if (transactionCount != null && transactionCount == 1) {
            this.redisTemplate.expire(key, java.time.Duration.ofSeconds(60));
        }

        // Check if the transaction count exceeds the maximum allowed transactions per minute
        return transactionCount != null && transactionCount > this.maxTransactionPerMinute;
    }

    /**
     * Check if the transaction amount is suspicious based on historical average.
     * If the amount is greater than a certain multiplier of the average, it is considered suspicious.
     *
     * @param accountNumber The account number associated with the transaction.
     * @param amount        The transaction amount to check.
     * @return true if the amount is suspicious, false otherwise.
     */
    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount) {
        // Key to store the average amount for the account in Redis
        String key = "fraud:avg_amount:" + accountNumber;

        // Get the average amount from Redis
        String avgAmountStr = this.redisTemplate.opsForValue().get(key);

        // If no average amount is found, set it to the current amount and return false
        if (avgAmountStr == null) {
            this.redisTemplate.opsForValue().set(key, amount.toString());
            return false;
        }

        // Calculate the threshold for suspicious amount
        BigDecimal avgAmount = new BigDecimal(avgAmountStr);
        BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(this.suspiciousAmountMultiplier));

        // Update the average amount
        BigDecimal newAvgAmount = avgAmount
                .add(amount)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        this.redisTemplate.opsForValue().set(key, newAvgAmount.toString());
        
        // Check if the transaction amount exceeds the threshold
        return amount.compareTo(threshold) > 0;
    }

    /**
     * Check if the transaction amount is within the allowed percentage of the account balance.
     *
     * @param accountBalance The current balance of the account.
     * @param amount         The transaction amount to check.
     * @return true if the transaction amount is within the allowed percentage, false otherwise.
     */
    private boolean isBalanceCheckPassed(BigDecimal accountBalance, BigDecimal amount) {
        // Calculate the maximum allowed transaction amount based on the account balance and the configured percentage
        BigDecimal maxAllowed = accountBalance.multiply(BigDecimal.valueOf(this.maxBalancePercentage));

        // Check if the transaction amount is less than or equal to the maximum allowed amount
        return amount.compareTo(maxAllowed) <= 0;
    }

}
