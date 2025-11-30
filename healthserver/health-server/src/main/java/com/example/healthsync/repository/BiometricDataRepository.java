package com.example.healthsync.repository;

import com.example.healthsync.model.BiometricData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricDataRepository extends MongoRepository<BiometricData, String> {

    BiometricData findTopByEmailAndDateOrderByReceivedAtDesc(String email, String date);

    BiometricData findByEmailAndDate(String email, String date);

    List<BiometricData> findByEmailOrderByReceivedAtDesc(String email);

    long countByEmail(String email);

}
