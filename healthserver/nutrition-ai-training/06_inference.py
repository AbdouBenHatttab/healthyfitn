"""
06_inference.py
Teste le modèle sur une nouvelle image

Utilisation:
    python 06_inference.py --image path/to/image.jpg

Exemple:
    python 06_inference.py --image test_pizza.jpg
"""

import tensorflow as tf
from tensorflow.keras.models import load_model
import numpy as np
import json
from pathlib import Path
import argparse
from PIL import Image
import matplotlib.pyplot as plt

BASE_DIR = Path("./nutrition_ai_data")
NUTRITION_FILE = BASE_DIR / "nutrition_database.json"
MODEL_DIR = BASE_DIR / "models"

IMG_SIZE = 224

# Charger les classes
with open(NUTRITION_FILE) as f:
    NUTRITION_DB = json.load(f)

FOOD_CLASSES = sorted(NUTRITION_DB.keys())
IDX_TO_CLASS = {idx: cls for idx, cls in enumerate(FOOD_CLASSES)}

NUTRITION_NAMES = ['calories', 'protein', 'fat', 'carbs', 'fiber', 'sugars', 'sodium']

def load_and_preprocess_image(image_path):
    """Charge et prétraite une image pour l'inférence"""
    # Charger l'image
    img = Image.open(image_path).convert('RGB')
    
    # Sauvegarder la taille originale pour l'affichage
    original_img = img.copy()
    
    # Redimensionner
    img = img.resize((IMG_SIZE, IMG_SIZE))
    
    # Convertir en array et normaliser
    img_array = np.array(img) / 255.0
    
    # Ajouter la dimension batch
    img_array = np.expand_dims(img_array, axis=0)
    
    return img_array, original_img

def predict(model, image_array):
    """Fait une prédiction"""
    classification_pred, nutrition_pred = model.predict(image_array, verbose=0)
    
    # Top 5 prédictions de classe
    top5_idx = np.argsort(classification_pred[0])[::-1][:5]
    top5_probs = classification_pred[0][top5_idx]
    top5_classes = [IDX_TO_CLASS[idx] for idx in top5_idx]
    
    # Valeurs nutritionnelles prédites
    nutrition_values = nutrition_pred[0]
    
    return top5_classes, top5_probs, nutrition_values

def format_nutrition_output(nutrition_values):
    """Formate la sortie nutritionnelle en JSON"""
    return {
        'calories': round(float(nutrition_values[0]), 1),
        'protein_g': round(float(nutrition_values[1]), 1),
        'fat_g': round(float(nutrition_values[2]), 1),
        'carbs_g': round(float(nutrition_values[3]), 1),
        'fiber_g': round(float(nutrition_values[4]), 1),
        'sugars_g': round(float(nutrition_values[5]), 1),
        'sodium_mg': round(float(nutrition_values[6]), 1)
    }

def display_results(original_img, top5_classes, top5_probs, nutrition_values):
    """Affiche les résultats visuellement"""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
    
    # Image originale
    ax1.imshow(original_img)
    ax1.axis('off')
    ax1.set_title(f'Prediction: {top5_classes[0].replace("_", " ").title()}\n'
                  f'Confidence: {top5_probs[0]*100:.1f}%', 
                  fontsize=14, fontweight='bold')
    
    # Graphique des prédictions
    y_pos = np.arange(len(top5_classes))
    ax2.barh(y_pos, top5_probs, color='skyblue')
    ax2.set_yticks(y_pos)
    ax2.set_yticklabels([cls.replace('_', ' ').title() for cls in top5_classes])
    ax2.set_xlabel('Probability')
    ax2.set_title('Top 5 Predictions')
    ax2.set_xlim([0, 1])
    
    # Ajouter les pourcentages
    for i, v in enumerate(top5_probs):
        ax2.text(v + 0.01, i, f'{v*100:.1f}%', va='center')
    
    plt.tight_layout()
    plt.show()
    
    # Afficher les valeurs nutritionnelles
    nutrition = format_nutrition_output(nutrition_values)
    
    print("\n" + "=" * 60)
    print("🍽️  RÉSULTATS DE L'ANALYSE NUTRITIONNELLE")
    print("=" * 60)
    print(f"\n📊 Plat détecté: {top5_classes[0].replace('_', ' ').title()}")
    print(f"✅ Confiance: {top5_probs[0]*100:.1f}%")
    print("\n📈 Valeurs nutritionnelles (pour 100g ou portion standard):")
    print("-" * 60)
    print(f"   Calories:      {nutrition['calories']:>8.1f} kcal")
    print(f"   Protéines:     {nutrition['protein_g']:>8.1f} g")
    print(f"   Lipides:       {nutrition['fat_g']:>8.1f} g")
    print(f"   Glucides:      {nutrition['carbs_g']:>8.1f} g")
    print(f"   Fibres:        {nutrition['fiber_g']:>8.1f} g")
    print(f"   Sucres:        {nutrition['sugars_g']:>8.1f} g")
    print(f"   Sodium:        {nutrition['sodium_mg']:>8.1f} mg")
    print("=" * 60)
    
    # Format JSON
    result_json = {
        'food': top5_classes[0],
        'confidence': round(float(top5_probs[0]), 3),
        'nutrition': nutrition,
        'top5_predictions': [
            {
                'food': cls,
                'confidence': round(float(prob), 3)
            }
            for cls, prob in zip(top5_classes, top5_probs)
        ]
    }
    
    print("\n📋 Format JSON:")
    print(json.dumps(result_json, indent=2))
    
    return result_json

def main():
    parser = argparse.ArgumentParser(description='Analyse nutritionnelle d\'une image')
    parser.add_argument('--image', type=str, required=True, help='Chemin vers l\'image')
    parser.add_argument('--model', type=str, default=None, help='Chemin vers le modèle (optionnel)')
    parser.add_argument('--save', type=str, default=None, help='Sauvegarder le résultat JSON')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("🔮 ANALYSE NUTRITIONNELLE D'IMAGE")
    print("=" * 60)
    print()
    
    # Vérifier l'image
    image_path = Path(args.image)
    if not image_path.exists():
        print(f"❌ Image non trouvée: {image_path}")
        return
    
    print(f"📸 Image: {image_path}")
    
    # Charger le modèle
    if args.model:
        model_path = Path(args.model)
    else:
        model_path = MODEL_DIR / "nutrition_model_final.h5"
    
    if not model_path.exists():
        print(f"❌ Modèle non trouvé: {model_path}")
        print("   Exécutez d'abord: python 04_train_model.py")
        return
    
    print(f"📦 Chargement du modèle: {model_path}")
    model = load_model(model_path)
    print("✅ Modèle chargé\n")
    
    # Prétraiter l'image
    print("🔄 Prétraitement de l'image...")
    image_array, original_img = load_and_preprocess_image(image_path)
    
    # Prédiction
    print("🤖 Analyse en cours...")
    top5_classes, top5_probs, nutrition_values = predict(model, image_array)
    
    # Afficher les résultats
    result = display_results(original_img, top5_classes, top5_probs, nutrition_values)
    
    # Sauvegarder si demandé
    if args.save:
        save_path = Path(args.save)
        with open(save_path, 'w') as f:
            json.dump(result, f, indent=2)
        print(f"\n💾 Résultat sauvegardé: {save_path}")
    
    print()

if __name__ == "__main__":
    main()