"""
Main Application - MODIFIÉ pour Eureka + AI Model
"""

from flask import Flask, jsonify
from flask_cors import CORS
from nutrition_app.config import Config
from nutrition_app.routes.nutrition_routes import nutrition_bp
import logging
import os
import atexit

# ⭐ NOUVEAU: Import Eureka Client
from eureka_client import eureka_client

# Import du model loader
from nutrition_app.services.ai_model_loader import init_model

def create_app():
    """Factory function to create Flask app"""

    # Initialize Flask app
    app = Flask(__name__)

    # Load configuration
    app.config.from_object(Config)
    Config.validate()

    # ========================================
    # ⭐ NOUVEAU: Configuration Eureka
    # ========================================
    app.config['EUREKA_URL'] = os.getenv('EUREKA_URL', 'http://localhost:8761/eureka')
    app.config['APP_NAME'] = os.getenv('APP_NAME', 'nutrition-service')
    app.config['HOSTNAME'] = os.getenv('HOSTNAME', 'localhost')
    app.config['PORT'] = int(os.getenv('PORT', Config.FLASK_PORT))

    # Initialiser le client Eureka
    eureka_client.init_app(app)
    # ========================================

    # Enable CORS
    CORS(app, resources={
        r"/api/*": {
            "origins": ["http://localhost:4200", "http://localhost:8080"],
            "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
            "allow_headers": ["Content-Type", "Authorization"],
            "supports_credentials": True
        }
    })

    # Configure logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
    )

    # Charger le modèle AI au démarrage
    with app.app_context():
        try:
            init_model()
            app.logger.info("✅ Modèle AI chargé avec succès")
        except Exception as e:
            app.logger.error(f"❌ Erreur chargement modèle AI: {str(e)}")
            app.logger.warning("⚠️  Le service fonctionnera en mode dégradé (sans AI)")

    # Register blueprints
    app.register_blueprint(nutrition_bp, url_prefix='/api/v1/nutrition')

    # Global error handlers
    @app.errorhandler(404)
    def not_found(error):
        return jsonify({
            'success': False,
            'message': 'Resource not found'
        }), 404

    @app.errorhandler(500)
    def internal_error(error):
        app.logger.error(f'Internal Server Error: {error}')
        return jsonify({
            'success': False,
            'message': 'Internal server error'
        }), 500

    @app.errorhandler(Exception)
    def handle_exception(error):
        app.logger.error(f'Unhandled exception: {error}')
        return jsonify({
            'success': False,
            'message': 'An unexpected error occurred'
        }), 500

    # Root endpoint
    @app.route('/')
    def index():
        return jsonify({
            'service': 'Nutrition Analysis Service',
            'version': '1.0.0',
            'status': 'running',
            'ai_enabled': True,
            'eureka_enabled': True,  # ⭐ NOUVEAU
            'endpoints': {
                'analyze': 'POST /api/v1/nutrition/analyze',
                'model_status': 'GET /api/v1/nutrition/model/status',
                'history': 'GET /api/v1/nutrition/history',
                'statistics': 'GET /api/v1/nutrition/statistics',
                'health': 'GET /health',  # ⭐ MODIFIÉ (plus de /api/v1/nutrition)
                'info': 'GET /info'  # ⭐ NOUVEAU
            }
        })

    # ========================================
    # ⭐ NOUVEAU: Endpoints requis pour Eureka
    # ========================================
    @app.route('/health')
    def health():
        """Endpoint de santé pour Eureka"""
        return jsonify({
            'status': 'UP',
            'service': 'nutrition-service',
            'ai_model': 'loaded'
        }), 200

    @app.route('/info')
    def info():
        """Endpoint d'information pour Eureka"""
        return jsonify({
            'name': app.config['APP_NAME'],
            'status': 'UP',
            'version': '1.0.0',
            'description': 'Nutrition Analysis Service with AI',
            'port': app.config['PORT']
        }), 200
    # ========================================

    return app


# ========================================
# ⭐ NOUVEAU: Fonctions de gestion Eureka
# ========================================
def register_service():
    """Enregistre le service auprès d'Eureka"""
    success = eureka_client.register_with_eureka()
    if not success:
        print("⚠️  Échec de l'enregistrement Eureka, réessai dans 10 secondes...")
        import threading
        timer = threading.Timer(10.0, register_service)
        timer.daemon = True
        timer.start()

def deregister_service():
    """Désenregistre le service à l'arrêt"""
    eureka_client.deregister()
# ========================================


# Main execution
if __name__ == '__main__':
    app = create_app()

    print("""
    ========================================
    🥗 Nutrition Service Started!
    📍 Port: 8086
    📊 MongoDB: health_nutrition_db
    🖼️  MinIO: nutrition-images bucket
    🤖 AI Model: Loaded
    🔍 Eureka: Enabled  ⭐ NOUVEAU
    🎯 Endpoints:
       POST /api/v1/nutrition/analyze
       GET  /api/v1/nutrition/model/status
       GET  /api/v1/nutrition/history
       GET  /api/v1/nutrition/statistics
       GET  /health (Eureka)  ⭐ NOUVEAU
       GET  /info (Eureka)    ⭐ NOUVEAU
    ========================================
    """)

    # ⭐ NOUVEAU: Enregistrement Eureka au démarrage
    register_service()

    # ⭐ NOUVEAU: Désenregistrement à l'arrêt
    atexit.register(deregister_service)

    app.run(
        host='0.0.0.0',
        port=app.config['PORT'],
        debug=(Config.FLASK_ENV == 'development')
    )