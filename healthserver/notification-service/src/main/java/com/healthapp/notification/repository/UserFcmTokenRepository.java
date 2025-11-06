package com.healthapp.notification.repository;

import com.healthapp.notification.entity.UserFcmToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends MongoRepository<UserFcmToken, String> {
    
    Optional<UserFcmToken> findByUserId(String userId);
    
    Optional<UserFcmToken> findByFcmToken(String fcmToken);
    
    void deleteByUserId(String userId);
}
