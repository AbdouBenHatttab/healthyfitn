"""
03_build_model.py
Construit l'architecture du modèle de nutrition avec transfer learning

Architecture:
- Base: EfficientNetV2B0 (pré-entraîné sur ImageNet)
- Classification: 101 classes de plats
- Régression: 7 valeurs nutritionnelles

Utilisation:
    python 03_build_model.py

Durée: < 1 minute
"""

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import json
from pathlib import Path

BASE_DIR = Path("./nutrition_ai_data")
NUTRITION_FILE = BASE_DIR / "nutrition_database.json"
MODEL_DIR = BASE_DIR / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)

# Configuration
IMG_SIZE = 224
NUM_CLASSES = 101
NUM_NUTRITION_VALUES = 7  # calories, protein, fat, carbs, fiber, sugars, sodium

# Charger les classes
with open(NUTRITION_FILE) as f:
    nutrition_db = json.load(f)
    FOOD_CLASSES = sorted(nutrition_db.keys())

print(f"📊 {len(FOOD_CLASSES)} classes de plats")

def build_nutrition_model():
    """
    Construit le modèle complet avec deux sorties:
    1. Classification du plat (softmax)
    2. Régression des valeurs nutritionnelles
    """
    
    # Input
    inputs = keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3), name='image_input')
    
    # Base Model: EfficientNetV2B0 (pré-entraîné)
    base_model = keras.applications.EfficientNetV2B0(
        include_top=False,
        weights='imagenet',
        input_tensor=inputs,
        pooling='avg'
    )
    
    # Geler les premières couches
    base_model.trainable = True
    for layer in base_model.layers[:-50]:  # Geler toutes sauf les 50 dernières
        layer.trainable = False
    
    # Features extractor
    x = base_model.output
    
    # ========== BRANCHE 1: CLASSIFICATION ==========
    classification_branch = layers.Dense(512, activation='relu', name='class_dense1')(x)
    classification_branch = layers.Dropout(0.3)(classification_branch)
    classification_branch = layers.Dense(256, activation='relu', name='class_dense2')(classification_branch)
    classification_branch = layers.Dropout(0.2)(classification_branch)
    
    # Sortie classification
    classification_output = layers.Dense(
        NUM_CLASSES, 
        activation='softmax', 
        name='food_classification'
    )(classification_branch)
    
    # ========== BRANCHE 2: RÉGRESSION NUTRITIONNELLE ==========
    nutrition_branch = layers.Dense(512, activation='relu', name='nutr_dense1')(x)
    nutrition_branch = layers.Dropout(0.3)(nutrition_branch)
    nutrition_branch = layers.Dense(256, activation='relu', name='nutr_dense2')(nutrition_branch)
    nutrition_branch = layers.Dropout(0.2)(nutrition_branch)
    nutrition_branch = layers.Dense(128, activation='relu', name='nutr_dense3')(nutrition_branch)
    
    # Sortie nutrition (7 valeurs)
    nutrition_output = layers.Dense(
        NUM_NUTRITION_VALUES, 
        activation='linear',  # Régression
        name='nutrition_values'
    )(nutrition_branch)
    
    # Créer le modèle complet
    model = keras.Model(
        inputs=inputs,
        outputs=[classification_output, nutrition_output],
        name='NutritionAnalyzer'
    )
    
    return model

def compile_model(model):
    """Compile le modèle avec les bonnes loss functions"""
    
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss={
            'food_classification': 'categorical_crossentropy',
            'nutrition_values': 'mse'  # Mean Squared Error pour la régression
        },
        loss_weights={
            'food_classification': 1.0,
            'nutrition_values': 0.5  # Poids plus faible pour la régression
        },
        metrics={
            'food_classification': ['accuracy', 'top_k_categorical_accuracy'],
            'nutrition_values': ['mae', 'mse']  # Mean Absolute Error
        }
    )
    
    return model

def display_model_info(model):
    """Affiche les informations du modèle"""
    print("\n" + "=" * 60)
    print("🏗️  ARCHITECTURE DU MODÈLE")
    print("=" * 60)
    
    model.summary()
    
    print("\n📊 Informations:")
    print(f"   • Paramètres totaux: {model.count_params():,}")
    print(f"   • Paramètres entraînables: {sum([tf.size(w).numpy() for w in model.trainable_weights]):,}")
    print(f"   • Taille d'entrée: {IMG_SIZE}x{IMG_SIZE}x3")
    print(f"   • Classes de sortie: {NUM_CLASSES}")
    print(f"   • Valeurs nutritionnelles: {NUM_NUTRITION_VALUES}")
    
    print("\n📤 Sorties du modèle:")
    print("   1. food_classification: probabilité pour chaque classe")
    print("   2. nutrition_values: [calories, protein, fat, carbs, fiber, sugars, sodium]")

def save_model_architecture(model):
    """Sauvegarde l'architecture du modèle"""
    
    # Sauvegarder en JSON
    model_json = model.to_json()
    json_file = MODEL_DIR / "model_architecture.json"
    with open(json_file, 'w') as f:
        f.write(model_json)
    
    print(f"\n✅ Architecture sauvegardée: {json_file}")
    
    # Sauvegarder le modèle non entraîné
    model_file = MODEL_DIR / "nutrition_model_untrained.h5"
    model.save(model_file)
    
    print(f"✅ Modèle non entraîné sauvegardé: {model_file}")

def test_model_forward_pass(model):
    """Teste que le modèle fonctionne avec une entrée aléatoire"""
    print("\n🧪 Test du forward pass...")
    
    # Créer une image aléatoire
    dummy_image = tf.random.normal((1, IMG_SIZE, IMG_SIZE, 3))
    
    # Forward pass
    classification, nutrition = model(dummy_image, training=False)
    
    print(f"   ✅ Classification output shape: {classification.shape}")
    print(f"   ✅ Nutrition output shape: {nutrition.shape}")
    print(f"   ✅ Forward pass réussi!")

def main():
    print("=" * 60)
    print("🏗️  CONSTRUCTION DU MODÈLE")
    print("=" * 60)
    print()
    
    # Vérifier GPU
    gpus = tf.config.list_physical_devices('GPU')
    if gpus:
        print(f"✅ GPU détecté: {len(gpus)} GPU(s)")
        for gpu in gpus:
            print(f"   • {gpu}")
    else:
        print("⚠️  Pas de GPU détecté - utilisation du CPU")
    print()
    
    # Construire le modèle
    print("📦 Chargement d'EfficientNetV2B0 pré-entraîné...")
    model = build_nutrition_model()
    
    # Compiler
    print("⚙️  Compilation du modèle...")
    model = compile_model(model)
    
    # Afficher les infos
    display_model_info(model)
    
    # Test
    test_model_forward_pass(model)
    
    # Sauvegarder
    save_model_architecture(model)
    
    print()
    print("=" * 60)
    print("✅ MODÈLE CONSTRUIT AVEC SUCCÈS")
    print("=" * 60)
    print()
    print("Prochaine étape: python 04_train_model.py")

if __name__ == "__main__":
    main()
    