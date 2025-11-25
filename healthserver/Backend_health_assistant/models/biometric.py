from pymongo import MongoClient
from config import Config
from datetime import datetime, timedelta

# Connexion MongoDB
client = MongoClient(Config.MONGO_URI)

# Utiliser directement le nom de la base de données
db = client.healthsync_db  # Ou db = client["healthsync_db"]
biometrics = db.biometric_data

print("Connexion MongoDB établie avec healthsync_db")

def get_latest_biometric_data(email: str):
    try:
        # Cherche le dernier enregistrement par date
        data = biometrics.find_one(
            {"email": email},
            sort=[("receivedAt", -1)]
        )

        if not data:
            print(f"Aucune donnée biométrique trouvée pour l'utilisateur {email}")
            return {}

        # Extraire les dernières valeurs
        return {
            "steps": data.get("totalSteps", 0),
            "avg_heart_rate": data.get("avgHeartRate", 0),
            "min_heart_rate": data.get("minHeartRate", 0),
            "max_heart_rate": data.get("maxHeartRate", 0),
            "distance_km": data.get("totalDistanceKm", "0"),
            "sleep_hours": data.get("totalSleepHours", "0"),
            "hydration_liters": data.get("totalHydrationLiters", "0"),
            "stress_level": data.get("stressLevel", "Inconnu"),
            "stress_score": data.get("stressScore", 0),
            "oxygen_saturation": get_last_value(data.get("oxygenSaturation", []), "percentage"),
            "body_temperature": get_last_value(data.get("bodyTemperature", []), "temperature"),
            "blood_pressure": get_last_bp(data.get("bloodPressure", [])),
            "weight_kg": get_last_value(data.get("weight", []), "weight"),
            "height_m": get_last_value(data.get("height", []), "height"),
        }
    except Exception as e:
        print(f"Erreur lors de la récupération des données biométriques: {e}")
        return {}

def get_last_value(arr, key):
    if not arr:
        return None
    return arr[-1].get(key)

def get_last_bp(arr):
    if not arr:
        return None
    bp = arr[-1]
    return f"{bp.get('systolic')}/{bp.get('diastolic')}"