package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.DoctorRegisterRequest;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.service.DoctorAuthService;
import com.healthapp.doctor.service.DoctorLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Contrôleur d'authentification des médecins
 * Endpoints publics pour l'inscription et la connexion
 *
 * Ces endpoints ne nécessitent PAS d'authentification
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorAuthController {

    private final DoctorAuthService doctorAuthService;
    private final DoctorLoginService doctorLoginService;

    /**
     * Inscrire un nouveau médecin (endpoint PUBLIC)
     *
     * Aucune authentification requise
     *
     * @param request Données d'inscription du médecin
     * @return DoctorResponse avec le statut de l'inscription
     */
    @PostMapping("/register")
    public ResponseEntity<DoctorResponse> registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        log.info("🏥 Demande d'inscription d'un médecin reçue pour : {}", request.getEmail());

        DoctorResponse response = doctorAuthService.registerDoctor(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Connexion d'un médecin (endpoint PUBLIC)
     *
     * Aucune authentification requise
     * Retourne le token d'accès et le refresh token si succès
     * Retourne une erreur si le compte n'est pas encore activé
     *
     * @param loginRequest Email et mot de passe
     * @return Token d'accès, refresh token et informations du médecin
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginDoctor(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        log.info("🔐 Demande de connexion pour le médecin : {}", email);

        Map<String, Object> response = doctorLoginService.loginDoctor(email, password);

        // Si le compte n'est pas activé, renvoyer 403 Forbidden
        if (response.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de vérification de l'état du service
     *
     * @return Statut du service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service d'activation des médecins opérationnel");
    }
}
