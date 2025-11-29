
"""
AI Model Loader Service - VERSION COMPLÈTE AVEC SCALER
Charge le modèle TensorFlow entraîné avec scaler pour dénormalisation
"""

import os
import json
import numpy as np
import pickle
from pathlib import Path
import logging

# Configuration du logging
logger = logging.getLogger(__name__)

# Variables globales pour le modèle (chargé une seule fois)
_model = None
_scaler = None
_food_classes = None
_nutrition_db = None

# Chemins
BASE_DIR = Path(__file__).parent.parent.parent
MODELS_DIR = BASE_DIR / "models"
MODEL_PATH = MODELS_DIR / "nutrition_model.h5"
SCALER_PATH = MODELS_DIR / "nutrition_scaler.pkl"
CLASSES_PATH = MODELS_DIR / "food_classes.json"
NUTRITION_DB_PATH = MODELS_DIR / "nutrition_database.json"
METADATA_PATH = MODELS_DIR / "model_metadata.json"


def load_model():
    """
    Charge le modèle TensorFlow et le scaler (une seule fois au démarrage)
    """
    global _model, _scaler, _food_classes, _nutrition_db
    
    if _model is not None:
        return _model
    
    try:
        logger.info("🔄 Chargement du modèle AI...")
        
        # Vérifier que les fichiers existent
        if not MODEL_PATH.exists():
            raise FileNotFoundError(f"Modèle non trouvé: {MODEL_PATH}")
        
        if not CLASSES_PATH.exists():
            raise FileNotFoundError(f"Classes non trouvées: {CLASSES_PATH}")
        
        if not NUTRITION_DB_PATH.exists():
            raise FileNotFoundError(f"Base nutritionnelle non trouvée: {NUTRITION_DB_PATH}")
        
        # Charger TensorFlow
        import tensorflow as tf
        
        # Désactiver les warnings TensorFlow
        os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
        tf.get_logger().setLevel('ERROR')
        
        # Charger le modèle
        _model = tf.keras.models.load_model(str(MODEL_PATH), compile=False)
        logger.info(f"✅ Modèle chargé depuis: {MODEL_PATH}")
        
        # Charger le scaler (normalisation/dénormalisation)
        if SCALER_PATH.exists():
            with open(SCALER_PATH, 'rb') as f:
                _scaler = pickle.load(f)
            logger.info(f"✅ Scaler chargé depuis: {SCALER_PATH}")
        else:
            logger.warning("⚠️  Scaler non trouvé - les valeurs ne seront pas dénormalisées")
            _scaler = None
        
        # Charger les classes
        with open(CLASSES_PATH, 'r') as f:
            _food_classes = json.load(f)
        logger.info(f"✅ {len(_food_classes)} classes chargées")
        
        # Charger la base nutritionnelle
        with open(NUTRITION_DB_PATH, 'r') as f:
            _nutrition_db = json.load(f)
        logger.info(f"✅ Base nutritionnelle chargée ({len(_nutrition_db)} plats)")
        
        # Test du modèle
        dummy_input = np.random.rand(1, 224, 224, 3).astype(np.float32)
        _ = _model.predict(dummy_input, verbose=0)
        logger.info("✅ Modèle AI opérationnel")
        
        return _model
        
    except Exception as e:
        logger.error(f"❌ Erreur lors du chargement du modèle: {str(e)}")
        raise


def preprocess_image(image_array):
    """
    Prétraite une image pour le modèle
    
    Args:
        image_array: numpy array, bytes, ou PIL Image
    
    Returns:
        numpy array prétraité (1, 224, 224, 3)
    """
    try:
        from PIL import Image
        import io
        
        # ========================================
        # ÉTAPE 1: Convertir en numpy array si nécessaire
        # ========================================
        
        # Si l'image vient déjà prétraitée d'image_service avec batch dimension
        if isinstance(image_array, np.ndarray):
            # Vérifier la forme
            if image_array.ndim == 4:  # (1, 224, 224, 3) ou (1, 1, 224, 3)
                # Supprimer les dimensions extra
                if image_array.shape[0] == 1:
                    image_array = image_array[0]  # (1, 224, 224, 3) → (224, 224, 3)
                
                # Vérifier si c'est encore mal formé
                if image_array.ndim == 3 and image_array.shape[0] == 1:
                    image_array = image_array[0]  # (1, 224, 3) → (224, 3) (erreur)
            
            # Si forme correcte (224, 224, 3), reconvertir en image pour normaliser
            if image_array.ndim == 3 and image_array.shape == (224, 224, 3):
                # Vérifier si déjà normalisé [0,1]
                if image_array.max() <= 1.0:
                    # Déjà normalisé, juste ajouter batch dimension
                    return np.expand_dims(image_array, axis=0)
                else:
                    # Pas normalisé, convertir en PIL pour traiter
                    image = Image.fromarray(image_array.astype('uint8'))
            else:
                # Forme incorrecte, essayer de reconstruire
                logger.warning(f"⚠️  Forme inattendue: {image_array.shape}, reconstruction...")
                # Essayer de prendre la bonne partie
                if image_array.ndim >= 2:
                    # Flatten et reshape si possible
                    try:
                        if image_array.size == 224 * 224 * 3:
                            image_array = image_array.reshape(224, 224, 3)
                            if image_array.max() > 1.0:
                                image = Image.fromarray(image_array.astype('uint8'))
                            else:
                                return np.expand_dims(image_array, axis=0)
                        else:
                            raise ValueError(f"Taille incorrecte: {image_array.size}")
                    except Exception as reshape_error:
                        logger.error(f"Impossible de reshape: {reshape_error}")
                        raise ValueError(f"Format d'image invalide: {image_array.shape}")
        
        # Si c'est des bytes
        elif isinstance(image_array, bytes):
            image = Image.open(io.BytesIO(image_array))
        
        # Si c'est déjà une PIL Image
        elif hasattr(image_array, 'convert'):
            image = image_array
        
        else:
            raise ValueError(f"Type d'image non supporté: {type(image_array)}")
        
        # ========================================
        # ÉTAPE 2: Traiter l'image PIL
        # ========================================
        
        # Convertir en RGB si nécessaire
        if image.mode != 'RGB':
            image = image.convert('RGB')
        
        # Redimensionner à 224x224
        if image.size != (224, 224):
            image = image.resize((224, 224), Image.Resampling.LANCZOS)
        
        # Convertir en numpy array
        img_array = np.array(image)
        
        # Normaliser [0, 255] -> [0, 1]
        img_array = img_array.astype('float32') / 255.0
        
        # Ajouter dimension batch (224, 224, 3) -> (1, 224, 224, 3)
        img_array = np.expand_dims(img_array, axis=0)
        
        logger.debug(f"✅ Image prétraitée: {img_array.shape}")
        
        return img_array
        
    except Exception as e:
        logger.error(f"❌ Erreur prétraitement image: {str(e)}")
        raise


def predict_nutrition(image_array):
    """
    Fait une prédiction sur une image avec dénormalisation via scaler
    
    Args:
        image_array: Image prétraitée, bytes ou PIL Image
    
    Returns:
        dict avec les résultats de prédiction
    """
    global _model, _scaler, _food_classes, _nutrition_db
    
    try:
        # Charger le modèle si pas déjà fait
        if _model is None:
            load_model()
        
        # Prétraiter l'image
        processed_image = preprocess_image(image_array)
        
        logger.info("🤖 Prédiction en cours...")
        logger.debug(f"📊 Shape entrée modèle: {processed_image.shape}")
        
        # Prédiction
        classification_pred, nutrition_pred = _model.predict(processed_image, verbose=0)
        
        # Top 5 prédictions de classes
        top5_indices = np.argsort(classification_pred[0])[::-1][:5]
        top5_probs = classification_pred[0][top5_indices]
        top5_classes = [_food_classes[idx] for idx in top5_indices]
        
        # Classe principale
        main_class = top5_classes[0]
        main_prob = float(top5_probs[0])
        
        # ⭐ DÉNORMALISER les valeurs nutritionnelles avec le scaler
        if _scaler is not None:
            nutrition_real = _scaler.inverse_transform(nutrition_pred)[0]
        else:
            # Si pas de scaler, utiliser les valeurs brutes (non recommandé)
            nutrition_real = nutrition_pred[0]
            logger.warning("⚠️  Prédiction sans dénormalisation (scaler manquant)")
        
        # Valeurs nutritionnelles prédites (assurer valeurs positives)
        predicted_nutrition = {
            'calories': round(max(0, float(nutrition_real[0])), 1),
            'protein': round(max(0, float(nutrition_real[1])), 1),
            'fat': round(max(0, float(nutrition_real[2])), 1),
            'carbs': round(max(0, float(nutrition_real[3])), 1),
            'fiber': round(max(0, float(nutrition_real[4])), 1),
            'sugars': round(max(0, float(nutrition_real[5])), 1),
            'sodium': round(max(0, float(nutrition_real[6])), 1)
        }
        
        # Valeurs nutritionnelles de référence (si disponibles)
        reference_nutrition = _nutrition_db.get(main_class, {})
        
        # Résultat complet
        result = {
            'detected_food': {
                'name': main_class.replace('_', ' ').title(),
                'class_id': main_class,
                'confidence': round(main_prob * 100, 2)
            },
            'nutrition': predicted_nutrition,
            'reference_nutrition': reference_nutrition,
            'top5_predictions': [
                {
                    'name': cls.replace('_', ' ').title(),
                    'class_id': cls,
                    'confidence': round(float(prob) * 100, 2)
                }
                for cls, prob in zip(top5_classes, top5_probs)
            ]
        }
        
        logger.info(f"✅ Prédiction: {main_class} ({main_prob*100:.1f}%)")
        
        return result
        
    except Exception as e:
        logger.error(f"❌ Erreur prédiction: {str(e)}")
        raise


def get_model_info():
    """
    Retourne les informations sur le modèle chargé
    """
    global _model, _scaler, _food_classes
    
    if _model is None:
        load_model()
    
    return {
        'model_loaded': _model is not None,
        'model_path': str(MODEL_PATH),
        'scaler_loaded': _scaler is not None,
        'scaler_path': str(SCALER_PATH) if SCALER_PATH.exists() else None,
        'num_classes': len(_food_classes) if _food_classes else 0,
        'input_shape': [1, 224, 224, 3],
        'output_shapes': {
            'classification': [1, len(_food_classes)] if _food_classes else [1, 0],
            'nutrition': [1, 7]
        }
    }


def init_model():
    """
    Initialise le modèle au démarrage de l'application
    À appeler dans app/main.py
    """
    try:
        logger.info("=" * 60)
        logger.info("🚀 INITIALISATION DU MODÈLE AI")
        logger.info("=" * 60)
        
        load_model()
        
        logger.info("=" * 60)
        logger.info("✅ MODÈLE AI PRÊT")
        logger.info("=" * 60)
        
    except Exception as e:
        logger.error("=" * 60)
        logger.error(f"❌ ERREUR CHARGEMENT MODÈLE: {str(e)}")
        logger.error("=" * 60)
        logger.error("⚠️  Le service fonctionnera en mode dégradé")