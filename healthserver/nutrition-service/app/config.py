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
    
    # JWT
    JWT_SECRET = os.getenv('JWT_SECRET')
    JWT_ALGORITHM = os.getenv('JWT_ALGORITHM', 'HS256')
    JWT_ALGORITHMS = os.getenv('JWT_ALGORITHMS', 'HS256,RS256').split(',')
    
    # Auth Service
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
        if not Config.JWT_SECRET:
            raise ValueError("JWT_SECRET must be set in .env file")
        if len(Config.JWT_SECRET) < 64:
            raise ValueError("JWT_SECRET must be at least 32 characters")
        return True