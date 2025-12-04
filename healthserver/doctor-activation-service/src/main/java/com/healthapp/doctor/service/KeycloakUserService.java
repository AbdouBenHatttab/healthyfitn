package com.healthapp.doctor.service;

import com.healthapp.doctor.dto.response.AuthResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.web.client.RestTemplateBuilder;

import jakarta.ws.rs.core.Response;
import jakarta.annotation.PostConstruct;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.*;

/**
 * Service pour gérer les utilisateurs dans Keycloak
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloak;
    private final RestTemplateBuilder restTemplateBuilder; // ✅ AJOUTEZ CECI
    private RestTemplate restTemplate;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.roles.doctor:DOCTOR}")
    private String doctorRole;

    /**
     * Initialisation du RestTemplate avec timeouts
     */
    @PostConstruct
    public void init() {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        log.info("✅ KeycloakUserService initialized with RestTemplate");
        log.info("🔐 Keycloak Server: {}", keycloakServerUrl);
        log.info("🔐 Realm: {}", realm);
        log.info("🔐 Client ID: {}", clientId);
    }

    /**
     * Créer un utilisateur doctor dans Keycloak
     */
    public String createDoctorUser(
            String email,
            String firstName,
            String lastName,
            String password,
            String userId) {

        log.info("========================================");
        log.info("🔐 CREATING DOCTOR USER IN KEYCLOAK");
        log.info("========================================");
        log.info("Email: {}", email);
        log.info("Name: {} {}", firstName, lastName);
        log.info("User ID: {}", userId);
        log.info("Password provided: {}", password != null && !password.isEmpty() ? "YES" : "NO");
        log.info("========================================");

        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Vérifier si l'utilisateur existe déjà
            List<UserRepresentation> existingUsers = usersResource.search(email, true);
            if (!existingUsers.isEmpty()) {
                log.warn("⚠️ User already exists in Keycloak: {}", email);
                return existingUsers.get(0).getId();
            }

            // Créer la représentation de l'utilisateur
            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(false); // DÉSACTIVÉ - Sera activé après validation admin
            user.setEmailVerified(false);

            // Attributs personnalisés
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("userId", List.of(userId));
            attributes.put("accountType", List.of("DOCTOR"));
            attributes.put("activationStatus", List.of("PENDING"));
            user.setAttributes(attributes);

            // Définir le mot de passe immédiatement
            if (password != null && !password.isEmpty()) {
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);
                user.setCredentials(List.of(credential));
                log.info("✅ Password configured for user creation");
            } else {
                log.warn("⚠️ No password provided - user won't be able to login");
            }

            // Créer l'utilisateur
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                String locationHeader = response.getHeaderString("Location");
                String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

                log.info("✅ User created in Keycloak with ID: {}", keycloakUserId);

                // Assigner le rôle DOCTOR
                assignDoctorRole(keycloakUserId);

                log.info("========================================");
                log.info("✅ DOCTOR USER CREATED SUCCESSFULLY");
                log.info("Keycloak ID: {}", keycloakUserId);
                log.info("Status: DISABLED (pending activation)");
                log.info("Password: CONFIGURED");
                log.info("========================================");

                return keycloakUserId;

            } else {
                String errorMsg = response.readEntity(String.class);
                log.error("❌ Failed to create user in Keycloak. Status: {}, Error: {}",
                        response.getStatus(), errorMsg);
                throw new RuntimeException("Failed to create user in Keycloak: " + errorMsg);
            }

        } catch (Exception e) {
            log.error("❌ Exception creating user in Keycloak", e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Login avec Keycloak - VERSION CORRIGÉE
     */
    public AuthResponse login(String email, String password) {
        log.info("========================================");
        log.info("🔐 DOCTOR LOGIN WITH KEYCLOAK");
        log.info("========================================");
        log.info("📧 Email: {}", email);
        log.info("🔑 Client ID: {}", clientId);
        log.info("🌐 Keycloak Server: {}", keycloakServerUrl);
        log.info("========================================");

        try {
            String tokenUrl = keycloakServerUrl
                    + "/realms/" + realm + "/protocol/openid-connect/token";

            log.debug("📤 Token URL: {}", tokenUrl);

            // Préparer les paramètres de la requête
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "password");
            body.add("username", email);
            body.add("password", password);

            // Configurer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            log.debug("📤 Sending token request to Keycloak...");

            // Faire la requête
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.debug("📥 Response received: status = {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("❌ Unexpected response from Keycloak: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get token from Keycloak");
            }

            Map<String, Object> token = response.getBody();

            log.info("========================================");
            log.info("✅ LOGIN SUCCESSFUL");
            log.info("========================================");

            return AuthResponse.builder()
                    .accessToken(token.get("access_token").toString())
                    .refreshToken(token.get("refresh_token").toString())
                    .expiresIn(Long.parseLong(token.get("expires_in").toString()))
                    .tokenType(token.get("token_type").toString())
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("❌ HTTP Client Error during login");
            log.error("Status: {}", e.getStatusCode());
            log.error("Response: {}", e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Invalid email or password");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new RuntimeException("Invalid request. Check Keycloak client configuration.");
            }
            throw new RuntimeException("Login failed: " + e.getMessage());

        } catch (HttpServerErrorException e) {
            log.error("❌ Keycloak Server Error during login");
            log.error("Status: {}", e.getStatusCode());
            log.error("Response: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Authentication server error");

        } catch (ResourceAccessException e) {
            log.error("❌ Cannot reach Keycloak server");
            log.error("Error: {}", e.getMessage());
            throw new RuntimeException("Authentication server is unreachable. Please try again later.");

        } catch (Exception e) {
            log.error("❌ Keycloak login failed", e);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    /**
     * Assigner le rôle DOCTOR à l'utilisateur
     */
    private void assignDoctorRole(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(keycloakUserId);

            // Récupérer le rôle DOCTOR
            RoleRepresentation doctorRoleRep = realmResource.roles().get(doctorRole).toRepresentation();

            // Assigner le rôle
            userResource.roles().realmLevel().add(List.of(doctorRoleRep));

            log.info("✅ Role {} assigned to user {}", doctorRole, keycloakUserId);

        } catch (Exception e) {
            log.error("❌ Failed to assign role to user", e);
            throw new RuntimeException("Failed to assign role: " + e.getMessage(), e);
        }
    }

    /**
     * Activer un utilisateur doctor dans Keycloak après validation admin
     */
    public void enableDoctorUser(String keycloakUserId) {
        log.info("========================================");
        log.info("✅ ENABLING DOCTOR USER IN KEYCLOAK");
        log.info("Keycloak ID: {}", keycloakUserId);
        log.info("========================================");

        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(keycloakUserId);

            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(true); // ACTIVER LE COMPTE

            // Mettre à jour le statut d'activation
            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put("activationStatus", List.of("APPROVED"));
            user.setAttributes(attributes);

            userResource.update(user);

            log.info("✅ Doctor user enabled in Keycloak: {}", keycloakUserId);

            // Envoyer un email pour définir le mot de passe
            sendPasswordSetupEmail(keycloakUserId);

        } catch (Exception e) {
            log.error("❌ Failed to enable user in Keycloak", e);
            throw new RuntimeException("Failed to enable user: " + e.getMessage(), e);
        }
    }

    /**
     * Envoyer un email pour définir le mot de passe (Keycloak action)
     */
    private void sendPasswordSetupEmail(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(keycloakUserId);

            // Envoyer l'action "UPDATE_PASSWORD" par email
            userResource.executeActionsEmail(List.of("UPDATE_PASSWORD"));

            log.info("📧 Password setup email sent to user: {}", keycloakUserId);

        } catch (Exception e) {
            log.warn("⚠️ Could not send password setup email: {}", e.getMessage());
        }
    }

    /**
     * Désactiver un utilisateur doctor (en cas de rejet)
     */
    public void disableDoctorUser(String keycloakUserId, String reason) {
        log.info("❌ Disabling doctor user in Keycloak: {}", keycloakUserId);

        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(keycloakUserId);

            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(false);

            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put("activationStatus", List.of("REJECTED"));
            attributes.put("rejectionReason", List.of(reason));
            user.setAttributes(attributes);

            userResource.update(user);

            log.info("✅ Doctor user disabled in Keycloak: {}", keycloakUserId);

        } catch (Exception e) {
            log.error("❌ Failed to disable user in Keycloak", e);
            throw new RuntimeException("Failed to disable user: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifier si un utilisateur existe dans Keycloak
     */
    public boolean userExists(String email) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.search(email, true);
            return !users.isEmpty();

        } catch (Exception e) {
            log.error("❌ Error checking user existence", e);
            return false;
        }
    }

    /**
     * Récupérer un utilisateur par email
     */
    public Optional<UserRepresentation> getUserByEmail(String email) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.search(email, true);
            return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));

        } catch (Exception e) {
            log.error("❌ Error fetching user", e);
            return Optional.empty();
        }
    }
}