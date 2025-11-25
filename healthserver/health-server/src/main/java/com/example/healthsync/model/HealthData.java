package com.example.healthsync.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class HealthData {

    @JsonProperty("dailyData")
    private List<DailyData> dailyData;
    @JsonProperty("email")
    private String email;

    @Data
    public static class DailyData {
        private String date;
        private List<StepRecord> steps;
        private Integer totalSteps;
        private List<HeartRateRecord> heartRate;
        private Integer minHeartRate;
        private Integer maxHeartRate;
        private Integer avgHeartRate;
        private List<DistanceRecord> distance;
        private String totalDistanceKm;
        private List<SleepRecord> sleep;
        private String totalSleepHours;
        private List<ExerciseRecord> exercise;
        private List<OxygenSaturationRecord> oxygenSaturation;
        private List<BodyTemperatureRecord> bodyTemperature;
        private List<BloodPressureRecord> bloodPressure;
        private List<WeightRecord> weight;
        private List<HeightRecord> height;
        private List<HydrationRecord> hydration;
        private String totalHydrationLiters;
        private String stressLevel;
        private Integer stressScore;
    }

    @Data
    public static class StepRecord {
        private Long count;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class HeartRateRecord {
        private List<Long> samples;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class DistanceRecord {
        private Double distanceMeters;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class SleepRecord {
        private String title;
        private String startTime;
        private String endTime;
        private Long durationMinutes;
    }

    @Data
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

        // Cadence et allure (COMPLET: min/max/avg)
        private Integer avgCadence;
        private Integer minCadence;
        private Integer maxCadence;

        // Vitesse (COMPLET: min/max/avg)
        private String avgSpeedKmh;
        private String maxSpeedKmh;
        private String minSpeedKmh;

        // Longueur de foulée (COMPLET: min/max/avg)
        private String avgStrideLengthMeters;
        private String minStrideLengthMeters;
        private String maxStrideLengthMeters;

        // Puissance (cyclisme)
        private Integer avgPowerWatts;
    }

    @Data
    public static class OxygenSaturationRecord {
        private Double percentage;
        private String time;
    }

    @Data
    public static class BodyTemperatureRecord {
        private Double temperature;
        private String time;
    }

    @Data
    public static class BloodPressureRecord {
        private Double systolic;
        private Double diastolic;
        private String time;
    }

    @Data
    public static class WeightRecord {
        private Double weight;
        private String time;
    }

    @Data
    public static class HeightRecord {
        private Double height;
        private String time;
    }

    @Data
    public static class HydrationRecord {
        private Double volumeMl;
        private String time;
    }
}