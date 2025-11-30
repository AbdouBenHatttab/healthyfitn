package com.example.healthsync.service;

import com.example.healthsync.model.BiometricData;
import com.example.healthsync.model.HealthData;
import com.example.healthsync.repository.BiometricDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricDataService {

    private final BiometricDataRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();



    /**
     * Sauvegarde les données biométriques reçues de l'app Android
     * Convertit HealthData → BiometricData pour MongoDB
     */
    public List<BiometricData> saveBiometricData(HealthData healthData) {
        List<BiometricData> savedRecords = new ArrayList<>();

        if (healthData.getDailyData() == null || healthData.getDailyData().isEmpty()) {
            throw new IllegalArgumentException("Aucune donnée quotidienne à sauvegarder");
        }

        String email = healthData.getEmail();
        log.info("📝 Email utilisateur utilisé comme userId: {}", email);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email utilisateur manquant");
        }

        for (HealthData.DailyData day : healthData.getDailyData()) {
            try {
                BiometricData data = new BiometricData();

                // Métadonnées
                data.setEmail(email);
                data.setReceivedAt(LocalDateTime.now());
                data.setDate(day.getDate());

                // ✅ Données agrégées
                data.setTotalSteps(day.getTotalSteps());
                data.setAvgHeartRate(day.getAvgHeartRate());
                data.setMinHeartRate(day.getMinHeartRate());
                data.setMaxHeartRate(day.getMaxHeartRate());
                data.setTotalDistanceKm(day.getTotalDistanceKm());
                data.setTotalSleepHours(day.getTotalSleepHours());
                data.setTotalHydrationLiters(day.getTotalHydrationLiters());
                data.setStressLevel(day.getStressLevel());
                data.setStressScore(day.getStressScore());

                // ✅ Conversion des listes détaillées
                if (day.getSteps() != null) {
                    data.setSteps(day.getSteps().stream()
                            .map(s -> new BiometricData.StepRecord(s.getCount(), s.getStartTime(), s.getEndTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getHeartRate() != null) {
                    data.setHeartRate(day.getHeartRate().stream()
                            .map(hr -> new BiometricData.HeartRateRecord(hr.getSamples(), hr.getStartTime(), hr.getEndTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getDistance() != null) {
                    data.setDistance(day.getDistance().stream()
                            .map(d -> new BiometricData.DistanceRecord(d.getDistanceMeters(), d.getStartTime(), d.getEndTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getSleep() != null) {
                    data.setSleep(day.getSleep().stream()
                            .map(s -> new BiometricData.SleepRecord(s.getTitle(), s.getStartTime(), s.getEndTime(), s.getDurationMinutes()))
                            .collect(Collectors.toList()));
                }

                if (day.getExercise() != null) {
                    data.setExercise(day.getExercise().stream()
                            .map(e -> new BiometricData.ExerciseRecord(
                                    e.getTitle(), e.getExerciseType(), e.getExerciseTypeName(),
                                    e.getStartTime(), e.getEndTime(), e.getDurationMinutes(),
                                    e.getSteps(), e.getDistanceMeters(), e.getDistanceKm(),
                                    e.getActiveCalories(), e.getTotalCalories(),
                                    e.getAvgHeartRate(), e.getMinHeartRate(), e.getMaxHeartRate(),
                                    e.getAvgCadence(), e.getMinCadence(), e.getMaxCadence(),
                                    e.getAvgSpeedKmh(), e.getMaxSpeedKmh(), e.getMinSpeedKmh(),
                                    e.getAvgStrideLengthMeters(), e.getMinStrideLengthMeters(), e.getMaxStrideLengthMeters(),
                                    e.getAvgPowerWatts()
                            ))
                            .collect(Collectors.toList()));
                }

                if (day.getOxygenSaturation() != null) {
                    data.setOxygenSaturation(day.getOxygenSaturation().stream()
                            .map(o2 -> new BiometricData.OxygenSaturationRecord(o2.getPercentage(), o2.getTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getBodyTemperature() != null) {
                    data.setBodyTemperature(day.getBodyTemperature().stream()
                            .map(temp -> new BiometricData.BodyTemperatureRecord(temp.getTemperature(), temp.getTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getBloodPressure() != null) {
                    data.setBloodPressure(day.getBloodPressure().stream()
                            .map(bp -> new BiometricData.BloodPressureRecord(bp.getSystolic(), bp.getDiastolic(), bp.getTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getWeight() != null) {
                    data.setWeight(day.getWeight().stream()
                            .map(w -> new BiometricData.WeightRecord(w.getWeight(), w.getTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getHeight() != null) {
                    data.setHeight(day.getHeight().stream()
                            .map(h -> new BiometricData.HeightRecord(h.getHeight(), h.getTime()))
                            .collect(Collectors.toList()));
                }

                if (day.getHydration() != null) {
                    data.setHydration(day.getHydration().stream()
                            .map(hyd -> new BiometricData.HydrationRecord(hyd.getVolumeMl(), hyd.getTime()))
                            .collect(Collectors.toList()));
                }

                // ✅ Sauvegarde dans MongoDB
                BiometricData saved = repository.save(data);
                savedRecords.add(saved);

                log.info("✅ Sauvegardé: userId={}, date={}, id={}",
                        email, data.getDate(), saved.getId());

            } catch (Exception e) {
                log.error("❌ Erreur sauvegarde jour {}: {}", day.getDate(), e.getMessage());
                throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage(), e);
            }
        }

        return savedRecords;
    }

    public List<BiometricData> getUserData(String email) {
        return repository.findByEmailOrderByReceivedAtDesc(email);
    }


    public String getUserStats(String email) {
        long total = repository.countByEmail(email);
        return String.format("👤 User %s: %d enregistrements", email, total);
    }

    public BiometricData getTodayData(String userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("Recherche des données biométriques pour {} à la date {}", userId, today);
        return repository.findTopByEmailAndDateOrderByReceivedAtDesc(userId, today);
    }
}