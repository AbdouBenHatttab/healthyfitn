# config.py
import os
from dotenv import load_dotenv

# Charger le fichier .env
load_dotenv()

class Config:
    # API Configuration
    GROQ_API_KEY = os.getenv("GROQ_API_KEY")
    GROQ_MODEL = "llama-3.1-70b-versatile"

    # Server Configuration
    HOST = "0.0.0.0"
    PORT = 8000

    # Health Norms (basé sur OMS/CDC)
    HEALTH_NORMS = {
        'steps': {'optimal': 10000, 'minimum': 5000},
        'heart_rate': {'min': 60, 'max': 100},
        'sleep': {'optimal': 8, 'minimum': 7},
        'hydration': {'optimal': 2.5, 'minimum': 1.5}
    }
