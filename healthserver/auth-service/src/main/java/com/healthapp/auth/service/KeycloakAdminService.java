package com.healthapp.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.Response;
import java.util.*;

/**
 * Service pour gérer les utilisateurs dans Keycloak via l'Admin API
 */
@Service
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    // Propriétés pour l'authentification admin (Option 1: Master Admin)
    @Value("${keycloak.admin.realm:master}")
    private String adminRealm;

    @Value("${keycloak.admin.username:amine}")
    private String adminUsername;

    @Value("${keycloak.admin.password:Password123!}")
    private String adminPassword;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    private Keycloak keycloak;
    private RealmResource realmResource;

    @PostConstruct
    public void initKeycloak() {
        log.info("🔐 Initialisation du client Keycloak Admin...");
        log.info("Server URL: {}", serverUrl);
        log.info("Target Realm: {}", realm);
        log.info("Admin Realm: {}", adminRealm);
        log.info("Admin Username: {}", adminUsername);
        log.info("Admin Client ID: {}", adminClientId);

        try {
            this.keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(adminRealm)
                    .username(adminUsername)
                    .password(adminPassword)
                    .clientId(adminClientId)
                    .build();

            this.realmResource = keycloak.realm(realm);

            // Test de connexion
            realmResource.toRepresentation();

            log.info("✅ Client Keycloak Admin initialisé avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation du client Keycloak Admin", e);
            throw new RuntimeException("Impossible d'initialiser le client Keycloak Admin", e);
        }
    }

    @PreDestroy
    public void closeKeycloak() {
        if (keycloak != null) {
            keycloak.close();
            log.info("🔒 Client Keycloak Admin fermé");
        }
    }

    public String createUser(String email, String password, String firstName,
                             String lastName, List<String> roles) {
        log.info("👤 Création d'un utilisateur dans Keycloak : {}", email);

        UsersResource usersResource = realmResource.users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);

        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            String errorMsg = response.readEntity(String.class);
            log.error("❌ Erreur lors de la création de l'utilisateur : {} - {}",
                    response.getStatusInfo(), errorMsg);
            response.close();
            throw new RuntimeException("Impossible de créer l'utilisateur dans Keycloak: " + errorMsg);
        }

        String userId = extractUserIdFromResponse(response);
        log.info("✅ Utilisateur créé avec l'ID : {}", userId);

        setUserPassword(userId, password);
        assignRolesToUser(userId, roles);

        response.close();
        return userId;
    }

    public String createDoctor(String email, String password, String firstName, String lastName,
                               String medicalLicense, String specialization,
                               String hospital, Integer yearsOfExperience) {
        log.info("👨‍⚕️ Création d'un médecin dans Keycloak : {}", email);

        UsersResource usersResource = realmResource.users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(false);
        user.setEmailVerified(true);

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("medicalLicenseNumber", Collections.singletonList(medicalLicense));
        attributes.put("specialization", Collections.singletonList(specialization));
        attributes.put("hospitalAffiliation", Collections.singletonList(hospital));
        attributes.put("yearsOfExperience", Collections.singletonList(String.valueOf(yearsOfExperience)));
        attributes.put("isActivated", Collections.singletonList("false"));
        user.setAttributes(attributes);

        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            String errorMsg = response.readEntity(String.class);
            log.error("❌ Erreur lors de la création du médecin : {} - {}",
                    response.getStatusInfo(), errorMsg);
            response.close();
            throw new RuntimeException("Impossible de créer le médecin dans Keycloak: " + errorMsg);
        }

        String userId = extractUserIdFromResponse(response);
        log.info("✅ Médecin créé avec l'ID : {} (en attente d'activation)", userId);

        setUserPassword(userId, password);
        assignRolesToUser(userId, Arrays.asList("DOCTOR", "USER"));

        response.close();
        return userId;
    }

    public void activateDoctor(String userId) {
        log.info("✅ Activation du médecin : {}", userId);

        UserResource userResource = realmResource.users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        user.setEnabled(true);

        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put("isActivated", Collections.singletonList("true"));
        attributes.put("activationDate", Collections.singletonList(new Date().toString()));
        user.setAttributes(attributes);

        userResource.update(user);

        log.info("✅ Médecin activé avec succès");
    }

    public List<UserRepresentation> getPendingDoctors() {
        log.info("📋 Récupération des médecins en attente...");

        UsersResource usersResource = realmResource.users();
        List<UserRepresentation> allUsers = usersResource.list();

        List<UserRepresentation> pendingDoctors = new ArrayList<>();

        for (UserRepresentation user : allUsers) {
            if (hasRole(user.getId(), "DOCTOR") && !user.isEnabled()) {
                pendingDoctors.add(user);
            }
        }

        log.info("📋 {} médecin(s) en attente trouvés", pendingDoctors.size());
        return pendingDoctors;
    }

    private boolean hasRole(String userId, String roleName) {
        try {
            UserResource userResource = realmResource.users().get(userId);
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listEffective();

            return roles.stream()
                    .anyMatch(role -> role.getName().equals(roleName));
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la vérification du rôle pour l'utilisateur {}: {}",
                    userId, e.getMessage());
            return false;
        }
    }

    private void setUserPassword(String userId, String password) {
        try {
            UserResource userResource = realmResource.users().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);

            userResource.resetPassword(credential);
            log.info("🔐 Mot de passe défini pour l'utilisateur : {}", userId);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la définition du mot de passe pour {}: {}",
                    userId, e.getMessage());
            throw new RuntimeException("Impossible de définir le mot de passe", e);
        }
    }

    private void assignRolesToUser(String userId, List<String> roleNames) {
        try {
            UserResource userResource = realmResource.users().get(userId);

            List<RoleRepresentation> rolesToAdd = new ArrayList<>();

            for (String roleName : roleNames) {
                try {
                    RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                    if (role != null) {
                        rolesToAdd.add(role);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Rôle '{}' introuvable dans Keycloak, ignoré", roleName);
                }
            }

            if (!rolesToAdd.isEmpty()) {
                userResource.roles().realmLevel().add(rolesToAdd);
                log.info("👥 Rôles assignés à l'utilisateur {} : {}", userId, roleNames);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'assignation des rôles pour {}: {}",
                    userId, e.getMessage());
            throw new RuntimeException("Impossible d'assigner les rôles", e);
        }
    }

    private String extractUserIdFromResponse(Response response) {
        String location = response.getHeaderString("Location");
        if (location == null) {
            throw new RuntimeException("Impossible de récupérer l'ID de l'utilisateur");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public boolean userExists(String email) {
        try {
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> users = usersResource.search(email, true);
            boolean exists = !users.isEmpty();
            log.debug("Vérification existence utilisateur '{}': {}", email, exists);
            return exists;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification de l'existence de l'utilisateur '{}': {}",
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible de vérifier l'existence de l'utilisateur", e);
        }
    }

    public Optional<UserRepresentation> getUserByEmail(String email) {
        try {
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> users = usersResource.search(email, true);

            if (users.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(users.get(0));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de l'utilisateur '{}': {}",
                    email, e.getMessage());
            throw new RuntimeException("Impossible de récupérer l'utilisateur", e);
        }
    }
}