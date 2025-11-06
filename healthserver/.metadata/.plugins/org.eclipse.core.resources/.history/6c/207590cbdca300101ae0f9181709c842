package com.healthapp.doctor.repository;

import com.healthapp.doctor.entity.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends MongoRepository<Doctor, String> {
    
    Optional<Doctor> findByUserId(String userId);
    
    Optional<Doctor> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByMedicalLicenseNumber(String medicalLicenseNumber);
    
    Optional<Doctor> findByMedicalLicenseNumber(String medicalLicenseNumber);
    
    List<Doctor> findByActivationStatus(String activationStatus);
    
    List<Doctor> findByIsActivatedTrue();
    
    List<Doctor> findBySpecialization(String specialization);
    
    List<Doctor> findByHospitalAffiliation(String hospitalAffiliation);
    
    long countByActivationStatus(String activationStatus);
}
