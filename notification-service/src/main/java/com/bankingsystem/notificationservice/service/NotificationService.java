package com.bankingsystem.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    /**
     * Simulates sending a notification to the user.
     *
     * @param accountNumber The account number of the user to notify.
     * @param subject       The subject of the notification.
     * @param message       The message content of the notification.
     */
    public void sendNotification(String accountNumber, String subject, String message) {
        log.info("***********************************");
        log.info("Sending notification to account: {}", accountNumber);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("***********************************");
    }
}
