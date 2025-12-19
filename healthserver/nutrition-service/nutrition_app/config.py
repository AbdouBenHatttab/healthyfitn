import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    # MongoDB
    MONGO_URI = os.getenv('MONGO_URI', 'mongodb://localhost:27017/')
    MONGO_DATABASE = os.getenv('MONGO_DATABASE', 'health_nutrition_db')

    # MinIO
    MINIO_ENDPOINT = os.getenv('MINIO_ENDPOINT', 'localhost:9000')
    MINIO_ACCESS_KEY = os.getenv('MINIO_ACCESS_KEY', 'minioadmin')
    MINIO_SECRET_KEY = os.getenv('MINIO_SECRET_KEY', 'minioadmin')
    MINIO_BUCKET = os.getenv('MINIO_BUCKET', 'nutrition-images')
    MINIO_SECURE = os.getenv('MINIO_SECURE', 'False').lower() == 'true'

    # ========================================
    # ⭐ KEYCLOAK CONFIGURATION
    # ========================================
    KEYCLOAK_SERVER_URL = os.getenv('KEYCLOAK_SERVER_URL', 'http://localhost:8080')
    KEYCLOAK_REALM = os.getenv('KEYCLOAK_REALM', 'health-app-realm')
    KEYCLOAK_CLIENT_ID = os.getenv('KEYCLOAK_CLIENT_ID', 'health-backend-services')
    KEYCLOAK_CLIENT_SECRET = os.getenv('KEYCLOAK_CLIENT_SECRET', 'iMeoAcmu6sVppVs5X523cmfBCsJmdWbA')

    # JWT Legacy (pour backward compatibility si besoin)
    JWT_SECRET = os.getenv('JWT_SECRET')
    JWT_ALGORITHM = os.getenv('JWT_ALGORITHM', 'HS256')
    JWT_ALGORITHMS = os.getenv('JWT_ALGORITHMS', 'RS256,HS256').split(',')  # RS256 en priorité

    # Auth Service (pour validation fallback)
    AUTH_SERVICE_URL = os.getenv('AUTH_SERVICE_URL', 'http://localhost:8082')

    # Flask
    FLASK_ENV = os.getenv('FLASK_ENV', 'development')
    FLASK_PORT = int(os.getenv('FLASK_PORT', 8086))

    # APIs
    OPENFOODFACTS_API = os.getenv('OPENFOODFACTS_API', 'https://world.openfoodfacts.org/api/v2')

    # ML Model
    ML_MODEL_PATH = os.getenv('ML_MODEL_PATH', 'models/food_detection_model.h5')

    @staticmethod
    def validate():
        """Validate critical configuration"""
        # Keycloak est maintenant prioritaire
        if not Config.KEYCLOAK_SERVER_URL:
            raise ValueError("KEYCLOAK_SERVER_URL must be set in .env file")
        if not Config.KEYCLOAK_REALM:
            raise ValueError("KEYCLOAK_REALM must be set in .env file")

        # JWT_SECRET optionnel (seulement pour fallback)
        if Config.JWT_SECRET and len(Config.JWT_SECRET) < 32:
            raise ValueError("JWT_SECRET must be at least 32 characters if provided")

        return True