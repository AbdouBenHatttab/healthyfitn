from flask import Flask, request, jsonify
from flask_cors import CORS
from services.gemini_service import generate_health_response
from models.biometric import get_latest_biometric_data
import os

app = Flask(__name__)
CORS(app)

@app.route("/chat", methods=["POST"])
def chat():
    try:
        data = request.get_json()
        user_id = data.get("userId")
        prompt = data.get("prompt", "").strip()

        if not user_id or not prompt:
            return jsonify({"error": "userId et prompt requis"}), 400

        bio_data = get_latest_biometric_data(user_id)
        response_text = generate_health_response(prompt, bio_data)

        return jsonify({
            "response": response_text
        })

    except Exception as e:
        return jsonify({"response": "Désolé, erreur serveur."}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)