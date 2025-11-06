# from flask import Flask, jsonify
# from flask_cors import CORS
# from app.config import Config
# from app.routes.nutrition_routes import nutrition_bp
# import logging

# def create_app():
#     """Factory function to create Flask app"""
    
#     # Initialize Flask app
#     app = Flask(__name__)
    
#     # Load configuration
#     app.config.from_object(Config)
#     Config.validate()
    
#     # Enable CORS
#     CORS(app, resources={
#         r"/api/*": {
#             "origins": ["http://localhost:4200", "http://localhost:8080"],
#             "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
#             "allow_headers": ["Content-Type", "Authorization"],
#             "supports_credentials": True
#         }
#     })
    
#     # Configure logging
#     logging.basicConfig(
#         level=logging.INFO,
#         format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
#     )
    
#     # Register blueprints
#     app.register_blueprint(nutrition_bp, url_prefix='/api/v1/nutrition')
    
#     # Global error handlers
#     @app.errorhandler(404)
#     def not_found(error):
#         return jsonify({
#             'success': False,
#             'message': 'Resource not found'
#         }), 404
    
#     @app.errorhandler(500)
#     def internal_error(error):
#         app.logger.error(f'Internal Server Error: {error}')
#         return jsonify({
#             'success': False,
#             'message': 'Internal server error'
#         }), 500
    
#     @app.errorhandler(Exception)
#     def handle_exception(error):
#         app.logger.error(f'Unhandled exception: {error}')
#         return jsonify({
#             'success': False,
#             'message': 'An unexpected error occurred'
#         }), 500
    
#     # Root endpoint
#     @app.route('/')
#     def index():
#         return jsonify({
#             'service': 'Nutrition Analysis Service',
#             'version': '1.0.0',
#             'status': 'running',
#             'endpoints': {
#                 'analyze': 'POST /api/v1/nutrition/analyze',
#                 'history': 'GET /api/v1/nutrition/history',
#                 'statistics': 'GET /api/v1/nutrition/statistics',
#                 'health': 'GET /api/v1/nutrition/health'
#             }
#         })
    
#     # Health check
#     @app.route('/health')
#     def health():
#         return jsonify({
#             'status': 'healthy',
#             'service': 'nutrition-service'
#         })
    
#     return app


# # Main execution
# if __name__ == '__main__':
#     app = create_app()
    
#     print("""
#     ========================================
#     🥗 Nutrition Service Started!
#     📍 Port: 8086
#     📊 MongoDB: health_nutrition_db
#     🖼️  MinIO: nutrition-images bucket
#     🤖 ML Model: Loaded
#     🎯 Endpoints:
#        POST /api/v1/nutrition/analyze
#        GET  /api/v1/nutrition/history
#        GET  /api/v1/nutrition/statistics
#     ========================================
#     """)
    
#     app.run(
#         host='0.0.0.0',
#         port=Config.FLASK_PORT,
#         debug=(Config.FLASK_ENV == 'development')
#     )


"""
Main Application - MODIFIÉ pour charger le modèle AI au démarrage
"""

from flask import Flask, jsonify
from flask_cors import CORS
from app.config import Config
from app.routes.nutrition_routes import nutrition_bp
import logging

# ⭐ NOUVEAU: Import du model loader
from app.services.ai_model_loader import init_model

def create_app():
    """Factory function to create Flask app"""
    
    # Initialize Flask app
    app = Flask(__name__)
    
    # Load configuration
    app.config.from_object(Config)
    Config.validate()
    
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
    
    # ========================================
    # ⭐ NOUVEAU: Charger le modèle AI au démarrage
    # ========================================
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
            'ai_enabled': True,  # ⭐ NOUVEAU
            'endpoints': {
                'analyze': 'POST /api/v1/nutrition/analyze',
                'model_status': 'GET /api/v1/nutrition/model/status',  # ⭐ NOUVEAU
                'history': 'GET /api/v1/nutrition/history',
                'statistics': 'GET /api/v1/nutrition/statistics',
                'health': 'GET /api/v1/nutrition/health'
            }
        })
    
    # Health check
    @app.route('/health')
    def health():
        return jsonify({
            'status': 'healthy',
            'service': 'nutrition-service'
        })
    
    return app


# Main execution
if __name__ == '__main__':
    app = create_app()
    
    print("""
    ========================================
    🥗 Nutrition Service Started!
    📍 Port: 8086
    📊 MongoDB: health_nutrition_db
    🖼️  MinIO: nutrition-images bucket
    🤖 AI Model: Loaded  ⭐ NOUVEAU
    🎯 Endpoints:
       POST /api/v1/nutrition/analyze
       GET  /api/v1/nutrition/model/status  ⭐ NOUVEAU
       GET  /api/v1/nutrition/history
       GET  /api/v1/nutrition/statistics
    ========================================
    """)
    
    app.run(
        host='0.0.0.0',
        port=Config.FLASK_PORT,
        debug=(Config.FLASK_ENV == 'development')
    )