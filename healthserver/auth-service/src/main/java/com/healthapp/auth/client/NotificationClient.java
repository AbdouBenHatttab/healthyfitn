package com.healthapp.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationClient {
    
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to: {}, subject: {}", to, subject);
    }
}