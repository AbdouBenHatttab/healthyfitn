package com.healthapp.doctor.service;

import com.healthapp.doctor.dto.request.AppointmentRequest;
import com.healthapp.doctor.dto.response.AppointmentResponse;
import com.healthapp.doctor.dto.response.DoctorStatsResponse;
import com.healthapp.doctor.dto.response.PatientInfoResponse;
import com.healthapp.doctor.entity.Appointment;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.AppointmentRepository;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de gestion des rendez-vous avec sécurité Keycloak
 *
 * ✅ Toutes les méthodes vérifient que le docteur a accès aux données
 * ✅ Protection contre les accès non autorisés
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 🔐 Vérifier que le rendez-vous appartient au docteur
     */
    private void verifyDoctorOwnsAppointment(String appointmentId, String doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("❌ Appointment not found: {}", appointmentId);
                    return new RuntimeException("Appointment not found");
                });

        if (!appointment.getDoctorId().equals(doctorId)) {
            log.error("❌ Unauthorized access attempt: Doctor {} tried to access appointment {} (belongs to {})",
                    doctorId, appointmentId, appointment.getDoctorId());
            throw new RuntimeException("Unauthorized: This appointment does not belong to you");
        }
    }

    /**
     * PATIENT: Create new appointment
     */
    public AppointmentResponse createAppointment(
            AppointmentRequest request,
            String patientId,
            String patientEmail,
            String patientName) {

        log.info("========================================");
        log.info("📅 CREATING NEW APPOINTMENT");
        log.info("========================================");
        log.info("Patient: {} ({})", patientName, patientEmail);
        log.info("Doctor ID: {}", request.getDoctorId());
        log.info("DateTime: {}", request.getAppointmentDateTime());
        log.info("========================================");

        // Verify doctor exists and is activated
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> {
                    log.error("❌ Doctor not found: {}", request.getDoctorId());
                    return new RuntimeException("Doctor not found");
                });

        if (!doctor.getIsActivated()) {
            log.error("❌ Doctor is not activated: {}", request.getDoctorId());
            throw new RuntimeException("Doctor is not activated");
        }

        // Create appointment
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .patientEmail(patientEmail)
                .patientName(patientName)
                .doctorId(doctor.getId())
                .doctorEmail(doctor.getEmail())
                .doctorName(doctor.getFullName())
                .specialization(doctor.getSpecialization())
                .appointmentDateTime(request.getAppointmentDateTime())
                .appointmentType(request.getAppointmentType())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status("PENDING")
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        log.info("✅ Appointment created with PENDING status");
        log.info("Appointment ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    /**
     * DOCTOR: Get all appointments for a doctor
     */
    public List<AppointmentResponse> getDoctorAppointments(String doctorId) {
        log.info("📋 Fetching all appointments for doctor: {}", doctorId);

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId);

        log.info("✅ Found {} appointments", appointments.size());

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * DOCTOR: Get upcoming appointments
     */
    public List<AppointmentResponse> getUpcomingAppointments(String doctorId) {
        log.info("📋 Fetching upcoming appointments for doctor: {}", doctorId);

        List<Appointment> appointments = appointmentRepository
                .findUpcomingAppointmentsForDoctor(doctorId, LocalDateTime.now());

        log.info("✅ Found {} upcoming appointments", appointments.size());

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * DOCTOR: Get pending appointments (need response from doctor)
     */
    public List<AppointmentResponse> getPendingAppointments(String doctorId) {
        log.info("📋 Fetching pending appointments for doctor: {}", doctorId);

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndStatusOrderByAppointmentDateTimeAsc(doctorId, "PENDING");

        log.info("✅ Found {} pending appointments", appointments.size());

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * DOCTOR: Accept an appointment
     */
    @Transactional
    public AppointmentResponse acceptAppointment(String appointmentId, String doctorId) {
        log.info("========================================");
        log.info("✅ ACCEPTING APPOINTMENT");
        log.info("========================================");
        log.info("Appointment ID: {}", appointmentId);
        log.info("Doctor ID: {}", doctorId);
        log.info("========================================");

        // Vérifier que le rendez-vous appartient au docteur
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("❌ Appointment not found: {}", appointmentId);
                    return new RuntimeException("Appointment not found");
                });

        if (!appointment.getDoctorId().equals(doctorId)) {
            log.error("❌ Unauthorized: Doctor {} tried to accept appointment {} (belongs to {})",
                    doctorId, appointmentId, appointment.getDoctorId());
            throw new RuntimeException("Unauthorized: This appointment does not belong to you");
        }

        if (!"PENDING".equals(appointment.getStatus())) {
            log.error("❌ Appointment is not pending. Current status: {}", appointment.getStatus());
            throw new RuntimeException("Appointment is not pending");
        }

        appointment.setStatus("SCHEDULED");
        appointment.setDoctorResponse("ACCEPTED");
        appointment.setRespondedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        log.info("✅ Appointment accepted successfully");

        return mapToResponse(saved);
    }

    /**
     * DOCTOR: Reject an appointment
     */
    @Transactional
    public AppointmentResponse rejectAppointment(
            String appointmentId,
            String doctorId,
            String reason,
            String availableHours) {

        log.info("========================================");
        log.info("❌ REJECTING APPOINTMENT");
        log.info("========================================");
        log.info("Appointment ID: {}", appointmentId);
        log.info("Doctor ID: {}", doctorId);
        log.info("Reason: {}", reason);
        log.info("========================================");

        // Vérifier que le rendez-vous appartient au docteur
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("❌ Appointment not found: {}", appointmentId);
                    return new RuntimeException("Appointment not found");
                });

        if (!appointment.getDoctorId().equals(doctorId)) {
            log.error("❌ Unauthorized: Doctor {} tried to reject appointment {} (belongs to {})",
                    doctorId, appointmentId, appointment.getDoctorId());
            throw new RuntimeException("Unauthorized: This appointment does not belong to you");
        }

        if (!"PENDING".equals(appointment.getStatus())) {
            log.error("❌ Appointment is not pending. Current status: {}", appointment.getStatus());
            throw new RuntimeException("Appointment is not pending");
        }

        appointment.setStatus("REJECTED");
        appointment.setDoctorResponse("REJECTED");
        appointment.setDoctorResponseReason(reason);
        appointment.setAvailableHoursSuggestion(availableHours);
        appointment.setRespondedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        log.info("✅ Appointment rejected successfully");

        return mapToResponse(saved);
    }

    /**
     * DOCTOR: Get patients list
     */
    public List<PatientInfoResponse> getDoctorPatients(String doctorId) {
        log.info("👥 Fetching patients for doctor: {}", doctorId);

        List<Appointment> allAppointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId);

        // Group by patient
        Map<String, List<Appointment>> byPatient = allAppointments.stream()
                .collect(Collectors.groupingBy(Appointment::getPatientId));

        log.info("✅ Found {} unique patients", byPatient.size());

        return byPatient.entrySet().stream()
                .map(entry -> {
                    String patientId = entry.getKey();
                    List<Appointment> appointments = entry.getValue();

                    Appointment latest = appointments.get(0);

                    long completed = appointments.stream()
                            .filter(a -> "COMPLETED".equals(a.getStatus()))
                            .count();

                    long cancelled = appointments.stream()
                            .filter(a -> "CANCELLED".equals(a.getStatus()))
                            .count();

                    LocalDateTime next = appointments.stream()
                            .filter(Appointment::isUpcoming)
                            .map(Appointment::getAppointmentDateTime)
                            .min(LocalDateTime::compareTo)
                            .orElse(null);

                    LocalDateTime first = appointments.stream()
                            .map(Appointment::getCreatedAt)
                            .min(LocalDateTime::compareTo)
                            .orElse(null);

                    return PatientInfoResponse.builder()
                            .patientId(patientId)
                            .patientName(latest.getPatientName())
                            .patientEmail(latest.getPatientEmail())
                            .patientPhone(latest.getPatientPhone())
                            .totalAppointments(appointments.size())
                            .completedAppointments((int) completed)
                            .cancelledAppointments((int) cancelled)
                            .lastAppointmentDate(latest.getAppointmentDateTime())
                            .nextAppointmentDate(next)
                            .firstVisitDate(first)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * DOCTOR: Get dashboard statistics
     */
    public DoctorStatsResponse getDoctorStats(String doctorId) {
        log.info("📊 Generating stats for doctor: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    log.error("❌ Doctor not found: {}", doctorId);
                    return new RuntimeException("Doctor not found");
                });

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime endOfDay = now.with(LocalTime.MAX);

        // Today's appointments
        List<Appointment> todayAppts = appointmentRepository
                .findTodayAppointmentsForDoctor(doctorId, startOfDay, endOfDay);

        int todayTotal = todayAppts.size();
        int todayCompleted = (int) todayAppts.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
        int todayPending = (int) todayAppts.stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .count();

        // Overall stats
        long totalAppts = appointmentRepository.countByDoctorId(doctorId);
        long upcoming = appointmentRepository.countByDoctorIdAndStatus(doctorId, "SCHEDULED");
        long completed = appointmentRepository.countByDoctorIdAndStatus(doctorId, "COMPLETED");
        long cancelled = appointmentRepository.countByDoctorIdAndStatus(doctorId, "CANCELLED");
        long pending = appointmentRepository.countByDoctorIdAndStatus(doctorId, "PENDING");

        // This week
        LocalDateTime startOfWeek = now.with(LocalDate.now().minusDays(now.getDayOfWeek().getValue() - 1))
                .with(LocalTime.MIN);
        List<Appointment> weekAppts = appointmentRepository
                .findAppointmentsBetweenDates(doctorId, startOfWeek, now);

        // This month
        LocalDateTime startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
        List<Appointment> monthAppts = appointmentRepository
                .findAppointmentsBetweenDates(doctorId, startOfMonth, now);

        // Count unique patients
        List<Appointment> distinctPatients = appointmentRepository.findDistinctPatientsByDoctorId(doctorId);
        int uniquePatients = distinctPatients.stream()
                .map(Appointment::getPatientId)
                .collect(Collectors.toSet())
                .size();

        log.info("✅ Stats generated: {} total appointments, {} patients", totalAppts, uniquePatients);

        return DoctorStatsResponse.builder()
                .doctorId(doctorId)
                .doctorName(doctor.getFullName())
                .specialization(doctor.getSpecialization())
                .todayAppointments(todayTotal)
                .todayCompleted(todayCompleted)
                .todayPending(todayPending)
                .pendingAppointments((int) pending)
                .totalAppointments((int) totalAppts)
                .totalPatients(uniquePatients)
                .upcomingAppointments((int) upcoming)
                .completedAppointments((int) completed)
                .cancelledAppointments((int) cancelled)
                .thisWeekAppointments(weekAppts.size())
                .thisMonthAppointments(monthAppts.size())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * DOCTOR: Complete appointment
     */
    @Transactional
    public AppointmentResponse completeAppointment(
            String appointmentId,
            String doctorId,
            String diagnosis,
            String prescription,
            String notes) {

        log.info("========================================");
        log.info("✅ COMPLETING APPOINTMENT");
        log.info("========================================");
        log.info("Appointment ID: {}", appointmentId);
        log.info("Doctor ID: {}", doctorId);
        log.info("========================================");

        // Vérifier que le rendez-vous appartient au docteur
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("❌ Appointment not found: {}", appointmentId);
                    return new RuntimeException("Appointment not found");
                });

        if (!appointment.getDoctorId().equals(doctorId)) {
            log.error("❌ Unauthorized: Doctor {} tried to complete appointment {} (belongs to {})",
                    doctorId, appointmentId, appointment.getDoctorId());
            throw new RuntimeException("Unauthorized: This appointment does not belong to you");
        }

        appointment.setStatus("COMPLETED");
        appointment.setDiagnosis(diagnosis);
        appointment.setPrescription(prescription);
        appointment.setDoctorNotes(notes);
        appointment.setCompletedAt(LocalDateTime.now());

        Appointment updated = appointmentRepository.save(appointment);

        log.info("✅ Appointment completed successfully");

        return mapToResponse(updated);
    }

    /**
     * Cancel appointment with security check
     */
    @Transactional
    public void cancelAppointment(
            String appointmentId,
            String doctorId,
            String cancelledBy,
            String reason) {

        log.info("========================================");
        log.info("❌ CANCELLING APPOINTMENT");
        log.info("========================================");
        log.info("Appointment ID: {}", appointmentId);
        log.info("Doctor ID: {}", doctorId);
        log.info("Cancelled by: {}", cancelledBy);
        log.info("========================================");

        // Vérifier que le rendez-vous appartient au docteur
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("❌ Appointment not found: {}", appointmentId);
                    return new RuntimeException("Appointment not found");
                });

        if (!appointment.getDoctorId().equals(doctorId)) {
            log.error("❌ Unauthorized: Doctor {} tried to cancel appointment {} (belongs to {})",
                    doctorId, appointmentId, appointment.getDoctorId());
            throw new RuntimeException("Unauthorized: This appointment does not belong to you");
        }

        if (!appointment.canBeCancelled()) {
            log.error("❌ Appointment cannot be cancelled. Current status: {}", appointment.getStatus());
            throw new RuntimeException("Appointment cannot be cancelled");
        }

        appointment.setStatus("CANCELLED");
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancellationReason(reason);
        appointment.setCancelledAt(LocalDateTime.now());

        appointmentRepository.save(appointment);

        log.info("✅ Appointment cancelled successfully");
    }

    /**
     * PATIENT: Get patient appointments
     */
    public List<AppointmentResponse> getPatientAppointments(String patientId) {
        log.info("📅 Fetching appointments for patient: {}", patientId);

        List<Appointment> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateTimeDesc(patientId);

        log.info("✅ Found {} appointments", appointments.size());

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper: Map to response
     */
    private AppointmentResponse mapToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .patientEmail(appointment.getPatientEmail())
                .patientName(appointment.getPatientName())
                .patientPhone(appointment.getPatientPhone())
                .doctorId(appointment.getDoctorId())
                .doctorEmail(appointment.getDoctorEmail())
                .doctorName(appointment.getDoctorName())
                .specialization(appointment.getSpecialization())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .appointmentType(appointment.getAppointmentType())
                .reason(appointment.getReason())
                .notes(appointment.getNotes())
                .status(appointment.getStatus())
                .doctorResponse(appointment.getDoctorResponse())
                .doctorResponseReason(appointment.getDoctorResponseReason())
                .availableHoursSuggestion(appointment.getAvailableHoursSuggestion())
                .respondedAt(appointment.getRespondedAt())
                .diagnosis(appointment.getDiagnosis())
                .prescription(appointment.getPrescription())
                .doctorNotes(appointment.getDoctorNotes())
                .completedAt(appointment.getCompletedAt())
                .cancelledBy(appointment.getCancelledBy())
                .cancellationReason(appointment.getCancellationReason())
                .cancelledAt(appointment.getCancelledAt())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}