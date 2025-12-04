package com.healthapp.auth.repository;

import com.healthapp.auth.entity.User;
import com.healthapp.auth.Enums.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByEmailVerificationToken(String token);
    
    @Query("{ 'roles': ?0, 'isActivated': false }")
    List<User> findPendingUsersByRole(UserRole role);
    
    @Query("{ 'isActivated': false, 'roles': 'DOCTOR' }")
    List<User> findPendingDoctors();
    
    List<User> findByRolesContaining(UserRole role);
    
    long countByRolesContaining(UserRole role);
    
    @Query("{ 'roles': 'DOCTOR', 'isActivated': true }")
    List<User> findActivatedDoctors();
    
    @Query("{ 'accountStatus': ?0 }")
    List<User> findByAccountStatus(String accountStatus);
    Optional<User> findByKeycloakId(String keycloakId);
    boolean existsByKeycloakId(String keycloakId);

}