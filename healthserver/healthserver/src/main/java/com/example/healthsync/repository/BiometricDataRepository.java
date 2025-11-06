package com.example.healthsync.repository;

import com.example.healthsync.model.BiometricData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricDataRepository extends MongoRepository<BiometricData, String> {

    List<BiometricData> findByUserId(String userId);
    Optional<BiometricData> findByUserIdAndDate(String userId, String date);
    List<BiometricData> findByUserIdOrderByReceivedAtDesc(String userId);
    long countByUserId(String userId);
}