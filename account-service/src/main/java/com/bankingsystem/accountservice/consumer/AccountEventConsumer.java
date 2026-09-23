package com.bankingsystem.accountservice.consumer;

import com.bankingsystem.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    /**
     * This method listens to the "transaction_completed" topic for events indicating that a transaction has been completed.
     * Upon receiving such an event, it extracts the receiver's account number and the amount from the payload,
     * and then credits the specified amount to the receiver's account using the AccountService.
     *
     * @param payload A map containing the details of the completed transaction, including "receiverAccountNumber" and "amount".
     */
    @KafkaListener(topics = "transaction.completed", groupId = "account-service-group")
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload) {
        log.info("Received transaction completed event: {}", payload);

        try {
            String receiverAccount = (String) payload.get("receiverAccountNumber");
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            log.info("Crediting account {} with amount {}", receiverAccount, amount);
            this.accountService.creditBalance(receiverAccount, amount);
        } catch (Exception e) {
            log.error("Error occurred while processing transaction completed event", e);
        }
    }

    /**
     * This method listens to the "fraud_detected" topic for events indicating that fraud has been detected.
     * Upon receiving such an event, it extracts the account number from the payload,
     * and then blocks the specified account using the AccountService.
     *
     * @param payload A map containing the details of the fraud detection, including "accountNumber".
     */
    @KafkaListener(topics = "fraud.detected", groupId = "account-service-group")
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {
        log.info("Received fraud detected event: {}", payload);

        try {
            String accountNumber = (String) payload.get("accountNumber");
            log.info("Blocking account {} due to fraud detection", accountNumber);
            this.accountService.blockAccount(accountNumber);
        } catch (Exception e) {
            log.error("Error occurred while processing fraud detected event", e);
        }
    }

}
