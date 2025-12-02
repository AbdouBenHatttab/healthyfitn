package com.example.healthsync.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "biometric_data")
public class BiometricData {

    @Id
    private String id;  // MongoDB génère automatiquemen    t

    private String email;  // Clé étrangère vers User (temporairement aléatoire)

    private LocalDateTime receivedAt;  // Date de réception

    private String date;  // Date des données (format: "2025-10-30")

    // ✅ TOUTES LES DONNÉES AGRÉGÉES
    private Integer totalSteps;
    private Integer avgHeartRate;
    private Integer minHeartRate;
    private Integer maxHeartRate;
    private String totalDistanceKm;
    private String totalSleepHours;
    private String totalHydrationLiters;
    private String stressLevel;
    private Integer stressScore;

    // ✅ TOUTES LES DONNÉES DÉTAILLÉES (exactement comme HealthData)
    private List<StepRecord> steps;
    private List<HeartRateRecord> heartRate;
    private List<DistanceRecord> distance;
    private List<SleepRecord> sleep;
    private List<ExerciseRecord> exercise;
    private List<OxygenSaturationRecord> oxygenSaturation;
    private List<BodyTemperatureRecord> bodyTemperature;
    private List<BloodPressureRecord> bloodPressure;
    private List<WeightRecord> weight;
    private List<HeightRecord> height;
    private List<HydrationRecord> hydration;

    // ✅ Classes internes identiques à HealthData

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepRecord {
        private Long count;
        private String startTime;
        private String endTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeartRateRecord {
        private List<Long> samples;
        private String startTime;
        private String endTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistanceRecord {
        private Double distanceMeters;
        private String startTime;
        private String endTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepRecord {
        private String title;
        private String startTime;
        private String endTime;
        private Long durationMinutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExerciseRecord {
        private String title;
        private Integer exerciseType;
        private String exerciseTypeName;
        private String startTime;
        private String endTime;
        private Long durationMinutes;

        // Métriques de performance
        private Long steps;
        private Double distanceMeters;
        private String distanceKm;

        // Calories
        private Integer activeCalories;
        private Integer totalCalories;

        // Fréquence cardiaque
        private Integer avgHeartRate;
        private Integer minHeartRate;
        private Integer maxHeartRate;

        // Cadence
        private Integer avgCadence;
        private Integer minCadence;
        private Integer maxCadence;

        // Vitesse
        private String avgSpeedKmh;
        private String maxSpeedKmh;
        private String minSpeedKmh;

        // Longueur de foulée
        private String avgStrideLengthMeters;
        private String minStrideLengthMeters;
        private String maxStrideLengthMeters;

        // Puissance
        private Integer avgPowerWatts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OxygenSaturationRecord {
        private Double percentage;
        private String time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BodyTemperatureRecord {
        private Double temperature;
        private String time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BloodPressureRecord {
        private Double systolic;
        private Double diastolic;
        private String time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightRecord {
        private Double weight;
        private String time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeightRecord {
        private Double height;
        private String time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HydrationRecord {
        private Double volumeMl;
        private String time;
    }
}