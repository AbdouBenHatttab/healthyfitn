package com.healthapp.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceClient {
    
    public void syncUser(String userId) {
        log.info("Syncing user with user-service: {}", userId);
    }
}
