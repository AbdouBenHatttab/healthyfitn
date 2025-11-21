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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    /**
     * PATIENT: Create new appointment
     */
    public AppointmentResponse createAppointment(AppointmentRequest request, String patientId, String patientEmail, String patientName) {
        log.info("Creating appointment for patient: {} with doctor: {}", patientEmail, request.getDoctorId());

        // Verify doctor exists and is activated
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!doctor.getIsActivated()) {
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
                .status("PENDING") // ✅ Default to PENDING
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("✅ Appointment created with PENDING status: {}", saved.getId());

        return mapToResponse(saved);
    }

    /**
     * DOCTOR: Get all appointments for a doctor
     */
    public List<AppointmentResponse> getDoctorAppointments(String doctorId) {
        log.info("Fetching appointments for doctor: {}", doctorId);

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId);

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * DOCTOR: Get upcoming appointments
     */
    public List<AppointmentResponse> getUpcomingAppointments(String doctorId) {
        log.info("Fetching upcoming appointments for doctor: {}", doctorId);

        List<Appointment> appointments = appointmentRepository
                .findUpcomingAppointmentsForDoctor(doctorId, LocalDateTime.now());

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ NEW: Get pending appointments (need response from doctor)
     */
    public List<AppointmentResponse> getPendingAppointments(String doctorId) {
        log.info("📋 Fetching pending appointments for doctor: {}", doctorId);

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndStatusOrderByAppointmentDateTimeAsc(doctorId, "PENDING");

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ NEW: Doctor accepts an appointment
     */
    @Transactional
    public AppointmentResponse acceptAppointment(String appointmentId, String doctorId) {
        log.info("✅ Doctor {} accepting appointment {}", doctorId, appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("Unauthorized: Not your appointment");
        }

        if (!"PENDING".equals(appointment.getStatus())) {
            throw new RuntimeException("Appointment is not pending");
        }

        appointment.setStatus("SCHEDULED");
        appointment.setDoctorResponse("ACCEPTED");
        appointment.setRespondedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        log.info("✅ Appointment {} accepted by doctor {}", appointmentId, doctorId);

        return mapToResponse(saved);
    }

    /**
     * ✅ NEW: Doctor rejects an appointment
     */
    @Transactional
    public AppointmentResponse rejectAppointment(
            String appointmentId,
            String doctorId,
            String reason,
            String availableHours) {

        log.info("❌ Doctor {} rejecting appointment {}", doctorId, appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("Unauthorized: Not your appointment");
        }

        if (!"PENDING".equals(appointment.getStatus())) {
            throw new RuntimeException("Appointment is not pending");
        }

        appointment.setStatus("REJECTED");
        appointment.setDoctorResponse("REJECTED");
        appointment.setDoctorResponseReason(reason);
        appointment.setAvailableHoursSuggestion(availableHours);
        appointment.setRespondedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        log.info("❌ Appointment {} rejected. Reason: {}", appointmentId, reason);

        return mapToResponse(saved);
    }

    /**
     * DOCTOR: Get patients list
     */
    public List<PatientInfoResponse> getDoctorPatients(String doctorId) {
        log.info("Fetching patients for doctor: {}", doctorId);

        List<Appointment> allAppointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId);

        // Group by patient
        Map<String, List<Appointment>> byPatient = allAppointments.stream()
                .collect(Collectors.groupingBy(Appointment::getPatientId));

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
        log.info("Generating stats for doctor: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

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
        long pending = appointmentRepository.countByDoctorIdAndStatus(doctorId, "PENDING"); // ✅ NEW

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

        return DoctorStatsResponse.builder()
                .doctorId(doctorId)
                .doctorName(doctor.getFullName())
                .specialization(doctor.getSpecialization())
                .todayAppointments(todayTotal)
                .todayCompleted(todayCompleted)
                .todayPending(todayPending)
                .pendingAppointments((int) pending) // ✅ NEW
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
    public AppointmentResponse completeAppointment(String appointmentId, String diagnosis, String prescription, String notes) {
        log.info("Completing appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus("COMPLETED");
        appointment.setDiagnosis(diagnosis);
        appointment.setPrescription(prescription);
        appointment.setDoctorNotes(notes);
        appointment.setCompletedAt(LocalDateTime.now());

        Appointment updated = appointmentRepository.save(appointment);

        return mapToResponse(updated);
    }

    /**
     * Cancel appointment
     */
    public void cancelAppointment(String appointmentId, String cancelledBy, String reason) {
        log.info("Cancelling appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.canBeCancelled()) {
            throw new RuntimeException("Appointment cannot be cancelled");
        }

        appointment.setStatus("CANCELLED");
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancellationReason(reason);
        appointment.setCancelledAt(LocalDateTime.now());

        appointmentRepository.save(appointment);
    }

    /**
     * ✅ PATIENT: Get patient appointments
     */
    public List<AppointmentResponse> getPatientAppointments(String patientId) {
        log.info("📅 Fetching appointments for patient: {}", patientId);

        List<Appointment> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateTimeDesc(patientId);

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
                .diagnosis(appointment.getDiagnosis())
                .prescription(appointment.getPrescription())
                .doctorNotes(appointment.getDoctorNotes())
                .completedAt(appointment.getCompletedAt())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}