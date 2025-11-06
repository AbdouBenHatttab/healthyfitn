package com.healthapp.auth.service;

import com.healthapp.auth.dto.request.LoginRequest;
import com.healthapp.auth.dto.request.RefreshTokenRequest;
import com.healthapp.auth.dto.request.RegisterRequest;
import com.healthapp.auth.dto.response.AuthResponse;
import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.RefreshToken;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.Enums.UserRole;
import com.healthapp.auth.exception.InvalidTokenException;
import com.healthapp.auth.exception.UserAlreadyExistsException;
import com.healthapp.auth.repository.RefreshTokenRepository;
import com.healthapp.auth.repository.UserRepository;
import com.healthapp.auth.security.JwtSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtSecurity jwtService;
    private final AuthenticationManager authenticationManager;
    
    /**
     * Enregistrement d'un utilisateur normal
     * Les médecins doivent s'enregistrer via le doctor-service
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }
        
        User user = buildUserFromRequest(request);
        User savedUser = userRepository.save(user);
        
        log.info("User registered successfully: {} with role: {}", savedUser.getEmail(), request.getRole());
        
        String accessToken = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = createRefreshToken(savedUser);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(mapToUserResponse(savedUser))
                .build();
    }
    
    /**
     * Créer un compte basique (appelé par les autres microservices via Feign)
     * Ce endpoint est utilisé par doctor-service pour créer les credentials
     */
    public AuthResponse createBasicAccount(RegisterRequest request) {
        log.info("Creating basic account for: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }
        
        User user = buildUserFromRequest(request);
        User savedUser = userRepository.save(user);
        
        log.info("Basic account created: {} with roles: {}", savedUser.getEmail(), savedUser.getRoles());
        
        // Pour les comptes créés par d'autres services, on ne génère pas de tokens
        // Le service appelant gérera l'authentification
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .user(mapToUserResponse(savedUser))
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());
        
        if (!userRepository.existsByEmail(request.getEmail())) {
            log.warn("Login attempt with non-existent email: {}", request.getEmail());
            throw new UsernameNotFoundException("No account found with this email address");
        }
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.resetFailedLoginAttempts();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            String accessToken = jwtService.generateToken(user);
            RefreshToken refreshToken = createRefreshToken(user);
            
            log.info("User logged in successfully: {}", user.getEmail());
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                    .user(mapToUserResponse(user))
                    .build();
                    
        } catch (AuthenticationException e) {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user != null) {
                user.incrementFailedLoginAttempts();
                userRepository.save(user);
            }
            log.warn("Authentication failed for email: {}", request.getEmail());
            throw e;
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        
        if (!refreshToken.isValid()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expired or revoked");
        }
        
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        
        String newAccessToken = jwtService.generateToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(mapToUserResponse(user))
                .build();
    }
    
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }
    
    /**
     * Activer un compte utilisateur (appelé par d'autres services)
     */
    public void activateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActivated(true);
        user.setActivationDate(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User activated: {}", user.getEmail());
    }
    
    /**
     * Vérifier si un utilisateur existe
     */
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Récupérer un utilisateur par email (pour les autres services)
     */
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return mapToUserResponse(user);
    }
    
    private User buildUserFromRequest(RegisterRequest request) {
        User.UserBuilder userBuilder = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(request.getRole()));
        
        // Les utilisateurs normaux sont activés directement
        // Les médecins sont créés non-activés et gérés par doctor-service
        if (request.getRole() == UserRole.DOCTOR) {
            userBuilder.isActivated(false);
            userBuilder.activationRequestDate(LocalDateTime.now());
        } else {
            userBuilder.isActivated(true);
        }
        
        return userBuilder.build();
    }
    
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(jwtService.generateRefreshToken(user))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .isActivated(user.getIsActivated())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
