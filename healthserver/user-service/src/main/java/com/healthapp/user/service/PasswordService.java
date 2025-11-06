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
    
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.error("Current password is incorrect for user: {}", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        // Validate new password is different
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password must be different from current password");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
    }
}