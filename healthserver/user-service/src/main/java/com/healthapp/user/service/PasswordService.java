package com.healthapp.user.service;

import com.healthapp.user.dto.request.ChangePasswordRequest;
import com.healthapp.user.entity.User;
import com.healthapp.user.exception.InvalidPasswordException;
import com.healthapp.user.exception.UserNotFoundException;
import com.healthapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Changer le mot de passe d’un utilisateur
     */
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("🔄 Changement de mot de passe pour l'utilisateur : {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'ID : " + userId));

        // Vérifier le mot de passe actuel
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.error("Mot de passe actuel incorrect pour l'utilisateur : {}", userId);
            throw new InvalidPasswordException("Le mot de passe actuel est incorrect");
        }

        // Vérifier que le nouveau mot de passe est différent
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("✅ Mot de passe modifié avec succès pour l'utilisateur : {}", userId);
    }
}
