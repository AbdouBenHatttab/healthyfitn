import google.generativeai as genai
from config import Config

genai.configure(api_key=Config.GEMINI_API_KEY)
model = genai.GenerativeModel("gemini-2.5-flash-lite")

def generate_health_response(prompt: str, bio: dict) -> str:
    print("Prompt envoyé à Gemini :", prompt)

    context = f"""
    Tu es un assistant santé vocal. 1-2 phrases max, rassurant.

    Données biométriques :
    - Pas : {bio.get('steps', 0)}
    - Fréquence cardiaque moyenne : {bio.get('avg_heart_rate', 0)} bpm
    - Tension : {bio.get('blood_pressure', 'inconnue')}
    - Poids : {bio.get('weight_kg', 'inconnu')} kg
    - Sommeil : {bio.get('sleep_hours', 0)} h
    - Hydratation : {bio.get('hydration_liters', 0)} L
    - Stress : {bio.get('stress_level', 'inconnu')} (score {bio.get('stress_score', 0)})

    Question : {prompt}

    Conseil pertinent.
    """

    try:
        response = model.generate_content(context)
        print("Réponse Gemini :", response.text)
        return response.text.strip()
    except Exception as e:
        print("ERREUR GEMINI :", str(e))
        return "Désolé, je n'arrive pas à répondre pour le moment."