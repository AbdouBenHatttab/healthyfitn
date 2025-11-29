package com.example.healthsync.controller;

import com.example.healthsync.model.BiometricData;
import com.example.healthsync.model.HealthData;
import com.example.healthsync.model.HealthData.*;
import com.example.healthsync.service.BiometricDataService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/fetch")
@CrossOrigin(origins = "*") // TODO: Restreindre en production
@RequiredArgsConstructor
@Slf4j
public class HealthDataController {

    private final BiometricDataService biometricDataService;

    @GetMapping
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("✅ Serveur Spring Boot accessible depuis le téléphone!");
    }

    @PostMapping
    public ResponseEntity<String> receiveHealthData(@RequestBody HealthData healthData) {
        try {
            System.out.println("\n╔═════════════════════════════════════════════════════════════╗");
            System.out.println("║  🔔 DONNÉES HEALTH CONNECT REÇUES DEPUIS ANDROID          ║");
            System.out.println("╚═════════════════════════════════════════════════════════════╝\n");

            // ✅ SAUVEGARDE DANS MONGODB
            List<BiometricData> savedData = biometricDataService.saveBiometricData(healthData);
            String userEmail = healthData.getEmail();

            System.out.println("💾 DONNÉES SAUVEGARDÉES DANS MONGODB");
            System.out.println("   • Email utilisateur: " + userEmail);
            System.out.println("   • Nombre de jours: " + savedData.size());
            System.out.println("   • IDs MongoDB: ");
            savedData.forEach(data ->
                    System.out.println("      - " + data.getDate() + " → " + data.getId())
            );
            System.out.println();

            // ✅ Validation des données
            List<DailyData> dailyDataList = healthData.getDailyData();
            if (dailyDataList == null || dailyDataList.isEmpty()) {
                return ResponseEntity.badRequest().body("❌ Format de données invalide: dailyData vide");
            }

            int totalDataPoints = 0;

            // ✅ Parcours direct des données avec getters
            for (DailyData day : dailyDataList) {
                String date = day.getDate();

                System.out.println("\n╭─────────────────────────────────────────────────────────────╮");
                System.out.println("│  📅 DATE: " + date + "                                   │");
                System.out.println("╰─────────────────────────────────────────────────────────────╯\n");

                // 👣 STEPS
                Integer totalSteps = day.getTotalSteps();
                if (totalSteps != null && totalSteps > 0) {
                    System.out.println("👣 STEPS: " + totalSteps + " pas");
                    totalDataPoints++;
                }

                // ❤️ HEART RATE
                Integer avgHR = day.getAvgHeartRate();
                if (avgHR != null && avgHR > 0) {
                    Integer minHR = day.getMinHeartRate();
                    Integer maxHR = day.getMaxHeartRate();

                    System.out.println("❤️  HEART RATE:");
                    System.out.println("   • Moyenne: " + avgHR + " bpm");
                    if (minHR != null && maxHR != null) {
                        System.out.println("   • Min: " + minHR + " bpm | Max: " + maxHR + " bpm");
                    }
                    totalDataPoints++;
                }

                // 📏 DISTANCE
                String distKm = day.getTotalDistanceKm();
                if (distKm != null && !distKm.equals("0.00")) {
                    System.out.println("📏 DISTANCE: " + distKm + " km");
                    totalDataPoints++;
                }

                // 💤 SLEEP
                String sleepHours = day.getTotalSleepHours();
                List<SleepRecord> sleepRecords = day.getSleep();
                if (sleepHours != null && sleepRecords != null && !sleepRecords.isEmpty()) {
                    System.out.println("💤 SOMMEIL: " + sleepHours + " heures");

                    for (SleepRecord sleep : sleepRecords) {
                        System.out.println("   • " + sleep.getTitle() + ": " +
                                sleep.getStartTime() + " → " + sleep.getEndTime() +
                                " (" + sleep.getDurationMinutes() + " min)");
                    }
                    totalDataPoints++;
                }

                // 🏋️ EXERCISE
                List<ExerciseRecord> exercises = day.getExercise();
                if (exercises != null && !exercises.isEmpty()) {
                    System.out.println("\n🏋️  EXERCICES (" + exercises.size() + " sessions):");

                    for (int i = 0; i < exercises.size(); i++) {
                        ExerciseRecord ex = exercises.get(i);

                        System.out.println("\n   ┌─ Session " + (i + 1) + " ─────────────────────────────");
                        System.out.println("   │ 🏃 Type: " + ex.getExerciseTypeName());
                        System.out.println("   │ ⏱️  Durée: " + ex.getDurationMinutes() + " minutes");
                        System.out.println("   │ 🕐 Début: " + ex.getStartTime());

                        if (ex.getDistanceKm() != null && !ex.getDistanceKm().equals("0.00")) {
                            System.out.println("   │ 📏 Distance: " + ex.getDistanceKm() + " km");
                        }
                        if (ex.getSteps() != null && ex.getSteps() > 0) {
                            System.out.println("   │ 👣 Pas: " + ex.getSteps());
                        }
                        if (ex.getActiveCalories() != null && ex.getActiveCalories() > 0) {
                            System.out.println("   │ 🔥 Calories: " + ex.getActiveCalories() + " kcal");
                        }
                        if (ex.getAvgHeartRate() != null && ex.getAvgHeartRate() > 0) {
                            System.out.println("   │ ❤️  BPM: " + ex.getAvgHeartRate() + " bpm");
                        }

                        // Métriques avancées
                        if (ex.getAvgSpeedKmh() != null) {
                            System.out.println("   │ 🚀 Vitesse moy: " + ex.getAvgSpeedKmh() + " km/h");
                        }
                        if (ex.getAvgCadence() != null && ex.getAvgCadence() > 0) {
                            System.out.println("   │ 🎯 Cadence: " + ex.getAvgCadence() + " rpm");
                        }
                        if (ex.getAvgPowerWatts() != null && ex.getAvgPowerWatts() > 0) {
                            System.out.println("   │ ⚡ Puissance: " + ex.getAvgPowerWatts() + " W");
                        }

                        System.out.println("   └────────────────────────────────────────────");
                    }
                    totalDataPoints += exercises.size();
                }

                // 💧 HYDRATION
                String hydrationLiters = day.getTotalHydrationLiters();
                List<HydrationRecord> hydrationRecords = day.getHydration();
                if (hydrationLiters != null && hydrationRecords != null && !hydrationRecords.isEmpty()) {
                    System.out.println("\n💧 HYDRATATION: " + hydrationLiters + " L (" +
                            hydrationRecords.size() + " prises)");
                    totalDataPoints++;
                }

                // 😰 STRESS
                String stressLevel = day.getStressLevel();
                Integer stressScore = day.getStressScore();
                if (stressLevel != null && stressScore != null) {
                    System.out.println("😰 STRESS: " + stressLevel + " (score: " + stressScore + ")");
                    totalDataPoints++;
                }

                // 🫁 OXYGEN SATURATION
                List<OxygenSaturationRecord> oxygenRecords = day.getOxygenSaturation();
                if (oxygenRecords != null && !oxygenRecords.isEmpty()) {
                    System.out.println("\n🫁 SATURATION O2: " + oxygenRecords.size() + " mesures");
                    totalDataPoints += oxygenRecords.size();
                }

                // 🌡️ BODY TEMPERATURE
                List<BodyTemperatureRecord> tempRecords = day.getBodyTemperature();
                if (tempRecords != null && !tempRecords.isEmpty()) {
                    System.out.println("🌡️  TEMPÉRATURE: " + tempRecords.size() + " mesures");
                    totalDataPoints += tempRecords.size();
                }

                // 💉 BLOOD PRESSURE
                List<BloodPressureRecord> bpRecords = day.getBloodPressure();
                if (bpRecords != null && !bpRecords.isEmpty()) {
                    System.out.println("💉 PRESSION: " + bpRecords.size() + " mesures");
                    totalDataPoints += bpRecords.size();
                }

                // ⚖️ WEIGHT
                List<WeightRecord> weightRecords = day.getWeight();
                if (weightRecords != null && !weightRecords.isEmpty()) {
                    System.out.println("⚖️  POIDS: " + weightRecords.size() + " mesures");
                    totalDataPoints += weightRecords.size();
                }

                // 📏 HEIGHT
                List<HeightRecord> heightRecords = day.getHeight();
                if (heightRecords != null && !heightRecords.isEmpty()) {
                    System.out.println("📏 TAILLE: " + heightRecords.size() + " mesures");
                    totalDataPoints += heightRecords.size();
                }

                System.out.println("\n" + "─".repeat(65));
            }

            System.out.println("\n╔═════════════════════════════════════════════════════════════╗");
            System.out.println("║  ✅ SUCCÈS - " + totalDataPoints + " points sauvegardés dans MongoDB   ║");
            System.out.println("╚═════════════════════════════════════════════════════════════╝\n");

            log.info("Health data processed successfully - User: {}, Records: {}, Data points: {}",
                    userEmail, savedData.size(), totalDataPoints);

            return ResponseEntity.ok(String.format(
                    "✅ %d données reçues et sauvegardées!\n👤 Email: %s\n💾 %d enregistrements MongoDB",
                    totalDataPoints, userEmail, savedData.size()
            ));

        } catch (Exception e) {
            log.error("Error processing health data", e);
            System.err.println("\n❌ ERREUR: " + e.getMessage() + "\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Erreur: " + e.getMessage());
        }
    }

    // ✅ Récupérer les données d'un utilisateur
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserData(@PathVariable String userId) {
        try {
            log.info("Fetching data for user: {}", userId);
            List<BiometricData> data = biometricDataService.getUserData(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error fetching user data: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erreur: " + e.getMessage());
        }
    }

    // ✅ Stats d'un utilisateur
    @GetMapping("/stats/{userId}")
    public ResponseEntity<String> getUserStats(@PathVariable String userId) {
        try {
            log.info("Fetching stats for user: {}", userId);
            String stats = biometricDataService.getUserStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching user stats: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erreur: " + e.getMessage());
        }
    }

    // ✅ Récupérer les données biométriques pour la date du jour
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<?> getTodayBiometricData(@PathVariable String userId) {
        try {
            log.info("Fetching today's data for user: {}", userId);
            BiometricData data = biometricDataService.getTodayData(userId);

            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Aucune donnée biométrique trouvée pour aujourd'hui");
            }

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("Error fetching today's biometric data: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erreur: " + e.getMessage());
        }
    }

}