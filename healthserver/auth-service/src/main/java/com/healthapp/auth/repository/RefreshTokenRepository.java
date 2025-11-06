package com.healthapp.auth.repository;

import com.healthapp.auth.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUserId(String userId);
    
    void deleteByToken(String token);
    
    void deleteByUserId(String userId);
    
    void deleteByExpiryDateBefore(LocalDateTime now);
    
    List<RefreshToken> findByUserIdAndIsRevokedFalse(String userId);
}

