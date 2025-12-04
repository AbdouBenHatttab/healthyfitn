package com.healthapp.user.repository;

import com.healthapp.user.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.healthapp.user.Enums.AccountStatus;
import com.healthapp.user.Enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    // 🔐 Recherche par Keycloak ID
    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    boolean existsByKeycloakId(String keycloakId);

    List<User> findByRolesContaining(UserRole role);

    @Query("{ 'accountStatus': ?0 }")
    List<User> findByAccountStatus(AccountStatus status);

    @Query("{ 'isActivated': false, 'roles': 'DOCTOR' }")
    List<User> findPendingDoctors();

    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);

    long countByRolesContaining(UserRole role);

    @Query("{ $or: [ { 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'email': { $regex: ?0, $options: 'i' } } ] }")
    Page<User> searchUsers(String keyword, Pageable pageable);
}