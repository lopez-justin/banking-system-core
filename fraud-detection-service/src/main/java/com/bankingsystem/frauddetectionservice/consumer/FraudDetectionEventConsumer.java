package com.bankingsystem.frauddetectionservice.consumer;

import com.bankingsystem.frauddetectionservice.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-detection-service")
    public void consumeTransactionInitiated(@Payload Map<String, Object> payload) {
        log.info("Received transaction initiated event for fraud check: {}", payload);

        try {
            fraudDetectionService.checkTransaction(payload);
        } catch (Exception e) {
            log.error("Error occurred while checking transaction for fraud: {}", e.getMessage(), e);
        }
    }

}
