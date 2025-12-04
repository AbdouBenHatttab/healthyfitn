package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.response.CallSessionResponse;
import com.healthapp.doctor.service.WebRTCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WebRTC Controller - Handles video/audio call signaling
 *
 * ✅ COMPATIBLE AVEC KEYCLOAK
 * Aucune modification nécessaire car:
 * - Utilise SecurityContextHolder (compatible OAuth2)
 * - Les rôles sont extraits automatiquement par Spring Security
 * - Authentication.getName() retourne le 'preferred_username' de Keycloak
 */
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
@Slf4j
public class WebRTCController {

    private final WebRTCService webRTCService;

    /**
     * Get existing call session for an appointment
     * This allows the second participant to join the same call
     */
    @GetMapping("/calls/appointment/{appointmentId}")
    public ResponseEntity<CallSessionResponse> getCallByAppointment(
            @PathVariable String appointmentId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("🔍 Get call for appointment: {}", appointmentId);
        log.info("   Requested by: {} (Keycloak)", auth.getName());

        try {
            CallSessionResponse response = webRTCService.getCallByAppointment(appointmentId);
            log.info("✅ Found existing call: {}", response.getCallId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("⚠️ No existing call found for appointment: {}", appointmentId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Initiate a call session
     * ✅ Accessible to ANY authenticated user (DOCTOR or USER)
     * ✅ Compatible with Keycloak OAuth2
     */
    @PostMapping("/initiate")
    public ResponseEntity<CallSessionResponse> initiateCall(
            @RequestBody InitiateCallRequest request) {

        // Get authenticated user from SecurityContext
        // With Keycloak, auth.getName() returns 'preferred_username'
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        log.info("========================================");
        log.info("📞 INITIATE CALL REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("User Email: {}", userEmail);
        log.info("User Authorities: {}", auth.getAuthorities());
        log.info("Appointment ID: {}", request.getAppointmentId());
        log.info("Call Type: {}", request.getCallType());
        log.info("========================================");

        try {
            CallSessionResponse response = webRTCService.initiateCall(
                    request.getAppointmentId(),
                    request.getCallType(),
                    userEmail
            );

            log.info("✅ Call session created successfully");
            log.info("   Call ID: {}", response.getCallId());
            log.info("   ICE Servers: {}", response.getIceServers() != null ? "Provided" : "None");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ ERROR creating call session", e);
            log.error("   Error type: {}", e.getClass().getSimpleName());
            log.error("   Error message: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Send offer SDP
     */
    @PostMapping("/calls/{callId}/offer")
    public ResponseEntity<Void> sendOffer(
            @PathVariable String callId,
            @RequestBody Map<String, String> sdpData) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("📤 Send Offer - User: {}, Call: {}", auth.getName(), callId);

        try {
            webRTCService.saveOfferSdp(callId, sdpData.get("sdp"));
            log.info("✅ Offer SDP saved successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error saving offer SDP", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Send answer SDP
     */
    @PostMapping("/calls/{callId}/answer")
    public ResponseEntity<Void> sendAnswer(
            @PathVariable String callId,
            @RequestBody Map<String, String> sdpData) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("📥 Send Answer - User: {}, Call: {}", auth.getName(), callId);

        try {
            webRTCService.saveAnswerSdp(callId, sdpData.get("sdp"));
            log.info("✅ Answer SDP saved successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error saving answer SDP", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Send ICE candidate
     */
    @PostMapping("/calls/{callId}/ice-candidate")
    public ResponseEntity<Void> sendIceCandidate(
            @PathVariable String callId,
            @RequestBody Map<String, Object> candidateData) {

        log.debug("🧊 ICE candidate for call: {}", callId);

        try {
            // TODO: Store ICE candidates in database if needed
            log.debug("ICE candidate: {}", candidateData);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error saving ICE candidate", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get call session details
     */
    @GetMapping("/calls/{callId}")
    public ResponseEntity<CallSessionResponse> getCallSession(
            @PathVariable String callId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("📋 Get Call Session - User: {}, Call: {}", auth.getName(), callId);

        try {
            CallSessionResponse response = webRTCService.getCallSession(callId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting call session", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * End call
     */
    @PostMapping("/calls/{callId}/end")
    public ResponseEntity<Void> endCall(
            @PathVariable String callId,
            @RequestBody Map<String, String> reasonData) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String reason = reasonData.get("reason");

        log.info("🔵 End Call - User: {}, Call: {}, Reason: {}",
                auth.getName(), callId, reason);

        try {
            webRTCService.endCall(callId, reason);
            log.info("✅ Call ended successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error ending call", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DTO for initiate call request
     */
    public static class InitiateCallRequest {
        private String appointmentId;
        private String callType;

        // Getters and Setters
        public String getAppointmentId() {
            return appointmentId;
        }

        public void setAppointmentId(String appointmentId) {
            this.appointmentId = appointmentId;
        }

        public String getCallType() {
            return callType;
        }

        public void setCallType(String callType) {
            this.callType = callType;
        }
    }
}