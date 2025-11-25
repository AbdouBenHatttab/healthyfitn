from flask import Flask, request, jsonify
from flask_cors import CORS
from services.gemini_service import generate_health_response
from models.biometric import get_latest_biometric_data
from eureka_client import eureka_client
import os
import atexit

app = Flask(__name__)
CORS(app)

# Configuration Eureka
app.config['EUREKA_URL'] = os.getenv('EUREKA_URL', 'http://localhost:8761/eureka')
app.config['APP_NAME'] = os.getenv('APP_NAME', 'health-assistant-service')
app.config['HOSTNAME'] = os.getenv('HOSTNAME', 'localhost')
app.config['PORT'] = int(os.getenv('PORT', 5000))

# Initialisation du client Eureka
eureka_client.init_app(app)

@app.route("/chat", methods=["POST"])
def chat():
    try:
        data = request.get_json()
        email = data.get("email")
        prompt = data.get("prompt", "").strip()

        if not email or not prompt:
            return jsonify({"error": "email et prompt requis"}), 400

        bio_data = get_latest_biometric_data(email)
        response_text = generate_health_response(prompt, bio_data)

        return jsonify({
            "response": response_text
        })

    except Exception as e:
        app.logger.error(f"Erreur dans /chat: {str(e)}")
        return jsonify({"response": "Désolé, erreur serveur."}), 500

@app.route("/health", methods=["GET"])
def health_check():
    """Endpoint de santé pour Eureka"""
    return jsonify({"status": "UP"}), 200

@app.route("/info", methods=["GET"])
def info():
    """Endpoint d'information pour Eureka"""
    return jsonify({
        "name": app.config['APP_NAME'],
        "status": "UP",
        "version": "1.0.0"
    }), 200

def register_service():
    """Enregistre le service auprès d'Eureka"""
    success = eureka_client.register_with_eureka()
    if not success:
        app.logger.warning("Échec de l'enregistrement Eureka, réessai dans 10 secondes...")
        # Réessayer après 10 secondes
        import threading
        timer = threading.Timer(10.0, register_service)
        timer.daemon = True
        timer.start()

def deregister_service():
    """Désenregistre le service à l'arrêt"""
    eureka_client.deregister()

if __name__ == "__main__":
    # Enregistrement au démarrage
    register_service()

    # Désenregistrement à l'arrêt
    atexit.register(deregister_service)

    app.run(host="0.0.0.0", port=5000, debug=True)