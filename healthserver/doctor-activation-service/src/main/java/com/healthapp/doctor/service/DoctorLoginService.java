package com.healthapp.doctor.service;

import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import com.healthapp.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * DoctorLoginService - Gestion de l’authentification des médecins
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorLoginService {

    private final DoctorRepository doctorRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")  // 24h par défaut
    private Long jwtExpiration;

    /**
     * Connexion d’un médecin
     */
    public Map<String, Object> loginDoctor(String email, String password) {
        log.info("🔐 Tentative de connexion d’un médecin : {}", email);

        // Recherche du médecin par email
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe invalide"));

        // Vérification du mot de passe
        if (!passwordEncoder.matches(password, doctor.getPassword())) {
            log.error("❌ Mot de passe invalide pour : {}", email);
            throw new RuntimeException("Email ou mot de passe invalide");
        }

        // Vérifier si le compte est activé
        if (!doctor.getIsActivated()) {
            log.warn("⚠️ Compte médecin non activé : {}", email);
            return Map.of(
                    "error", "COMPTE_NON_ACTIVE",
                    "message", "Votre compte est en attente d'approbation par un administrateur.",
                    "activationStatus", doctor.getActivationStatus(),
                    "email", doctor.getEmail()
            );
        }

        // Génération du JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", doctor.getEmail());
        claims.put("userId", doctor.getUserId());
        claims.put("roles", List.of("DOCTOR"));
        claims.put("doctorId", doctor.getId());

        // Appel direct de la méthode statique
        String accessToken = JwtUtil.generateToken(claims, doctor.getEmail(), jwtExpiration, jwtSecret);
        String refreshToken = JwtUtil.generateToken(claims, doctor.getEmail(), jwtExpiration * 7, jwtSecret);

        log.info("✅ Connexion réussie pour le médecin : {}", email);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", doctor.getUserId(),
                "doctorId", doctor.getId(),
                "email", doctor.getEmail(),
                "fullName", doctor.getFullName(),
                "isActivated", doctor.getIsActivated(),
                "role", "DOCTOR"
        );
    }
}
