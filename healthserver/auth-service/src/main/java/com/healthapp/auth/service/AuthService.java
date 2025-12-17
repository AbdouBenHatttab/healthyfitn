package com.healthapp.auth.service;

import com.healthapp.auth.Enums.AccountStatus;
import com.healthapp.auth.dto.request.LoginRequest;
import com.healthapp.auth.dto.request.RegisterRequest;
import com.healthapp.auth.dto.response.AuthResponse;
import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.Enums.UserRole;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.exception.UserAlreadyExistsException;
import com.healthapp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service d'authentification avec Keycloak
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KeycloakAdminService keycloakAdminService;
    private final RestTemplateBuilder restTemplateBuilder;
    private RestTemplate restTemplate;
    @Autowired
    private final UserRepository userRepository;


    @Value("${keycloak.serverUrl:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:health-app-realm}")
    private String realm;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:health-backend-services}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret:${KEYCLOAK_CLIENT_SECRET:iMeoAcmu6sVppVs5X523cmfBCsJmdWbA}}")
    private String clientSecret;

    @PostConstruct
    public void init() {
        // Créer RestTemplate avec timeouts
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        log.info("✅ RestTemplate initialisé avec timeouts (10s connect, 10s read)");
    }

    /**
     * Inscription d'un utilisateur normal
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("📝 Inscription d'un utilisateur : {}", request.getEmail());

        if (keycloakAdminService.userExists(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Un utilisateur existe déjà avec cet email : " + request.getEmail()
            );
        }

        List<String> roles = new ArrayList<>();
        roles.add(request.getRole().name());
        if (request.getRole() != UserRole.ADMIN) {
            roles.add("USER");
        }

        String keycloakUserId;

        if (request.getRole() == UserRole.DOCTOR) {
            keycloakUserId = keycloakAdminService.createDoctor(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getMedicalLicenseNumber(),
                    request.getSpecialization(),
                    request.getHospitalAffiliation(),
                    request.getYearsOfExperience()
            );

            log.info("👨‍⚕️ Médecin créé dans Keycloak (en attente d'activation) : {}", request.getEmail());

            return AuthResponse.builder()
                    .userId(keycloakUserId)
                    .user(UserResponse.builder()
                            .id(keycloakUserId)
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .roles(Set.of(UserRole.DOCTOR))
                            .isActivated(false)
                            .build())
                    .build();

        } else {
            // ✅ Passer phoneNumber à createUser
            keycloakUserId = keycloakAdminService.createUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhoneNumber(), // ✅ AJOUT DU PHONE NUMBER
                    roles
            );

            log.info("✅ Utilisateur créé et activé dans Keycloak : {}", request.getEmail());

            Map<String, Object> tokenResponse = getTokenFromKeycloak(
                    request.getEmail(),
                    request.getPassword()
            );

            // ✅ Sauvegarder dans MongoDB avec le phoneNumber
            User user = User.builder()
                    .keycloakId(keycloakUserId)
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .birthDate(request.getBirthDate())
                    .gender(request.getGender())
                    .phoneNumber(request.getPhoneNumber()) // ✅ AJOUT DU PHONE NUMBER
                    .roles(Set.of(UserRole.USER))
                    .accountStatus(AccountStatus.ACTIVE)
                    .isActivated(true)
                    .isEmailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            User savedUser = userRepository.save(user);
            log.info("✅ User saved to MongoDB with phone: {}", savedUser.getPhoneNumber());

            return buildAuthResponse(keycloakUserId, tokenResponse, request.getEmail());
        }
    }

    /**
     * Connexion d'un utilisateur
     */
    public AuthResponse login(LoginRequest request) {
        log.info("🔐 Tentative de connexion : {}", request.getEmail());

        if (!keycloakAdminService.userExists(request.getEmail())) {
            log.warn("❌ Tentative de connexion avec un email inexistant : {}", request.getEmail());
            throw new RuntimeException("Aucun compte trouvé avec cette adresse email");
        }

        try {
            log.debug("📤 Demande de token à Keycloak pour : {}", request.getEmail());

            Map<String, Object> tokenResponse = getTokenFromKeycloak(
                    request.getEmail(),
                    request.getPassword()
            );

            log.debug("📥 Token reçu de Keycloak");

            Optional<UserRepresentation> keycloakUser =
                    keycloakAdminService.getUserByEmail(request.getEmail());

            if (keycloakUser.isEmpty()) {
                throw new RuntimeException("Utilisateur introuvable dans Keycloak");
            }

            String userId = keycloakUser.get().getId();

            log.info("✅ Connexion réussie pour : {}", request.getEmail());

            return buildAuthResponse(userId, tokenResponse, request.getEmail());

        } catch (HttpClientErrorException e) {
            log.error("❌ Erreur HTTP client {} lors de la connexion pour {}: {}",
                    e.getStatusCode(), request.getEmail(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Email ou mot de passe incorrect");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new RuntimeException("Requête invalide. Vérifiez la configuration du client Keycloak.");
            }
            throw new RuntimeException("Erreur lors de la connexion: " + e.getMessage());

        } catch (HttpServerErrorException e) {
            log.error("❌ Erreur serveur Keycloak {} lors de la connexion pour {}: {}",
                    e.getStatusCode(), request.getEmail(), e.getResponseBodyAsString());
            throw new RuntimeException("Erreur du serveur d'authentification");

        } catch (ResourceAccessException e) {
            log.error("❌ Impossible de joindre Keycloak : {}", e.getMessage());
            throw new RuntimeException("Le serveur d'authentification est inaccessible");

        } catch (Exception e) {
            log.error("❌ Échec de connexion pour {} : {}", request.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la connexion: " + e.getMessage());
        }
    }

    /**
     * Demander un token à Keycloak avec le grant type "password"
     */
    private Map<String, Object> getTokenFromKeycloak(String email, String password) {
        String tokenUrl = String.format(
                "%s/realms/%s/protocol/openid-connect/token",
                keycloakServerUrl,
                realm
        );

        log.debug("🌐 URL du token Keycloak : {}", tokenUrl);
        log.debug("🔑 Client ID : {}", clientId);
        log.debug("👤 Username : {}", email);

        // Préparer les paramètres de la requête
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);  // ← AJOUTÉ
        requestBody.add("username", email);
        requestBody.add("password", password);
        requestBody.add("grant_type", "password");
        requestBody.add("scope", "openid");

        // Configurer les headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(requestBody, headers);

        log.debug("📤 Envoi de la requête token à Keycloak...");

        try {
            // Faire la requête
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.debug("📥 Réponse reçue de Keycloak : status = {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("❌ Réponse inattendue de Keycloak : {}", response.getStatusCode());
                throw new RuntimeException("Impossible d'obtenir un token depuis Keycloak");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("❌ Exception lors de l'appel à Keycloak : {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Rafraîchir un token avec le refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.info("🔄 Rafraîchissement du token");

        String tokenUrl = String.format(
                "%s/realms/%s/protocol/openid-connect/token",
                keycloakServerUrl,
                realm
        );

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("refresh_token", refreshToken);
        requestBody.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Impossible de rafraîchir le token");
        }

        Map<String, Object> tokenResponse = response.getBody();

        log.info("✅ Token rafraîchi avec succès");

        return AuthResponse.builder()
                .accessToken((String) tokenResponse.get("access_token"))
                .refreshToken((String) tokenResponse.get("refresh_token"))
                .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                .tokenType("Bearer")
                .issuedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Déconnexion (révocation du refresh token)
     */
    public void logout(String refreshToken) {
        log.info("🚪 Déconnexion");

        String logoutUrl = String.format(
                "%s/realms/%s/protocol/openid-connect/logout",
                keycloakServerUrl,
                realm
        );

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(
                    logoutUrl,
                    HttpMethod.POST,
                    request,
                    Void.class
            );
            log.info("✅ Déconnexion réussie");
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la déconnexion : {}", e.getMessage());
        }
    }

    /**
     * Construire la réponse d'authentification
     */
    /**
     * Construire la réponse d'authentification
     */
    private AuthResponse buildAuthResponse(String userId, Map<String, Object> tokenResponse, String email) {
        // ✅ PRIORITÉ 1: Récupérer l'utilisateur depuis MongoDB
        Optional<User> userOptional = userRepository.findByEmail(email);

        UserResponse userResponse = null;

        if (userOptional.isPresent()) {
            // ✅ Mapper l'entité User depuis MongoDB vers UserResponse
            User user = userOptional.get();
            userResponse = UserResponse.builder()
                    .id(user.getKeycloakId()) // ou user.getId() selon ce que vous voulez exposer
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .fullName(user.getFirstName() + " " + user.getLastName())
                    .birthDate(user.getBirthDate())
                    .gender(user.getGender())
                    .phoneNumber(user.getPhoneNumber()) // ✅ Le téléphone vient de MongoDB
                    .profilePictureUrl(user.getProfilePictureUrl())
                    .roles(user.getRoles())
                    .accountStatus(user.getAccountStatus())
                    .isEmailVerified(user.getIsEmailVerified())
                    .isActivated(user.getIsActivated())
                    .lastLoginAt(user.getLastLoginAt())
                    .createdAt(user.getCreatedAt())
                    .medicalLicenseNumber(user.getMedicalLicenseNumber())
                    .specialization(user.getSpecialization())
                    .hospitalAffiliation(user.getHospitalAffiliation())
                    .yearsOfExperience(user.getYearsOfExperience())
                    .activationDate(user.getActivationDate())
                    .build();

            log.info("✅ User data loaded from MongoDB: {}, phone: {}", email, user.getPhoneNumber());
        } else {
            // ✅ FALLBACK: Récupérer depuis Keycloak si l'utilisateur n'est pas dans MongoDB
            log.warn("⚠️ User not found in MongoDB, fetching from Keycloak: {}", email);
            Optional<UserRepresentation> keycloakUser = keycloakAdminService.getUserByEmail(email);

            if (keycloakUser.isPresent()) {
                userResponse = mapKeycloakUserToResponse(keycloakUser.get());
                log.info("✅ User data loaded from Keycloak: {}", email);
            } else {
                log.error("❌ User not found in MongoDB nor Keycloak: {}", email);
            }
        }

        return AuthResponse.builder()
                .userId(userId)
                .accessToken((String) tokenResponse.get("access_token"))
                .refreshToken((String) tokenResponse.get("refresh_token"))
                .idToken((String) tokenResponse.get("id_token"))
                .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                .tokenType("Bearer")
                .user(userResponse) // ✅ UserResponse au lieu de Optional<UserResponse>
                .issuedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Mapper un UserRepresentation Keycloak vers UserResponse
     */
    private UserResponse mapKeycloakUserToResponse(UserRepresentation keycloakUser) {
        Map<String, List<String>> attributes = keycloakUser.getAttributes();

        return UserResponse.builder()
                .id(keycloakUser.getId())
                .email(keycloakUser.getEmail())
                .firstName(keycloakUser.getFirstName())
                .lastName(keycloakUser.getLastName())
                .fullName(keycloakUser.getFirstName() + " " + keycloakUser.getLastName())
                .phoneNumber(getAttributeValue(attributes, "phoneNumber")) // ✅ AJOUT
                .isActivated(keycloakUser.isEnabled())
                .isEmailVerified(keycloakUser.isEmailVerified())
                .medicalLicenseNumber(getAttributeValue(attributes, "medicalLicenseNumber"))
                .specialization(getAttributeValue(attributes, "specialization"))
                .hospitalAffiliation(getAttributeValue(attributes, "hospitalAffiliation"))
                .build();
    }

    /**
     * Extraire une valeur d'attribut de Keycloak
     */
    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes == null || !attributes.containsKey(key)) {
            return null;
        }
        List<String> values = attributes.get(key);
        return values.isEmpty() ? null : values.get(0);
    }
}