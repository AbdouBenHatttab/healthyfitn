package com.healthapp.user.service;

import com.healthapp.user.Enums.AccountStatus;
import com.healthapp.user.Enums.UserRole;
import com.healthapp.user.config.KeycloakConfig;
import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import jakarta.ws.rs.NotFoundException;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Service pour synchroniser les utilisateurs entre Keycloak et MongoDB
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class KeycloakSyncService {

    private final Keycloak keycloak;
    private final KeycloakConfig keycloakConfig;
    private final UserRepository userRepository;

    /**
     * Synchronise un utilisateur Keycloak vers MongoDB
     */
    public User syncUserFromKeycloak(String keycloakId) {
        log.info("🔄 Synchronisation de l'utilisateur Keycloak: {}", keycloakId);

        // Récupérer l'utilisateur depuis Keycloak
        UserRepresentation keycloakUser = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .get(keycloakId)
                .toRepresentation();

        // Vérifier si l'utilisateur existe déjà dans MongoDB
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    log.info("✨ Nouvel utilisateur, création dans MongoDB");
                    return User.builder()
                            .keycloakId(keycloakId)
                            .email(keycloakUser.getEmail())
                            .createdAt(LocalDateTime.now())
                            .build();
                });

        // Mettre à jour les informations
        user.setFirstName(keycloakUser.getFirstName());
        user.setLastName(keycloakUser.getLastName());
        user.setEmail(keycloakUser.getEmail());
        user.setIsEmailVerified(keycloakUser.isEmailVerified());
        user.setIsActivated(keycloakUser.isEnabled());
        user.setAccountStatus(keycloakUser.isEnabled() ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);

        // Synchroniser les rôles
        Set<UserRole> roles = extractRolesFromKeycloak(keycloakUser);
        user.setRoles(roles);

        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("✅ Utilisateur synchronisé: {} (MongoDB ID: {})", saved.getEmail(), saved.getId());

        return saved;
    }

    /**
     * Synchronise tous les utilisateurs Keycloak vers MongoDB
     */
    public void syncAllUsersFromKeycloak() {
        log.info("🔄 Synchronisation de tous les utilisateurs Keycloak");

        List<UserRepresentation> keycloakUsers = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .list();

        log.info("📊 {} utilisateurs trouvés dans Keycloak", keycloakUsers.size());

        for (UserRepresentation keycloakUser : keycloakUsers) {
            try {
                syncUserFromKeycloak(keycloakUser.getId());
            } catch (Exception e) {
                log.error("❌ Erreur lors de la synchronisation de l'utilisateur {}: {}",
                        keycloakUser.getEmail(), e.getMessage());
            }
        }

        log.info("✅ Synchronisation terminée");
    }

    /**
     * Extrait les rôles depuis un UserRepresentation Keycloak
     */
    private Set<UserRole> extractRolesFromKeycloak(UserRepresentation keycloakUser) {
        Set<UserRole> roles = new HashSet<>();

        // Récupérer les rôles realm
        List<String> realmRoles = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .get(keycloakUser.getId())
                .roles()
                .realmLevel()
                .listEffective()
                .stream()
                .map(role -> role.getName())
                .toList();

        // Mapper les rôles Keycloak vers les rôles de l'application
        if (realmRoles.contains(keycloakConfig.getRoles().getAdmin())) {
            roles.add(UserRole.ADMIN);
        }
        if (realmRoles.contains(keycloakConfig.getRoles().getDoctor())) {
            roles.add(UserRole.DOCTOR);
        }
        if (realmRoles.contains(keycloakConfig.getRoles().getUser())) {
            roles.add(UserRole.USER);
        }

        // Si aucun rôle, attribuer USER par défaut
        if (roles.isEmpty()) {
            roles.add(UserRole.USER);
        }

        return roles;
    }

    /**
     * Synchronise un utilisateur MongoDB vers Keycloak (mise à jour)
     */
    public void syncUserToKeycloak(String mongoUserId) {
        log.info("🔄 Synchronisation de l'utilisateur MongoDB vers Keycloak: {}", mongoUserId);

        User user = userRepository.findById(mongoUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + mongoUserId));

        if (user.getKeycloakId() == null) {
            log.warn("⚠️ Utilisateur sans Keycloak ID: {}", user.getEmail());
            return;
        }

        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setFirstName(user.getFirstName());
        keycloakUser.setLastName(user.getLastName());
        keycloakUser.setEmail(user.getEmail());
        keycloakUser.setEnabled(user.getIsActivated());
        keycloakUser.setEmailVerified(user.getIsEmailVerified());

        keycloak.realm(keycloakConfig.getRealm())
                .users()
                .get(user.getKeycloakId())
                .update(keycloakUser);

        log.info("✅ Utilisateur synchronisé vers Keycloak: {}", user.getEmail());
    }
    /**
     * Supprimer un utilisateur de Keycloak
     */
    public void deleteUserFromKeycloak(String keycloakUserId) {
        log.info("========================================");
        log.info("🔐 Deleting user from Keycloak");
        log.info("Keycloak User ID: {}", keycloakUserId);
        log.info("========================================");

        try {
            keycloak.realm(keycloakConfig.getRealm())
                    .users()
                    .delete(keycloakUserId);

            log.info("✅ User successfully deleted from Keycloak: {}", keycloakUserId);

        } catch (NotFoundException e) {
            log.warn("⚠️ User not found in Keycloak (already deleted?): {}", keycloakUserId);
        } catch (Exception e) {
            log.error("❌ Failed to delete user from Keycloak: {}", keycloakUserId, e);
            throw new RuntimeException("Failed to delete user from Keycloak: " + e.getMessage(), e);
        }
    }

}