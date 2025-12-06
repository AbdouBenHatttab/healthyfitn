package com.healthapp.doctor.repository;

import com.healthapp.doctor.entity.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    // Find by doctor
    List<Appointment> findByDoctorIdOrderByAppointmentDateTimeDesc(String doctorId);


    // Find by patient
    List<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(String patientId);


    // Find upcoming appointments for doctor
    @Query("{ 'doctorId': ?0, 'appointmentDateTime': { $gte: ?1 }, 'status': 'SCHEDULED' }")
    List<Appointment> findUpcomingAppointmentsForDoctor(String doctorId, LocalDateTime now);

    // Find appointments for today for a doctor
    @Query("{ 'doctorId': ?0, 'appointmentDateTime': { $gte: ?1, $lt: ?2 } }")
    List<Appointment> findTodayAppointmentsForDoctor(String doctorId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    // Count appointments by status for doctor
    List<Appointment> findByDoctorIdAndStatusOrderByAppointmentDateTimeAsc(String doctorId, String status);
    long countByDoctorIdAndStatus(String doctorId, String status);

    // Count total appointments for doctor
    long countByDoctorId(String doctorId);

    // Find appointments between dates
    @Query("{ 'doctorId': ?0, 'appointmentDateTime': { $gte: ?1, $lte: ?2 } }")
    List<Appointment> findAppointmentsBetweenDates(String doctorId, LocalDateTime start, LocalDateTime end);

    // Find unique patient IDs for a doctor
    @Query(value = "{ 'doctorId': ?0 }", fields = "{ 'patientId': 1 }")
    List<Appointment> findDistinctPatientsByDoctorId(String doctorId);

    // ✅ NOUVELLES MÉTHODES POUR LA SUPPRESSION

    /**
     * Supprimer tous les rendez-vous d'un docteur par son ID
     */
    void deleteByDoctorId(String doctorId);
    /**
     * Trouver tous les rendez-vous d'un patient par son ID
     */
    List<Appointment> findByPatientId(String patientId);

    /**
     * Trouver tous les rendez-vous d'un patient par son email
     */
    List<Appointment> findByPatientEmail(String patientEmail);

    /**
     * Supprimer tous les rendez-vous d'un patient par son ID
     */
    void deleteByPatientId(String patientId);

    /**
     * Supprimer tous les rendez-vous d'un patient par son email
     */
    void deleteByPatientEmail(String patientEmail);

    /**
     * Compter les rendez-vous d'un patient
     */
    long countByPatientId(String patientId);


}