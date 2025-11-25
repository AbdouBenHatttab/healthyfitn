# health_ai_system_complete.py
"""
Système d'IA Santé Complet avec MongoDB
Version: 2.0.0
Features: Analyse, Tendances, Alertes, Objectifs personnalisés
"""

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from matplotlib.font_manager import weight_dict
from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any
from datetime import datetime, timedelta
from pymongo import MongoClient, DESCENDING
from bson import ObjectId
import numpy as np
from sklearn.ensemble import IsolationForest, RandomForestRegressor
from sklearn.preprocessing import StandardScaler
import requests
import json
from config import Config
from eureka_client import eureka_client
import atexit

app = FastAPI(title="Health AI System", version="2.0.0")

# Configuration CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =====================================================
# 🗄️ CONFIGURATION MONGODB
# =====================================================

# Connexion MongoDB
MONGO_URI = "mongodb://localhost:27017/"  # Modifiez selon votre config
mongo_client = MongoClient(MONGO_URI)
db = mongo_client["healthsync_db"]
collection = db["biometric_data"]

# Index pour optimiser les requêtes
try:
    collection.create_index([("userId", 1), ("date", -1)])
    collection.create_index([("userId", 1), ("receivedAt", -1)])
except:
    pass

# =====================================================
# 📊 MODÈLES DE DONNÉES PYDANTIC
# =====================================================

class BiometricData(BaseModel):
    totalSteps: int
    avgHeartRate: int
    minHeartRate: int
    maxHeartRate: int
    totalDistanceKm: float
    totalSleepHours: float
    totalHydrationLiters: float
    stressLevel: str
    stressScore: int
    dailyTotalCalories: Optional[int] = 0
    oxygenSaturation: Optional[List[Dict]] = []
    bodyTemperature: Optional[List[Dict]] = []
    bloodPressure: Optional[List[Dict]] = []
    weight: Optional[List[Dict]] = []
    height: Optional[List[Dict]] = []
    exercise: Optional[List[Dict]] = []

class HealthAnalysisResult(BaseModel):
    healthScore: float
    riskLevel: str
    anomalies: List[str]
    recommendations: List[str]
    insights: Dict[str, Any]
    aiExplanation: str

class GoalPreferences(BaseModel):
    preferred_goals: List[str] = Field(
        default=["activity", "sleep", "hydration"],
        description="Liste des objectifs souhaités: activity, sleep, hydration, stress, cardiovascular"
    )
    timeframe_days: int = Field(default=7, ge=7, le=90, description="Délai pour atteindre les objectifs (7-90 jours)")
    difficulty: str = Field(default="moderate", description="Difficulté: easy, moderate, challenging")

# =====================================================
# 🧠 MOTEUR D'ANALYSE SANTÉ AVANCÉ
# =====================================================

class AdvancedHealthAnalyzer:
    """Analyseur de santé avec IA et normes médicales"""
    
    def __init__(self):
        # Normes médicales OMS/CDC
        self.norms = {
            'steps': {'optimal': 10000, 'minimum': 5000, 'low': 2000},
            'heart_rate': {'min': 60, 'max': 100, 'optimal_min': 60, 'optimal_max': 80},
            'sleep': {'optimal': 8, 'minimum': 7, 'maximum': 9},
            'hydration': {'optimal': 2.5, 'minimum': 1.5},
            'spo2': {'normal_min': 95, 'low': 90},
            'temperature': {'normal_min': 36.1, 'normal_max': 37.2, 'fever': 38.0},
            'blood_pressure': {
                'systolic': {'normal_max': 120, 'elevated': 130, 'high': 140},
                'diastolic': {'normal_max': 80, 'elevated': 85, 'high': 90}
            }
        }
        
        # Modèles ML
        self.anomaly_detector = IsolationForest(contamination=0.1, random_state=42)
        self.trend_predictor = RandomForestRegressor(n_estimators=100, random_state=42)
        self.scaler = StandardScaler()
    
    def calculate_health_score(self, data: BiometricData) -> Dict:
        """Calcule le score de santé global (0-100)"""
        scores = {}
        
        # 1. Activité physique (25 points)
        scores['activity'] = self._score_activity(data.totalSteps, data.totalDistanceKm, data.exercise)
        
        # 2. Cardiovasculaire (25 points)
        scores['cardiovascular'] = self._score_cardiovascular(
            data.avgHeartRate, data.minHeartRate, data.maxHeartRate, data.bloodPressure
        )
        
        # 3. Sommeil (20 points)
        scores['sleep'] = self._score_sleep(data.totalSleepHours)
        
        # 4. Hydratation (10 points)
        scores['hydration'] = self._score_hydration(data.totalHydrationLiters)
        
        # 5. Stress (10 points)
        scores['stress'] = self._score_stress(data.stressScore)
        
        # 6. Signes vitaux (10 points)
        scores['vitals'] = self._score_vital_signs(data.oxygenSaturation, data.bodyTemperature)
        
        total_score = sum(scores.values())
        
        return {
            'total_score': round(total_score, 1),
            'breakdown': scores
        }
    
    def _score_activity(self, steps: int, distance: float, exercises: List) -> float:
        """Score activité (0-25)"""
        score = 0
        
        # Pas (0-15)
        if steps >= self.norms['steps']['optimal']:
            score += 15
        elif steps >= self.norms['steps']['minimum']:
            score += 10 + (steps - self.norms['steps']['minimum']) / 5000 * 5
        elif steps >= self.norms['steps']['low']:
            score += 5 + (steps - self.norms['steps']['low']) / 3000 * 5
        else:
            score += (steps / self.norms['steps']['low']) * 5
        
        # Exercices (0-10)
        if exercises and len(exercises) > 0:
            total_duration = sum(ex.get('durationMinutes', 0) for ex in exercises)
            if total_duration >= 30:
                score += 10
            elif total_duration >= 20:
                score += 7
            elif total_duration >= 10:
                score += 5
            else:
                score += (total_duration / 10) * 5
        
        return min(score, 25)
    
    def _score_cardiovascular(self, avg_hr: int, min_hr: int, max_hr: int, bp_data: List) -> float:
        """Score cardiovasculaire (0-25)"""
        score = 0
        
        # FC (0-15)
        if 60 <= avg_hr <= 80:
            score += 15
        elif 50 <= avg_hr <= 100:
            score += 12
        elif avg_hr < 50:
            score += 10
        elif 100 < avg_hr <= 110:
            score += 8
        else:
            score += 5
        
        # Variabilité (0-5)
        variability = max_hr - min_hr
        if 15 <= variability <= 40:
            score += 5
        elif 10 <= variability <= 50:
            score += 3
        else:
            score += 1
        
        # Tension (0-5)
        if bp_data and len(bp_data) > 0:
            latest_bp = bp_data[-1]
            systolic = latest_bp.get('systolic', 120)
            diastolic = latest_bp.get('diastolic', 80)
            
            if systolic <= 120 and diastolic <= 80:
                score += 5
            elif systolic <= 130 and diastolic <= 85:
                score += 3
            else:
                score += 1
        
        return min(score, 25)
    
    def _score_sleep(self, hours: float) -> float:
        """Score sommeil (0-20)"""
        if 7 <= hours <= 9:
            return 20
        elif 6 <= hours < 7:
            return 15
        elif hours > 9:
            excess = hours - 9
            return max(10, 20 - excess * 3)
        elif 5 <= hours < 6:
            return 10
        else:
            return max(0, hours * 2)
    
    def _score_hydration(self, liters: float) -> float:
        """Score hydratation (0-10)"""
        if liters >= 2.5:
            return 10
        elif liters >= 1.5:
            return 7 + (liters - 1.5) * 3
        else:
            return (liters / 1.5) * 7
    
    def _score_stress(self, stress_score: int) -> float:
        """Score stress (0-10)"""
        return max(0, 10 - (stress_score / 10))
    
    def _score_vital_signs(self, spo2_data: List, temp_data: List) -> float:
        """Score signes vitaux (0-10)"""
        score = 0
        
        # SpO2 (0-5)
        if spo2_data and len(spo2_data) > 0:
            latest_spo2 = spo2_data[-1].get('percentage', 0)
            if latest_spo2 >= 95:
                score += 5
            elif latest_spo2 >= 90:
                score += 3
        
        # Température (0-5)
        if temp_data and len(temp_data) > 0:
            latest_temp = temp_data[-1].get('temperature', 36.5)
            if 36.1 <= latest_temp <= 37.2:
                score += 5
            elif latest_temp >= 38.0:
                score += 0
            else:
                score += 2
        
        return score
    
    def detect_anomalies(self, data: BiometricData) -> List[str]:
        """Détecte les anomalies médicales"""
        anomalies = []
        
        # Fréquence cardiaque
        if data.avgHeartRate < 50:
            anomalies.append("⚠️ Bradycardie détectée (FC < 50 bpm)")
        elif data.avgHeartRate > 110:
            anomalies.append("⚠️ Tachycardie détectée (FC > 110 bpm)")
        
        # Sommeil
        if data.totalSleepHours < 5:
            anomalies.append("⚠️ Privation de sommeil sévère (< 5h)")
        elif data.totalSleepHours > 12:
            anomalies.append("⚠️ Hypersomnie détectée (> 12h)")
        
        # Hydratation
        if data.totalHydrationLiters < 1.0:
            anomalies.append("⚠️ Déshydratation potentielle (< 1L)")
        
        # Stress
        if data.stressScore >= 80:
            anomalies.append("⚠️ Niveau de stress critique (≥ 80/100)")
        
        # SpO2
        if data.oxygenSaturation and len(data.oxygenSaturation) > 0:
            latest_spo2 = data.oxygenSaturation[-1].get('percentage', 100)
            if latest_spo2 < 90:
                anomalies.append("🚨 ALERTE: Hypoxie sévère (SpO2 < 90%)")
            elif latest_spo2 < 95:
                anomalies.append("⚠️ Oxygénation sous-optimale (SpO2 < 95%)")
        
        # Température
        if data.bodyTemperature and len(data.bodyTemperature) > 0:
            latest_temp = data.bodyTemperature[-1].get('temperature', 36.5)
            if latest_temp >= 38.0:
                anomalies.append(f"⚠️ Fièvre détectée ({latest_temp:.1f}°C)")
            elif latest_temp < 36.0:
                anomalies.append(f"⚠️ Hypothermie ({latest_temp:.1f}°C)")
        
        # Tension artérielle
        if data.bloodPressure and len(data.bloodPressure) > 0:
            latest_bp = data.bloodPressure[-1]
            systolic = latest_bp.get('systolic', 120)
            diastolic = latest_bp.get('diastolic', 80)
            
            if systolic >= 180 or diastolic >= 120:
                anomalies.append("🚨 URGENCE: Crise hypertensive (TA ≥ 180/120)")
            elif systolic >= 140 or diastolic >= 90:
                anomalies.append(f"⚠️ Hypertension ({systolic}/{diastolic})")
            elif systolic < 90 or diastolic < 60:
                anomalies.append(f"⚠️ Hypotension ({systolic}/{diastolic})")
        
        # Activité
        if data.totalSteps < 1000:
            anomalies.append("⚠️ Sédentarité excessive (< 1000 pas)")
        
        return anomalies
    
    def generate_recommendations(self, data: BiometricData, score_breakdown: Dict) -> List[str]:
        """Génère des recommandations personnalisées"""
        recommendations = []
        
        # Activité
        if score_breakdown['activity'] < 15:
            if data.totalSteps < 5000:
                recommendations.append("🚶 Augmentez progressivement à 10 000 pas/jour (+500 pas/semaine)")
            if not data.exercise or len(data.exercise) == 0:
                recommendations.append("🏃 Intégrez 150 min d'exercice modéré/semaine (OMS)")
        
        # Cardiovasculaire
        if score_breakdown['cardiovascular'] < 18:
            if data.avgHeartRate > 90:
                recommendations.append("❤️ Exercices cardio pour renforcer le cœur (3x/semaine)")
            if data.bloodPressure and data.bloodPressure[-1].get('systolic', 0) > 130:
                recommendations.append("💉 Consultez un médecin pour tension élevée")
                recommendations.append("🧂 Réduisez le sel (< 5g/jour)")
        
        # Sommeil
        if score_breakdown['sleep'] < 15:
            if data.totalSleepHours < 7:
                recommendations.append(f"💤 Visez 7-9h de sommeil (actuellement: {data.totalSleepHours}h)")
                recommendations.append("🌙 Routine de coucher fixe (22h-23h)")
            elif data.totalSleepHours > 9:
                recommendations.append("⏰ Consultez un médecin si hypersomnie persiste")
        
        # Hydratation
        if score_breakdown['hydration'] < 7:
            deficit = 2.5 - data.totalHydrationLiters
            recommendations.append(f"💧 Augmentez hydratation de {deficit:.1f}L (cible: 2.5L/jour)")
            recommendations.append("💦 Buvez 1 verre d'eau toutes les 2h")
        
        # Stress
        if score_breakdown['stress'] < 5:
            recommendations.append("🧘 Méditation quotidienne (10-15 min)")
            recommendations.append("🚶 Activité physique anti-stress (30 min/jour)")
            recommendations.append("😴 Sommeil réparateur (7-9h)")
        
        # Signes vitaux
        if data.oxygenSaturation and data.oxygenSaturation[-1].get('percentage', 100) < 95:
            recommendations.append("🫁 Consultez un médecin pour SpO2 basse")
        
        if data.bodyTemperature and data.bodyTemperature[-1].get('temperature', 36.5) >= 38.0:
            recommendations.append("🌡️ Restez hydraté et surveillez température")
        
        # Recommandations générales
        if len(recommendations) == 0:
            recommendations.append("✅ Excellentes habitudes de santé!")
            recommendations.append("📊 Continuez le suivi régulier")
        
        return recommendations[:10]  # Max 10 recommandations
    
    def determine_risk_level(self, health_score: float, anomalies: List[str]) -> str:
        """Détermine le niveau de risque"""
        critical_anomalies = [a for a in anomalies if "🚨" in a]
        
        if critical_anomalies or health_score < 40:
            return "Critique"
        elif health_score < 60:
            return "Élevé"
        elif health_score < 75:
            return "Modéré"
        else:
            return "Faible"

# =====================================================
# 🤖 GÉNÉRATEUR D'EXPLICATIONS IA
# =====================================================

class AIExplainer:
    """Génère des explications avec Groq API (gratuit)"""
    
    def __init__(self):
        self.api_key = Config.GROQ_API_KEY
        self.api_url = "https://api.groq.com/openai/v1/chat/completions"
        self.model = "llama-3.1-70b-versatile"
    
    def generate_explanation(self, data: BiometricData, analysis: Dict) -> str:
        """Génère une explication personnalisée"""
        
        # Version sans API (toujours utilisée pour fiabilité)
        return self._generate_fallback(data, analysis)
    
    def _generate_fallback(self, data: BiometricData, analysis: Dict) -> str:
        """Explication basée sur règles"""
        score = analysis['health_score']
        risk = analysis['risk_level']
        
        if score >= 80:
            return f"""Excellente santé globale (score: {score:.1f}/100). Vos métriques cardiovasculaires et d'activité sont optimales. Votre fréquence cardiaque moyenne de {data.avgHeartRate} bpm est dans la norme, et votre sommeil de {data.totalSleepHours}h est satisfaisant. Maintenez ces bonnes habitudes pour préserver votre bien-être à long terme."""
        elif score >= 60:
            weak_point = self._identify_weakness(analysis['breakdown'])
            return f"""Santé globale correcte (score: {score:.1f}/100), avec possibilités d'amélioration. Votre {weak_point} nécessite une attention particulière. Avec {data.totalSteps} pas aujourd'hui et {data.totalSleepHours}h de sommeil, concentrez-vous sur les recommandations prioritaires pour optimiser votre score de santé."""
        else:
            return f"""Score de santé de {score:.1f}/100 indiquant un risque {risk.lower()}. Plusieurs paramètres nécessitent une attention immédiate: activité à {data.totalSteps} pas (cible: 10000), sommeil à {data.totalSleepHours}h, stress à {data.stressScore}/100. Consultez un professionnel de santé pour un suivi personnalisé."""
    
    def _identify_weakness(self, breakdown: Dict) -> str:
        """Identifie le point faible"""
        min_category = min(breakdown, key=breakdown.get)
        names = {
            'activity': 'niveau d\'activité physique',
            'cardiovascular': 'santé cardiovasculaire',
            'sleep': 'qualité du sommeil',
            'hydration': 'niveau d\'hydratation',
            'stress': 'gestion du stress',
            'vitals': 'signes vitaux'
        }
        return names.get(min_category, 'certains paramètres')

# =====================================================
# 🎯 ANALYSEUR DE TENDANCES
# =====================================================

class TrendsAnalyzer:
    """Analyse les tendances de santé sur plusieurs jours"""

    @staticmethod
    def get_user_trends(email: str, days: int = 30) -> Dict:
        """Récupère et analyse les tendances d'un utilisateur sur plusieurs jours"""

        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)

        # ✅ Exclure le jour actuel pour avoir exactement N jours
        end_date = end_date - timedelta(days=1)

        # Convertir en strings
        start_date_str = start_date.strftime("%Y-%m-%d")
        end_date_str = end_date.strftime("%Y-%m-%d")

        print(f"🔍 Recherche de {start_date_str} à {end_date_str} pour {email}")

        records = get_unique_daily_data(email, start_date_str, end_date_str)

        print(f"📊 Nombre d'enregistrements trouvés: {len(records)}")

        if len(records) < 2:
            raise HTTPException(
                status_code=404,
                detail=f"Pas assez de données (minimum 2 jours, trouvés: {len(records)})"
            )

        # 📊 Initialisation des séries temporelles
        trends = {
            'dates': [],
            'steps': [],
            'heart_rate': [],
            'sleep_hours': [],
            'stress_score': [],
            'hydration': [],
            'health_scores': [],
            'weight': []
        }

        analyzer = AdvancedHealthAnalyzer()

        # 🧩 Parcours des données journalières
        for record in records:
            trends['dates'].append(record.get('date'))
            trends['steps'].append(record.get('totalSteps', 0))
            trends['heart_rate'].append(record.get('avgHeartRate', 0))
            trends['sleep_hours'].append(float(record.get('totalSleepHours', '0')))
            trends['stress_score'].append(record.get('stressScore', 0))
            trends['hydration'].append(float(record.get('totalHydrationLiters', '0')))

            # ✅ Correction ici : gestion du champ "weight"
            weight_data = record.get('weight', [])
            if isinstance(weight_data, list) and len(weight_data) > 0:
                latest_weight = weight_data[-1].get('weight', 0)
            elif isinstance(weight_data, (int, float)):
                latest_weight = weight_data
            else:
                latest_weight = 0
            trends['weight'].append(float(latest_weight))

            # Calcul du score santé historique
            try:
                data = BiometricData(**{
                    'totalSteps': record.get('totalSteps', 0),
                    'avgHeartRate': record.get('avgHeartRate', 70),
                    'minHeartRate': record.get('minHeartRate', 60),
                    'maxHeartRate': record.get('maxHeartRate', 90),
                    'totalDistanceKm': float(record.get('totalDistanceKm', '0')),
                    'totalSleepHours': float(record.get('totalSleepHours', '7')),
                    'totalHydrationLiters': float(record.get('totalHydrationLiters', '2')),
                    'stressLevel': record.get('stressLevel', 'Modéré'),
                    'stressScore': record.get('stressScore', 50),
                    'oxygenSaturation': record.get('oxygenSaturation', []),
                    'bodyTemperature': record.get('bodyTemperature', []),
                    'bloodPressure': record.get('bloodPressure', []),
                    'weight': record.get('weight', []),
                    'height': record.get('height', []),
                    'exercise': record.get('exercise', [])
                })
                score_result = analyzer.calculate_health_score(data)
                trends['health_scores'].append(score_result['total_score'])
            except Exception as e:
                print(f"Erreur calcul score: {e}")
                trends['health_scores'].append(0)

        # 📈 Calcul des moyennes mobiles (fenêtre adaptative)
        moving_averages = {}
        window = min(7, max(3, len(trends['steps']) // 2))  # fenêtre entre 3 et 7 jours

        if window > 0:
            for key in ['steps', 'heart_rate', 'sleep_hours', 'stress_score', 'hydration', 'weight']:
                values = np.array(trends[key])
                ma = np.convolve(values, np.ones(window) / window, mode='valid')
                moving_averages[f'{key}_ma{window}'] = ma.tolist()

        # 📊 Statistiques et tendances générales
        statistics = {}
        for key in ['steps', 'heart_rate', 'sleep_hours', 'stress_score', 'hydration', 'weight', 'health_scores']:
            values = np.array(trends[key])
            if len(values) > 0:
                statistics[key] = {
                    'min': float(np.min(values)),
                    'max': float(np.max(values)),
                    'mean': float(np.mean(values)),
                    'std': float(np.std(values)),
                    'trend': TrendsAnalyzer._detect_trend(values)
                }

        return {
            'email': email,
            'period_days': days,
            'data_points': len(records),
            'trends': trends,
            'moving_averages': moving_averages,
            'statistics': statistics
        }

    @staticmethod
    def _detect_trend(values: np.ndarray) -> str:
        """Détecte la tendance : increasing, decreasing, stable"""
        if len(values) < 3:
            return 'insufficient_data'

        x = np.arange(len(values))
        slope = np.polyfit(x, values, 1)[0]

        # Seuil dynamique : plus robuste
        mean_value = np.mean(values)
        threshold = max(0.02 * abs(mean_value), 0.01)  # au moins 0.01 d’écart requis

        if slope > threshold:
            return 'increasing'
        elif slope < -threshold:
            return 'decreasing'
        else:
            return 'stable'
# =====================================================
# 🚨 ANALYSEUR D'ALERTES
# =====================================================
def get_unique_daily_data(email, start_date, end_date):
    """
    Récupère les données uniques par jour pour un utilisateur
    Args:
        email: str - email de l'utilisateur
        start_date: str - date de début au format "YYYY-MM-DD"
        end_date: str - date de fin au format "YYYY-MM-DD"
    """

    print(f"🔍 Query MongoDB: email={email}, date>={start_date}, date<={end_date}")

    pipeline = [
        {
            "$match": {
                "email": email,
                "date": {"$gte": start_date, "$lte": end_date}
            }
        },
        {
            "$group": {
                "_id": "$date",
                "doc": {"$first": "$$ROOT"}
            }
        },
        {
            "$replaceRoot": {"newRoot": "$doc"}
        },
        {
            "$sort": {"date": 1}
        }
    ]

    results = list(collection.aggregate(pipeline))
    print(f"✅ Documents trouvés: {len(results)}")

    if len(results) > 0:
        print(f"📅 Première date: {results[0].get('date')}")
        print(f"📅 Dernière date: {results[-1].get('date')}")

    return results

class RiskAlertsAnalyzer:
    """Génère des alertes de risque intelligentes"""

    @staticmethod
    def generate_alerts(email: str, period_days: int = 7, specific_date: Optional[str] = None) -> Dict:
        """
        Génère des alertes basées sur les données récentes
        """

        # --- 1️⃣ Sélection des données à analyser ---
        if specific_date:
            start_date_str = end_date_str = specific_date
            # éliminer les doublons par date comme pour une période
            records = get_unique_daily_data(email, start_date_str, end_date_str)
            analysis_mode = "specific_date"

        else:
            # Cas 2 : analyse sur une période
            end_date = datetime.now()
            start_date = end_date - timedelta(days=period_days - 1)

            start_date_str = start_date.strftime("%Y-%m-%d")
            end_date_str = end_date.strftime("%Y-%m-%d")

            # 🔥 correction : éliminer les doublons par date
            records = get_unique_daily_data(email, start_date_str, end_date_str)

            analysis_mode = "period_average"

        # Aucun document trouvé ?
        if len(records) == 0:
            raise HTTPException(status_code=404, detail="Aucune donnée trouvée pour cette période")

        latest = records[0]
        alerts = []
        risk_factors = []
        action_priorities = []

        # --- 2️⃣ Calcul des moyennes sur la période ---
        if len(records) > 1 and analysis_mode == "period_average":
            current_steps = int(np.mean([r.get('totalSteps', 0) for r in records]))
            current_sleep = float(np.mean([float(r.get('totalSleepHours', '7')) for r in records]))
            current_hr = int(np.mean([r.get('avgHeartRate', 70) for r in records]))
            current_stress = int(np.mean([r.get('stressScore', 50) for r in records]))
            current_hydration = float(np.mean([float(r.get('totalHydrationLiters', '2')) for r in records]))

            analysis_type = f"Moyennes sur {len(records)} jours"

        else:
            # Si analyse d'un seul jour
            current_steps = latest.get('totalSteps', 0)
            current_sleep = float(latest.get('totalSleepHours', '7'))
            current_hr = latest.get('avgHeartRate', 70)
            current_stress = latest.get('stressScore', 50)
            current_hydration = float(latest.get('totalHydrationLiters', '2'))

            analysis_type = "Données du jour"

        # --- 3️⃣ Détection des alertes ---
        # SOMMEIL
        if current_sleep < 6:
            alerts.append("🚨 CRITIQUE: Sommeil insuffisant (< 6h)")
            risk_factors.append({
                'type': 'sleep_deprivation',
                'severity': 'high',
                'description': f'Sommeil à {current_sleep}h (recommandé: 7-9h)',
                'probability': 90.0,
                'actions': [
                    'Couchez-vous 1h plus tôt',
                    'Évitez écrans avant 22h',
                    'Créez une routine de coucher'
                ]
            })
            action_priorities.append({
                'action': 'Augmenter le sommeil à 7-8h/nuit',
                'category': 'sleep_deprivation',
                'urgency': 'high',
                'impact': 'Réduction de risque de 90%'
            })

        elif current_sleep < 7:
            alerts.append("⚠️ ATTENTION: Sommeil sous-optimal")
            risk_factors.append({
                'type': 'sleep_insufficient',
                'severity': 'medium',
                'description': f'Sommeil à {current_sleep}h',
                'probability': 60.0,
                'actions': ['Visez 7-9h de sommeil']
            })
            action_priorities.append({
                'action': 'Augmenter le sommeil à 7.5h',
                'category': 'sleep_insufficient',
                'urgency': 'medium',
                'impact': 'Amélioration de 60%'
            })

        # ACTIVITÉ
        if current_steps < 2000:
            alerts.append("🚨 CRITIQUE: Sédentarité extrême")
            risk_factors.append({
                'type': 'severe_inactivity',
                'severity': 'high',
                'description': f'{current_steps} pas (recommandé: 10000)',
                'probability': 85.0,
                'actions': [
                    'Marchez 15 min après chaque repas',
                    'Prenez les escaliers',
                    'Faites une promenade quotidienne'
                ]
            })
            action_priorities.append({
                'action': 'Augmenter activité à 5000 pas/jour',
                'category': 'severe_inactivity',
                'urgency': 'high',
                'impact': 'Réduction de risque de 85%'
            })

        elif current_steps < 5000:
            alerts.append("⚠️ ATTENTION: Activité insuffisante")
            risk_factors.append({
                'type': 'low_activity',
                'severity': 'medium',
                'description': f'{current_steps} pas (cible: 10000)',
                'probability': 65.0,
                'actions': ['Augmentez progressivement à 10000 pas']
            })
            action_priorities.append({
                'action': 'Augmenter à 8000 pas/jour',
                'category': 'low_activity',
                'urgency': 'medium',
                'impact': 'Amélioration de 40%'
            })

        # STRESS
        if current_stress >= 80:
            alerts.append("🚨 CRITIQUE: Stress très élevé")
            risk_factors.append({
                'type': 'high_stress',
                'severity': 'high',
                'description': f'Stress à {current_stress}/100',
                'probability': 80.0,
                'actions': [
                    'Pratiquez la respiration profonde',
                    'Méditez 10 min/jour',
                    'Consultez un professionnel'
                ]
            })
            action_priorities.append({
                'action': 'Réduire stress à < 60/100',
                'category': 'high_stress',
                'urgency': 'high',
                'impact': 'Réduction de risque de 80%'
            })

        elif current_stress >= 60:
            alerts.append("⚠️ Stress modéré à élevé")
            risk_factors.append({
                'type': 'moderate_stress',
                'severity': 'medium',
                'description': f'Stress à {current_stress}/100',
                'probability': 60.0,
                'actions': [
                    'Exercices de relaxation',
                    'Méditation quotidienne'
                ]
            })
            action_priorities.append({
                'action': 'Techniques anti-stress quotidiennes',
                'category': 'moderate_stress',
                'urgency': 'medium',
                'impact': 'Réduction de risque de 60%'
            })

        # HYDRATATION
        if current_hydration < 1.0:
            alerts.append("⚠️ Déshydratation probable")
            risk_factors.append({
                'type': 'dehydration',
                'severity': 'medium',
                'description': f'{current_hydration}L (recommandé: 2.5L)',
                'probability': 70.0,
                'actions': ['Buvez 2.5L d\'eau par jour']
            })
            action_priorities.append({
                'action': 'Augmenter hydratation à 2.5L/jour',
                'category': 'dehydration',
                'urgency': 'medium',
                'impact': 'Réduction de risque de 70%'
            })

        # SIGNES VITAUX
        if latest.get('oxygenSaturation') and len(latest['oxygenSaturation']) > 0:
            spo2 = latest['oxygenSaturation'][-1].get('percentage', 100)
            if spo2 < 90:
                alerts.append("🚨 URGENCE: SpO2 critique (< 90%)")
                risk_factors.append({
                    'type': 'critical_oxygen',
                    'severity': 'critical',
                    'description': f'SpO2 à {spo2}%',
                    'probability': 100.0,
                    'actions': ['APPELEZ SAMU IMMÉDIATEMENT']
                })
                action_priorities.insert(0, {
                    'action': 'Consulter médecin IMMÉDIATEMENT',
                    'category': 'critical_oxygen',
                    'urgency': 'critical',
                    'impact': 'Vital'
                })

        if latest.get('bodyTemperature') and len(latest['bodyTemperature']) > 0:
            temp = latest['bodyTemperature'][-1].get('temperature', 36.5)
            if temp >= 39:
                alerts.append("🚨 Fièvre élevée détectée")
                risk_factors.append({
                    'type': 'high_fever',
                    'severity': 'high',
                    'description': f'Température à {temp}°C',
                    'probability': 80.0,
                    'actions': [
                        'Consultez médecin dans 24h',
                        'Paracétamol selon posologie',
                        'Hydratation importante'
                    ]
                })
                action_priorities.append({
                    'action': 'Consultation médicale + traitement fièvre',
                    'category': 'high_fever',
                    'urgency': 'high',
                    'impact': 'Réduction de risque de 80%'
                })

        # --- 4️⃣ Niveau d’alerte global ---
        critical_count = len([a for a in alerts if "🚨" in a])

        if critical_count > 0:
            alert_level = "Critique"
        elif len(alerts) >= 3:
            alert_level = "Élevé"
        elif len(alerts) > 0:
            alert_level = "Modéré"
        else:
            alert_level = "Faible"
            alerts.append("✅ Aucune alerte critique")

        # Trier actions
        urgency_order = {'critical': 0, 'high': 1, 'medium': 2, 'low': 3}
        action_priorities.sort(key=lambda x: urgency_order.get(x['urgency'], 3))

        # --- 5️⃣ Résultat final ---
        next_checkup = datetime.now() + timedelta(days=7)
        if critical_count > 0:
            next_checkup = datetime.now() + timedelta(days=3)

        return {
            'email': email,
            'alert_level': alert_level,
            'analysis_period':
                f"{start_date.strftime('%Y-%m-%d')} au {end_date.strftime('%Y-%m-%d')}"
                if not specific_date else f"Date: {specific_date}",
            'analysis_type': analysis_type,
            'data_points_analyzed': len(records),
            'averages_computed': {
                'steps': current_steps,
                'sleep_hours': round(current_sleep, 1),
                'heart_rate': current_hr,
                'stress_score': current_stress,
                'hydration_liters': round(current_hydration, 1)
            },
            'alerts': alerts,
            'risk_factors': risk_factors,
            'action_priorities': action_priorities,
            'next_checkup_recommended': next_checkup.isoformat()
        }

# =====================================================
# 🎯 GÉNÉRATEUR D'OBJECTIFS SMART
# =====================================================

class PersonalizedGoalsGenerator:
    """Génère des objectifs SMART personnalisés"""
    
    @staticmethod
    def generate_goals(email: str, preferences: GoalPreferences) -> Dict:
        """Génère des objectifs basés sur préférences utilisateur"""

        # -----------------------
        # 🔹 1. Charger les données
        # -----------------------
        end_date = datetime.now().strftime("%Y-%m-%d")
        start_date = (datetime.now() - timedelta(days=preferences.timeframe_days)).strftime("%Y-%m-%d")

        records = get_unique_daily_data(email, start_date, end_date)

        if len(records) == 0:
            raise HTTPException(status_code=404, detail="Aucune donnée trouvée")

        # Sécuriser tri des données (si une date existe)
        if "date" in records[0]:
            records.sort(key=lambda x: x.get("date"), reverse=True)

        latest = records[0]

        goals = []

        # --------------------------
        # 🔹 2. Calculs des moyennes
        # --------------------------
        avg_steps = np.mean([r.get('totalSteps') or 0 for r in records])
        avg_sleep = np.mean([float(r.get('totalSleepHours') or 7) for r in records])
        avg_hydration = np.mean([float(r.get('totalHydrationLiters') or 2) for r in records])
        avg_stress = np.mean([r.get('stressScore') or 50 for r in records])
        avg_hr = np.mean([r.get('avgHeartRate') or 70 for r in records])

        # -------------------------------
        # 🔹 3. Vérifier timeframe correct
        # -------------------------------
        if preferences.timeframe_days < 2:
            raise HTTPException(400, "timeframe_days doit être ≥ 2")

        # ---------------------------------
        # 🔹 4. Multiplicateur de difficulté
        # ---------------------------------
        difficulty_multipliers = {
            'easy': 1.1,
            'moderate': 1.25,
            'challenging': 1.5
        }
        multiplier = difficulty_multipliers.get(preferences.difficulty, 1.25)

        days = preferences.timeframe_days

        # ---------------------------------------------------
        # 🔹 5. OBJECTIF ACTIVITÉ (corrigé + sécurisé)
        # ---------------------------------------------------
        if 'activity' in preferences.preferred_goals:
            current_steps = int(avg_steps)

            if current_steps < 5000:
                target_steps = min(int(current_steps * multiplier), 5000)
            elif current_steps < 8000:
                target_steps = min(int(current_steps * multiplier), 10000)
            else:
                target_steps = min(int(current_steps * multiplier), 12000)

            # éviter division par zéro
            step_increment = (target_steps - current_steps) / max(1, days / 2)

            milestones = []
            for day in [days // 3, 2 * days // 3, days]:
                milestone_target = current_steps + (step_increment * (day / max(1, days / 2)))
                milestones.append({
                    'day': day,
                    'target': round(milestone_target, 1),
                    'description': f'Atteindre {int(milestone_target)} pas'
                })

            goals.append({
                'category': 'activity',
                'title': f'Atteindre {target_steps} pas par jour',
                'current': current_steps,
                'target': target_steps,
                'timeframe': f'{days} jours',
                'priority': 'high' if current_steps < 5000 else 'medium',
                'tips': [
                    'Marchez 10 min après chaque repas',
                    'Prenez les escaliers au lieu de l\'ascenseur',
                    'Descendez du bus un arrêt plus tôt',
                    'Faites une promenade le matin ou soir'
                ],
                'milestones': milestones,
                'expected_improvement': '+15 points au score santé'
            })

        # ---------------------------------------------------
        # 🔹 6. OBJECTIF SOMMEIL
        # ---------------------------------------------------
        if 'sleep' in preferences.preferred_goals:
            current_sleep = float(avg_sleep)

            if current_sleep < 7:
                target_sleep = 7.5
                priority = 'high'
            elif current_sleep > 9:
                target_sleep = 8.5
                priority = 'medium'
            else:
                target_sleep = 8.0
                priority = 'low'

            goals.append({
                'category': 'sleep',
                'title': f'Dormir {target_sleep}h par nuit',
                'current': round(current_sleep, 1),
                'target': target_sleep,
                'timeframe': f'{days} jours',
                'priority': priority,
                'tips': [
                    'Couchez-vous à heure fixe (22h-23h)',
                    'Évitez écrans 1h avant coucher',
                    'Chambre fraîche (18-20°C)',
                    'Pas de caféine après 16h',
                    'Routine relaxante: lecture, méditation'
                ],
                'milestones': [
                    {
                        'day': days // 2,
                        'target': (current_sleep + target_sleep) / 2,
                        'description': f'Atteindre {(current_sleep + target_sleep) / 2:.1f}h'
                    },
                    {
                        'day': days,
                        'target': target_sleep,
                        'description': f'Atteindre {target_sleep}h'
                    }
                ],
                'expected_improvement': '+12 points au score santé'
            })

        # ---------------------------------------------------
        # 🔹 7. OBJECTIF HYDRATATION
        # ---------------------------------------------------
        if 'hydration' in preferences.preferred_goals:
            current_hydration = float(avg_hydration)
            target_hydration = 2.5

            if current_hydration < 1.5:
                priority = 'high'
            elif current_hydration < 2.0:
                priority = 'medium'
            else:
                priority = 'low'

            goals.append({
                'category': 'hydration',
                'title': f'Boire {target_hydration}L d\'eau par jour',
                'current': round(current_hydration, 1),
                'target': target_hydration,
                'timeframe': f'{days} jours',
                'priority': priority,
                'tips': [
                    'Buvez 1 verre au réveil',
                    'Gardez une bouteille avec vous',
                    'Buvez avant chaque repas',
                    '1 verre toutes les 2 heures',
                    'Infusions et tisanes comptent'
                ],
                'milestones': [
                    {
                        'day': days // 3,
                        'target': current_hydration + (target_hydration - current_hydration) / 3,
                        'description': 'Premier palier'
                    },
                    {
                        'day': days,
                        'target': target_hydration,
                        'description': f'Atteindre {target_hydration}L/jour'
                    }
                ],
                'expected_improvement': '+5 points au score santé'
            })

        # ---------------------------------------------------
        # 🔹 8. OBJECTIF STRESS
        # ---------------------------------------------------
        if 'stress' in preferences.preferred_goals:
            current_stress = int(avg_stress)

            if current_stress >= 70:
                target_stress = 50
                priority = 'high'
            elif current_stress >= 50:
                target_stress = 40
                priority = 'medium'
            else:
                target_stress = 30
                priority = 'low'

            goals.append({
                'category': 'stress',
                'title': f'Réduire stress à {target_stress}/100',
                'current': current_stress,
                'target': target_stress,
                'timeframe': f'{days} jours',
                'priority': priority,
                'tips': [
                    'Méditation guidée (10 min/jour)',
                    'Exercices de respiration profonde',
                    'Yoga ou stretching',
                    'Activité physique régulière',
                    'Limiter exposition aux écrans',
                    'Temps pour hobbies relaxants'
                ],
                'milestones': [
                    {
                        'day': days // 2,
                        'target': (current_stress + target_stress) / 2,
                        'description': f'Réduire à {int((current_stress + target_stress) / 2)}/100'
                    },
                    {
                        'day': days,
                        'target': target_stress,
                        'description': f'Atteindre {target_stress}/100'
                    }
                ],
                'expected_improvement': '+8 points au score santé'
            })

        # ---------------------------------------------------
        # 🔹 9. OBJECTIF CARDIOVASCULAIRE
        # ---------------------------------------------------
        if 'cardiovascular' in preferences.preferred_goals:
            current_hr = int(avg_hr)

            if current_hr > 85:
                target_hr = 75
                priority = 'high'
                tips = [
                    'Exercices cardio 3x/semaine (30 min)',
                    'Marche rapide quotidienne',
                    'Natation ou vélo',
                    'Réduire caféine et stress'
                ]
            elif current_hr < 55:
                target_hr = 65
                priority = 'medium'
                tips = [
                    'Consultez un médecin',
                    'Exercices modérés réguliers'
                ]
            else:
                target_hr = 70
                priority = 'low'
                tips = [
                    'Maintenez exercices réguliers',
                    'Alimentation équilibrée'
                ]

            goals.append({
                'category': 'cardiovascular',
                'title': f'Optimiser FC moyenne à {target_hr} bpm',
                'current': current_hr,
                'target': target_hr,
                'timeframe': f'{days} jours',
                'priority': priority,
                'tips': tips,
                'milestones': [
                    {
                        'day': days,
                        'target': target_hr,
                        'description': f'FC stable à {target_hr} bpm'
                    }
                ],
                'expected_improvement': '+10 points au score santé'
            })

        # --------------------------------------
        # 🔹 10. Calcul estimations générales
        # --------------------------------------
        def extract_improvement(s: str) -> float:
            return float(s.replace("+", "").split()[0])

        total_improvement = sum(extract_improvement(g['expected_improvement']) for g in goals)
        high_priority_count = len([g for g in goals if g['priority'] == 'high'])

        # Optimiser calcul health score
        analyzer = AdvancedHealthAnalyzer()

        avg_health_score = np.mean([
            analyzer.calculate_health_score(
                BiometricData(**{
                    'totalSteps': r.get('totalSteps') or 0,
                    'avgHeartRate': r.get('avgHeartRate') or 70,
                    'minHeartRate': r.get('minHeartRate') or 60,
                    'maxHeartRate': r.get('maxHeartRate') or 90,
                    'totalDistanceKm': float(r.get('totalDistanceKm') or 0),
                    'totalSleepHours': float(r.get('totalSleepHours') or 7),
                    'totalHydrationLiters': float(r.get('totalHydrationLiters') or 2),
                    'stressLevel': r.get('stressLevel') or 'Modéré',
                    'stressScore': r.get('stressScore') or 50,
                    'oxygenSaturation': r.get('oxygenSaturation') or [],
                    'bodyTemperature': r.get('bodyTemperature') or [],
                    'bloodPressure': r.get('bloodPressure') or [],
                    'weight': r.get('weight') or [],
                    'height': r.get('height') or [],
                    'exercise': r.get('exercise') or []
                })
            )['total_score']
            for r in records
        ])

        projected_health_score = min(100, avg_health_score + total_improvement)

        return {
            'email': email,
            'total_goals': len(goals),
            'high_priority_count': high_priority_count,
            'timeframe_days': days,
            'difficulty': preferences.difficulty,
            'estimated_improvement': round(total_improvement, 1),
            "average_current_health_score": f"{round(avg_health_score, 1)} pour les 7 derniers jours",
            "projected_health_score": f"{round(projected_health_score, 1)} après atteinte des objectifs dans {days} jours",
            'goals': goals
        }



# =====================================================
# 🌐 ENDPOINTS API
# =====================================================

analyzer = AdvancedHealthAnalyzer()
ai_explainer = AIExplainer()

# ✅ ENDPOINT EXISTANT (CONSERVÉ)
@app.post("/analyze-health", response_model=HealthAnalysisResult)
async def analyze_health(data: BiometricData):
    """Analyse de santé complète (endpoint original)"""
    try:
        # 1. Calculer score
        score_result = analyzer.calculate_health_score(data)
        health_score = score_result['total_score']
        score_breakdown = score_result['breakdown']
        
        # 2. Anomalies
        anomalies = analyzer.detect_anomalies(data)
        
        # 3. Niveau de risque
        risk_level = analyzer.determine_risk_level(health_score, anomalies)
        
        # 4. Recommandations
        recommendations = analyzer.generate_recommendations(data, score_breakdown)
        
        # 5. Insights
        insights = {
            'score_breakdown': score_breakdown,
            'activity_details': {
                'steps': data.totalSteps,
                'distance_km': data.totalDistanceKm,
                'exercises_count': len(data.exercise) if data.exercise else 0
            },
            'cardiovascular_details': {
                'avg_heart_rate': data.avgHeartRate,
                'hr_variability': data.maxHeartRate - data.minHeartRate
            },
            'sleep_details': {
                'hours': data.totalSleepHours,
                'quality': 'Optimal' if 7 <= data.totalSleepHours <= 9 else 'À améliorer'
            },
            'stress_details': {
                'level': data.stressLevel,
                'score': data.stressScore
            }
        }
        
        # 6. Explication IA
        analysis_data = {
            'health_score': health_score,
            'risk_level': risk_level,
            'anomalies': anomalies,
            'breakdown': score_breakdown
        }
        ai_explanation = ai_explainer.generate_explanation(data, analysis_data)
        
        return HealthAnalysisResult(
            healthScore=health_score,
            riskLevel=risk_level,
            anomalies=anomalies,
            recommendations=recommendations,
            insights=insights,
            aiExplanation=ai_explanation
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erreur d'analyse: {str(e)}")

# 🆕 ENDPOINT 1: TENDANCES
@app.get("/health-trends/{email}")
async def get_health_trends(
    email: str,
    days: int = Query(default=30, ge=2, le=90, description="Nombre de jours d'historique")
):
    """Analyse des tendances de santé sur plusieurs jours"""
    try:
        trends = TrendsAnalyzer.get_user_trends(email, days)
        return trends
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erreur: {str(e)}")

# 🆕 ENDPOINT 2: ALERTES DE RISQUE
@app.get("/risk-alerts/{email}")
async def get_risk_alerts(
    email: str,
    period_days: Optional[int] = 7,
    specific_date: Optional[str] = None
):
    """
    Génère des alertes de risque personnalisées
    
    Args:
        email: email de l'utilisateur
        period_days: Nombre de jours (1, 7, 30) - défaut: 7
        specific_date: Date spécifique "YYYY-MM-DD" (optionnel)
    """
    try:
        alerts = RiskAlertsAnalyzer.generate_alerts(email, period_days, specific_date)
        return alerts
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erreur: {str(e)}")
    
# 🆕 ENDPOINT 3: OBJECTIFS PERSONNALISÉS
@app.post("/personalized-goals/{email}")
async def get_personalized_goals(
    email: str,
    preferences: GoalPreferences = GoalPreferences()
):
    """Génère des objectifs SMART personnalisés avec préférences utilisateur"""
    try:
        goals = PersonalizedGoalsGenerator.generate_goals(email, preferences)
        return goals
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erreur: {str(e)}")

# 📊 ENDPOINT: RÉSUMÉ COMPLET
@app.get("/health-summary/{user_id}")
async def get_health_summary(user_id: str):
    """Résumé complet de santé (dashboard)"""
    try:
        # Dernière donnée
        latest = collection.find_one(
            {"userId": user_id},
            sort=[("date", -1)]
        )
        
        if not latest:
            raise HTTPException(status_code=404, detail="Aucune donnée")
        
        # Calculer score actuel
        data = BiometricData(**{
            'totalSteps': latest.get('totalSteps', 0),
            'avgHeartRate': latest.get('avgHeartRate', 70),
            'minHeartRate': latest.get('minHeartRate', 60),
            'maxHeartRate': latest.get('maxHeartRate', 90),
            'totalDistanceKm': float(latest.get('totalDistanceKm', '0')),
            'totalSleepHours': float(latest.get('totalSleepHours', '7')),
            'totalHydrationLiters': float(latest.get('totalHydrationLiters', '2')),
            'stressLevel': latest.get('stressLevel', 'Modéré'),
            'stressScore': latest.get('stressScore', 50),
            'oxygenSaturation': latest.get('oxygenSaturation', []),
            'bodyTemperature': latest.get('bodyTemperature', []),
            'bloodPressure': latest.get('bloodPressure', []),
            'weight': latest.get('weight', []),
            'height': latest.get('height', []),
            'exercise': latest.get('exercise', [])
        })
        
        score_result = analyzer.calculate_health_score(data)
        anomalies = analyzer.detect_anomalies(data)
        risk_level = analyzer.determine_risk_level(score_result['total_score'], anomalies)
        recommendations = analyzer.generate_recommendations(data, score_result['breakdown'])
        
        # Tendances 7 jours
        try:
            trends = TrendsAnalyzer.get_user_trends(user_id, 7)
            health_scores = trends['trends']['health_scores']
            
            if len(health_scores) >= 2:
                score_change = health_scores[-1] - health_scores[0]
                if score_change > 2:
                    direction = 'improving'
                elif score_change < -2:
                    direction = 'declining'
                else:
                    direction = 'stable'
            else:
                score_change = 0
                direction = 'insufficient_data'
        except:
            score_change = 0
            direction = 'insufficient_data'
        
        return {
            'user_id': user_id,
            'timestamp': datetime.now().isoformat(),
            'current_health': {
                'score': score_result['total_score'],
                'risk_level': risk_level,
                'breakdown': score_result['breakdown']
            },
            'evolution': {
                'score_change_7d': round(score_change, 1),
                'direction': direction
            },
            'top_recommendations': recommendations[:5],
            'anomalies_count': len(anomalies),
            'data_quality': 'excellent' if latest else 'insufficient'
        }
        
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erreur: {str(e)}")

# ✅ HEALTH CHECK
@app.get("/health")
async def health_check():
    """Vérification API"""
    try:
        # Test connexion MongoDB
        db.command('ping')
        mongo_status = "connected"
    except:
        mongo_status = "disconnected"
    
    return {
        "status": "healthy",
        "version": "2.0.0",
        "mongodb": mongo_status,
        "endpoints": {
            "analyze": "/analyze-health (POST)",
            "trends": "/health-trends/{user_id} (GET)",
            "alerts": "/risk-alerts/{user_id} (GET)",
            "goals": "/personalized-goals/{user_id} (POST)",
            "summary": "/health-summary/{user_id} (GET)"
        }
    }

@app.get("/")
async def root():
    return {
        "message": "Health AI System v2.0.0",
        "documentation": "/docs",
        "features": [
            "Analyse de santé complète",
            "Tendances temporelles",
            "Alertes de risque",
            "Objectifs personnalisés SMART"
        ]
    }

# =====================================================
# 🚀 LANCEMENT AVEC EUREKA
# =====================================================

@app.on_event("startup")
async def startup_event():
    """Démarrage de l'application avec enregistrement Eureka"""
    print("🚀 Démarrage du Health AI System v2.0.0")

    # Enregistrement auprès d'Eureka
    if eureka_client.register_with_eureka():
        eureka_client.start_heartbeat()
    else:
        print("⚠️ Attention: Échec de l'enregistrement Eureka, mais le service démarre")

@app.on_event("shutdown")
async def shutdown_event():
    """Arrêt propre avec désenregistrement Eureka"""
    print("🛑 Arrêt du Health AI System")
    eureka_client.unregister()

if __name__ == "__main__":
    import uvicorn

    # Enregistrement pour désenregistrement propre à l'arrêt
    atexit.register(eureka_client.unregister)

    print("🚀 Lancement du Health AI System v2.0.0")
    print("📊 MongoDB: healthsync_db.biometric_data")
    print("🌐 API: http://localhost:8000")
    print("📖 Docs: http://localhost:8000/docs")
    print("🔗 Eureka: http://localhost:8761")

    uvicorn.run(app, host="0.0.0.0", port=8000)