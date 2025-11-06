package com.healthapp.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_fcm_tokens")
public class UserFcmToken {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    private String fcmToken;
    
    private String deviceType;  // ANDROID, IOS, WEB
    
    private String deviceModel;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime lastUpdated;
}
