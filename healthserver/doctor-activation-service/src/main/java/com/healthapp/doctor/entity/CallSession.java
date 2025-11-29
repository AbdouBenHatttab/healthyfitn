package com.healthapp.doctor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * CallSession - Stocke les métadonnées des sessions d'appel WebRTC
 * NE stocke PAS les flux média (compliance)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "call_sessions")
public class CallSession {

    @Id
    private String id;

    // Lié au rendez-vous
    private String appointmentId;

    // Participants
    private String doctorId;
    private String doctorEmail;
    private String patientId;
    private String patientEmail;

    // Type d'appel
    private String callType; // AUDIO, VIDEO

    // Statut
    @Builder.Default
    private String status = "INITIATED"; // INITIATED, RINGING, ACTIVE, ENDED, FAILED

    // Signaling
    private String offerSdp;        // SDP de l'offre
    private String answerSdp;       // SDP de la réponse
    private String initiatorRole;   // DOCTOR ou PATIENT

    // ICE Servers (Cloudflare TURN)
    private String iceServers;      // JSON des serveurs STUN/TURN

    // Timing
    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    // Qualité (QoS - à logger)
    private Integer durationSeconds;
    private String endReason;       // COMPLETED, CANCELLED, FAILED, NO_ANSWER

    // Metadata pour audit
    private String deviceInfo;      // Infos device du patient
    private String networkType;     // WIFI, 4G, 5G

    // Business Methods
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean canBeJoined() {
        return "INITIATED".equals(status) || "RINGING".equals(status);
    }

    public void markAsActive() {
        this.status = "ACTIVE";
        this.startedAt = LocalDateTime.now();
    }

    public void end(String reason) {
        this.status = "ENDED";
        this.endedAt = LocalDateTime.now();
        this.endReason = reason;
        if (startedAt != null) {
            this.durationSeconds = (int) java.time.Duration.between(startedAt, endedAt).getSeconds();
        }
    }
}