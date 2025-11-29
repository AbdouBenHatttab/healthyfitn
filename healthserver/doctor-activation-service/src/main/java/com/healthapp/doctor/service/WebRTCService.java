package com.healthapp.doctor.service;

import com.healthapp.doctor.dto.response.CallSessionResponse;
import com.healthapp.doctor.entity.Appointment;
import com.healthapp.doctor.entity.CallSession;
import com.healthapp.doctor.repository.AppointmentRepository;
import com.healthapp.doctor.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebRTCService {
    private final CallSessionRepository callSessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final MeteredTurnCredentialsGenerator credentialsGenerator;

    /**
     * Initier un appel (DOCTOR ou USER)
     * ✅ CORRIGÉ: USER au lieu de PATIENT
     */
    public CallSessionResponse initiateCall(String appointmentId, String callType, String initiatorEmail) {
        log.info("═══════════════════════════════════════════");
        log.info("📞 INITIATING CALL");
        log.info("═══════════════════════════════════════════");
        log.info("   Appointment ID: {}", appointmentId);
        log.info("   Call Type: {}", callType);
        log.info("   Initiator Email: {}", initiatorEmail);
        log.info("═══════════════════════════════════════════");

        // Vérifier que le rendez-vous existe
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("❌ Appointment not found: {}", appointmentId);
                    return new RuntimeException("Appointment not found");
                });

        // ✅ CORRECTION: Le rôle patient est "USER" et non "PATIENT"
        boolean isDoctorInitiating = appointment.getDoctorEmail().equals(initiatorEmail);
        String initiatorRole = isDoctorInitiating ? "DOCTOR" : "USER";

        log.info("📋 Appointment found:");
        log.info("   Doctor: {} ({})", appointment.getDoctorName(), appointment.getDoctorEmail());
        log.info("   Patient: {} ({})", appointment.getPatientName(), appointment.getPatientEmail());
        log.info("   Initiator role: {}", initiatorRole);

        // Générer des credentials TURN temporaires
        String iceServersJson = credentialsGenerator.generateIceServersJson();
        log.info("🧊 Generated ICE servers (Metered.ca)");

        // Créer la session d'appel
        CallSession session = CallSession.builder()
                .appointmentId(appointmentId)
                .doctorId(appointment.getDoctorId())
                .doctorEmail(appointment.getDoctorEmail())
                .patientId(appointment.getPatientId())
                .patientEmail(appointment.getPatientEmail())
                .callType(callType)
                .status("INITIATED")
                .initiatorRole(initiatorRole)  // ✅ Sera "USER" ou "DOCTOR"
                .iceServers(iceServersJson)
                .createdAt(LocalDateTime.now())
                .build();

        CallSession saved = callSessionRepository.save(session);

        log.info("═══════════════════════════════════════════");
        log.info("✅ CALL SESSION CREATED");
        log.info("═══════════════════════════════════════════");
        log.info("   Call ID: {}", saved.getId());
        log.info("   Initiator: {}", initiatorRole);
        log.info("   Status: {}", saved.getStatus());
        log.info("═══════════════════════════════════════════");

        // TODO: Envoyer notification push
        if (isDoctorInitiating) {
            log.info("📱 Should notify USER (patient): {}", appointment.getPatientEmail());
        } else {
            log.info("📱 Should notify DOCTOR: {}", appointment.getDoctorEmail());
        }

        return mapToResponse(saved);
    }

    public void saveOfferSdp(String callId, String sdp) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.setOfferSdp(sdp);
        session.setStatus("RINGING");
        callSessionRepository.save(session);

        log.info("📤 Offer SDP saved for call: {} (length: {} chars)", callId, sdp.length());
    }

    public void saveAnswerSdp(String callId, String sdp) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.setAnswerSdp(sdp);
        callSessionRepository.save(session);

        log.info("📥 Answer SDP saved for call: {} (length: {} chars)", callId, sdp.length());
    }

    public void markCallAsActive(String callId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.markAsActive();
        callSessionRepository.save(session);

        log.info("✅ Call {} is now ACTIVE", callId);
    }

    public void endCall(String callId, String reason) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.end(reason);
        callSessionRepository.save(session);

        log.info("🔵 Call {} ended: {} (Duration: {}s)",
                callId, reason, session.getDurationSeconds());
    }

    public CallSessionResponse getCallSession(String callId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        return mapToResponse(session);
    }

    public Map<String, Object> getCallQuality(String callId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        Map<String, Object> qos = new HashMap<>();
        qos.put("callId", session.getId());
        qos.put("durationSeconds", session.getDurationSeconds());
        qos.put("status", session.getStatus());
        qos.put("endReason", session.getEndReason());
        qos.put("networkType", session.getNetworkType());
        qos.put("callType", session.getCallType());

        return qos;
    }
/**
 * Get existing call session by appointment ID
 */
public CallSessionResponse getCallByAppointment(String appointmentId) {
    log.info("🔍 Looking for call session for appointment: {}", appointmentId);
    
    CallSession session = callSessionRepository
            .findByAppointmentIdAndStatus(
                appointmentId, 
                List.of("INITIATED", "RINGING", "ACTIVE")
            )
            .orElseThrow(() -> new RuntimeException("No active call found for this appointment"));
    
    log.info("✅ Found call session: {}", session.getId());
    return mapToResponse(session);
}
    private CallSessionResponse mapToResponse(CallSession session) {
        return CallSessionResponse.builder()
                .callId(session.getId())
                .appointmentId(session.getAppointmentId())
                .doctorId(session.getDoctorId())
                .doctorEmail(session.getDoctorEmail())
                .patientId(session.getPatientId())
                .patientEmail(session.getPatientEmail())
                .callType(session.getCallType())
                .status(session.getStatus())
                .initiatorRole(session.getInitiatorRole())
                .iceServers(session.getIceServers())
                .offerSdp(session.getOfferSdp())
                .answerSdp(session.getAnswerSdp())
                .createdAt(session.getCreatedAt())
                .build();
    }
}