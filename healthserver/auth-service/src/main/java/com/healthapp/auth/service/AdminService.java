package com.healthapp.auth.service;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.Enums.UserRole;
import com.healthapp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Administrateur - Gestion de l’activation et du rejet des médecins
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;

    /**
     * Récupérer tous les médecins en attente (isActivated = false)
     */
    public List<UserResponse> getPendingDoctors() {
        log.info("Récupération des médecins en attente d’approbation...");

        List<User> pendingDoctors = userRepository.findPendingDoctors();

        log.info("{} médecin(s) en attente trouvés", pendingDoctors.size());

        return pendingDoctors.stream()
                .map(userService::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Activer un compte médecin
     */
    public void activateDoctor(String doctorId) {
        log.info("Activation du médecin avec l’ID : {}", doctorId);

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable avec l’ID : " + doctorId));

        if (!doctor.hasRole(UserRole.DOCTOR)) {
            throw new RuntimeException("L’utilisateur n’est pas un médecin");
        }

        if (doctor.getIsActivated()) {
            log.warn("Ce médecin est déjà activé : {}", doctor.getEmail());
            return;
        }

        // Activation du médecin
        doctor.setIsActivated(true);
        doctor.setActivationDate(LocalDateTime.now());
        userRepository.save(doctor);

        // Envoi d’un email de confirmation
        emailService.sendDoctorActivationConfirmation(doctor);

        log.info("✅ Médecin activé avec succès : {}", doctor.getEmail());
    }

    /**
     * Rejeter un compte médecin
     */
    public void rejectDoctor(String doctorId, String reason) {
        log.info("Rejet du médecin avec l’ID : {}", doctorId);

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable avec l’ID : " + doctorId));

        if (!doctor.hasRole(UserRole.DOCTOR)) {
            throw new RuntimeException("L’utilisateur n’est pas un médecin");
        }

        // isActivated reste false, mais on envoie un email de rejet
        // (ajouter un champ rejectionReason dans l’entité User si nécessaire)

        emailService.sendDoctorRejectionNotification(doctor, reason);

        log.info("❌ Médecin rejeté : {} - Raison : {}", doctor.getEmail(), reason);
    }

    /**
     * Obtenir le nombre de médecins en attente
     */
    public long getPendingDoctorsCount() {
        return userRepository.findPendingDoctors().size();
    }

    /**
     * Récupérer tous les médecins activés
     */
    public List<UserResponse> getActivatedDoctors() {
        log.info("Récupération des médecins activés...");

        List<User> activatedDoctors = userRepository.findActivatedDoctors();

        log.info("{} médecin(s) activé(s) trouvés", activatedDoctors.size());

        return activatedDoctors.stream()
                .map(userService::mapToUserResponse)
                .collect(Collectors.toList());
    }
}
