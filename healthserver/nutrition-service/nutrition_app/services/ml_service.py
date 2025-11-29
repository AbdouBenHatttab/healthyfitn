
"""
ML Service - Utilise le vrai modèle AI entraîné
REMPLACE l'ancien ml_service.py avec le placeholder
"""

import numpy as np
import logging
from nutrition_app.services.ai_model_loader import predict_nutrition, get_model_info

logger = logging.getLogger(__name__)


class MLService:
    """
    Service ML utilisant le modèle TensorFlow entraîné
    """
    
    def __init__(self):
        """Initialisation du service ML"""
        logger.info("🤖 MLService initialisé avec modèle AI réel")
    
    def detect_food(self, preprocessed_image):
        """
        Détecte les aliments dans une image en utilisant le modèle AI
        
        Args:
            preprocessed_image: Image prétraitée (numpy array ou bytes)
        
        Returns:
            list: Liste des aliments détectés avec confiance
        """
        try:
            # Utiliser le modèle AI
            result = predict_nutrition(preprocessed_image)
            
            # Formater pour compatibilité avec l'ancienne interface
            detected_foods = [{
                'food_name': result['detected_food']['name'],
                'class_id': result['detected_food']['class_id'],
                'confidence': result['detected_food']['confidence'],
                'food_id': 0  # Placeholder
            }]
            
            # Ajouter les top 5 comme alternatives
            for pred in result['top5_predictions'][1:]:  # Ignorer le premier (déjà ajouté)
                detected_foods.append({
                    'food_name': pred['name'],
                    'class_id': pred['class_id'],
                    'confidence': pred['confidence'],
                    'food_id': 0
                })
            
            return detected_foods
            
        except Exception as e:
            logger.error(f"❌ Erreur détection: {str(e)}")
            # Fallback en cas d'erreur
            return self._fallback_detection(preprocessed_image)
    
    def get_nutrition_from_ai(self, preprocessed_image):
        """
        NOUVELLE MÉTHODE: Obtient directement nutrition + classification
        
        Args:
            preprocessed_image: Image prétraitée
        
        Returns:
            dict: Résultat complet avec nutrition et classification
        """
        try:
            return predict_nutrition(preprocessed_image)
        except Exception as e:
            logger.error(f"❌ Erreur prédiction nutrition: {str(e)}")
            raise
    
    def extract_food_features(self, preprocessed_image):
        """
        Extrait les features d'une image (pour recherche de similarité)
        NON IMPLÉMENTÉ dans cette version
        """
        logger.warning("⚠️  Feature extraction non implémentée")
        return np.random.rand(128)  # Placeholder
    
    def estimate_portion_size(self, image_array):
        """
        Estime la taille de la portion
        Utilise une heuristique simple basée sur la luminosité
        """
        try:
            # Calculer la luminosité moyenne
            if isinstance(image_array, bytes):
                from PIL import Image
                import io
                img = Image.open(io.BytesIO(image_array))
                image_array = np.array(img)
            
            brightness = np.mean(image_array) / 255.0
            
            # Heuristique simple
            if brightness > 0.6:
                portion = 'large'
                multiplier = 1.5
            elif brightness > 0.4:
                portion = 'medium'
                multiplier = 1.0
            else:
                portion = 'small'
                multiplier = 0.7
            
            return {
                'portion_size': portion,
                'portion_multiplier': multiplier
            }
            
        except Exception as e:
            logger.error(f"❌ Erreur estimation portion: {str(e)}")
            return {
                'portion_size': 'medium',
                'portion_multiplier': 1.0
            }
    
    def get_model_status(self):
        """
        Retourne le statut du modèle AI
        """
        try:
            info = get_model_info()
            return {
                'status': 'loaded' if info['model_loaded'] else 'not_loaded',
                'model_info': info
            }
        except Exception as e:
            return {
                'status': 'error',
                'error': str(e)
            }
    
    def _fallback_detection(self, image_array):
        """
        Fallback en cas d'erreur du modèle AI
        """
        logger.warning("⚠️  Utilisation du fallback detection")
        
        # Retourner une détection générique
        return [{
            'food_name': 'Unknown Food',
            'confidence': 50.0,
            'food_id': 0
        }]