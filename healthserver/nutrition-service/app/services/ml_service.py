# import tensorflow as tf
# import numpy as np
# from app.config import Config
# import os

# class MLService:
#     def __init__(self):
#         self.model = None
#         self.food_classes = self._load_food_classes()
#         self._load_model()
    
#     def _load_model(self):
#         """Load pre-trained food detection model"""
#         try:
#             model_path = Config.ML_MODEL_PATH
#             if os.path.exists(model_path):
#                 self.model = tf.keras.models.load_model(model_path)
#                 print(f"✅ ML Model loaded from {model_path}")
#             else:
#                 print(f"⚠️ Model not found at {model_path}. Using fallback.")
#                 # Create a simple placeholder model for demonstration
#                 self.model = self._create_placeholder_model()
#         except Exception as e:
#             print(f"❌ Error loading model: {e}")
#             self.model = self._create_placeholder_model()
    
#     def _create_placeholder_model(self):
#         """
#         Create a simple placeholder model for demonstration
#         In production, replace with actual trained model
#         """
#         model = tf.keras.Sequential([
#             tf.keras.layers.Conv2D(32, (3, 3), activation='relu', input_shape=(224, 224, 3)),
#             tf.keras.layers.MaxPooling2D((2, 2)),
#             tf.keras.layers.Flatten(),
#             tf.keras.layers.Dense(128, activation='relu'),
#             tf.keras.layers.Dense(len(self.food_classes), activation='softmax')
#         ])
#         return model
    
#     def _load_food_classes(self):
#         """
#         Load food classes
#         In production, load from file or database
#         """
#         return [
#             'apple', 'banana', 'bread', 'broccoli', 'burger',
#             'carrot', 'cheese', 'chicken', 'chocolate', 'coffee',
#             'donut', 'egg', 'fish', 'french_fries', 'grapes',
#             'hot_dog', 'ice_cream', 'milk', 'orange', 'pasta',
#             'pizza', 'potato', 'rice', 'salad', 'sandwich',
#             'steak', 'strawberry', 'sushi', 'tomato', 'water'
#         ]
    
#     def detect_food(self, preprocessed_image):
#         """
#         Detect food items in image
        
#         Args:
#             preprocessed_image: numpy array from ImageService.preprocess_image()
        
#         Returns:
#             list of dict with detected food items and confidence
#         """
#         try:
#             # Make prediction
#             predictions = self.model.predict(preprocessed_image, verbose=0)
            
#             # Get top 3 predictions
#             top_indices = np.argsort(predictions[0])[::-1][:3]
            
#             detected_foods = []
#             for idx in top_indices:
#                 confidence = float(predictions[0][idx])
#                 if confidence > 0.1:  # Only include predictions with >10% confidence
#                     detected_foods.append({
#                         'food_name': self.food_classes[idx],
#                         'confidence': round(confidence * 100, 2),
#                         'food_id': idx
#                     })
            
#             return detected_foods
        
#         except Exception as e:
#             raise Exception(f"Error detecting food: {str(e)}")
    
#     def extract_food_features(self, preprocessed_image):
#         """
#         Extract features from image for similarity search
#         Used for finding similar foods in database
        
#         Returns:
#             numpy array of features
#         """
#         try:
#             # Create feature extraction model (using layers before final classification)
#             feature_model = tf.keras.Model(
#                 inputs=self.model.input,
#                 outputs=self.model.layers[-2].output  # Second to last layer
#             )
            
#             features = feature_model.predict(preprocessed_image, verbose=0)
#             return features[0]  # Remove batch dimension
        
#         except Exception as e:
#             raise Exception(f"Error extracting features: {str(e)}")
    
#     def estimate_portion_size(self, image_array):
#         """
#         Estimate portion size from image
#         This is a simplified version - in production use more sophisticated methods
        
#         Returns:
#             dict with estimated portion size
#         """
#         try:
#             # Calculate image brightness as proxy for food density
#             brightness = np.mean(image_array)
            
#             # Simple heuristic (replace with actual portion size estimation model)
#             if brightness > 0.6:
#                 portion = 'large'
#                 multiplier = 1.5
#             elif brightness > 0.4:
#                 portion = 'medium'
#                 multiplier = 1.0
#             else:
#                 portion = 'small'
#                 multiplier = 0.7
            
#             return {
#                 'portion_size': portion,
#                 'portion_multiplier': multiplier
#             }
        
#         except Exception as e:
#             return {
#                 'portion_size': 'medium',
#                 'portion_multiplier': 1.0
#             }

"""
ML Service - Utilise le vrai modèle AI entraîné
REMPLACE l'ancien ml_service.py avec le placeholder
"""

import numpy as np
import logging
from app.services.ai_model_loader import predict_nutrition, get_model_info

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