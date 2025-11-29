package com.healthapp.doctor.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.healthapp.doctor.entity.CallSession;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallSessionRepository extends MongoRepository<CallSession, String> {
    List<CallSession> findByAppointmentId(String appointmentId);
    List<CallSession> findByDoctorIdOrderByCreatedAtDesc(String doctorId);
    List<CallSession> findByPatientIdOrderByCreatedAtDesc(String patientId);
    Optional<CallSession> findByAppointmentIdAndStatus(String appointmentId, List<String> status);

}
