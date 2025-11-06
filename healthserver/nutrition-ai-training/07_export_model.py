"""
07_export_model.py
Exporte le modèle pour différents formats de production

Formats supportés:
- TensorFlow SavedModel (pour TF Serving)
- TensorFlow Lite (pour mobile)
- ONNX (pour interopérabilité)
- TensorFlow.js (pour web)

Utilisation:
    python 07_export_model.py --format all
"""

import tensorflow as tf
from tensorflow.keras.models import load_model
import json
from pathlib import Path
import argparse
import numpy as np

BASE_DIR = Path("./nutrition_ai_data")
MODEL_DIR = BASE_DIR / "models"
EXPORT_DIR = BASE_DIR / "exports"
NUTRITION_FILE = BASE_DIR / "nutrition_database.json"

EXPORT_DIR.mkdir(parents=True, exist_ok=True)

IMG_SIZE = 224

with open(NUTRITION_FILE) as f:
    NUTRITION_DB = json.load(f)
FOOD_CLASSES = sorted(NUTRITION_DB.keys())

def export_savedmodel(model, export_path):
    """Exporte en TensorFlow SavedModel format"""
    print("\n📦 Export en SavedModel...")
    
    model.save(export_path, save_format='tf')
    
    print(f"✅ SavedModel exporté: {export_path}")
    print(f"   Taille: {sum(f.stat().st_size for f in export_path.rglob('*') if f.is_file()) / 1024 / 1024:.2f} MB")

def export_tflite(model, export_path):
    """Exporte en TensorFlow Lite"""
    print("\n📱 Export en TensorFlow Lite...")
    
    # Convertir en TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Optimisations
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Conversion
    tflite_model = converter.convert()
    
    # Sauvegarder
    with open(export_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ TFLite exporté: {export_path}")
    print(f"   Taille: {len(tflite_model) / 1024 / 1024:.2f} MB")
    
    # Créer le fichier de métadonnées
    metadata = {
        'input_shape': [1, IMG_SIZE, IMG_SIZE, 3],
        'outputs': {
            'classification': {
                'shape': [1, len(FOOD_CLASSES)],
                'classes': FOOD_CLASSES
            },
            'nutrition': {
                'shape': [1, 7],
                'labels': ['calories', 'protein', 'fat', 'carbs', 'fiber', 'sugars', 'sodium']
            }
        }
    }
    
    metadata_path = export_path.parent / f"{export_path.stem}_metadata.json"
    with open(metadata_path, 'w') as f:
        json.dump(metadata, f, indent=2)
    
    print(f"✅ Métadonnées: {metadata_path}")

def export_onnx(model, export_path):
    """Exporte en ONNX format"""
    print("\n🔄 Export en ONNX...")
    
    try:
        import tf2onnx
        import onnx
        
        # Convertir
        spec = (tf.TensorSpec((None, IMG_SIZE, IMG_SIZE, 3), tf.float32, name="input"),)
        output_path = str(export_path)
        
        model_proto, _ = tf2onnx.convert.from_keras(
            model,
            input_signature=spec,
            opset=13,
            output_path=output_path
        )
        
        print(f"✅ ONNX exporté: {export_path}")
        print(f"   Taille: {export_path.stat().st_size / 1024 / 1024:.2f} MB")
        
    except ImportError:
        print("⚠️  tf2onnx non installé. Installez avec: pip install tf2onnx onnx")
        print("   Export ONNX ignoré.")

def export_tfjs(model, export_path):
    """Exporte en TensorFlow.js format"""
    print("\n🌐 Export en TensorFlow.js...")
    
    try:
        import tensorflowjs as tfjs
        
        tfjs.converters.save_keras_model(model, str(export_path))
        
        print(f"✅ TensorFlow.js exporté: {export_path}")
        
        # Créer un exemple HTML
        html_example = """<!DOCTYPE html>
<html>
<head>
    <title>Nutrition Analyzer</title>
    <script src="https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@latest"></script>
</head>
<body>
    <h1>🍕 Nutrition Analyzer</h1>
    <input type="file" id="imageInput" accept="image/*">
    <br><br>
    <img id="preview" style="max-width: 400px;">
    <br><br>
    <div id="results"></div>

    <script>
        let model;
        
        async function loadModel() {
            model = await tf.loadLayersModel('./model.json');
            console.log('Model loaded');
        }
        
        async function predict(imageElement) {
            const tensor = tf.browser.fromPixels(imageElement)
                .resizeNearestNeighbor([224, 224])
                .toFloat()
                .div(255.0)
                .expandDims(0);
            
            const [classification, nutrition] = await model.predict(tensor).array();
            
            // Afficher les résultats
            const topClass = classification[0].indexOf(Math.max(...classification[0]));
            const nutritionValues = nutrition[0];
            
            document.getElementById('results').innerHTML = `
                <h2>Résultats:</h2>
                <p>Classe: ${topClass}</p>
                <p>Calories: ${nutritionValues[0].toFixed(1)} kcal</p>
                <p>Protéines: ${nutritionValues[1].toFixed(1)} g</p>
                <p>Lipides: ${nutritionValues[2].toFixed(1)} g</p>
                <p>Glucides: ${nutritionValues[3].toFixed(1)} g</p>
            `;
        }
        
        document.getElementById('imageInput').addEventListener('change', function(e) {
            const file = e.target.files[0];
            const reader = new FileReader();
            
            reader.onload = function(event) {
                const img = document.getElementById('preview');
                img.src = event.target.result;
                img.onload = () => predict(img);
            };
            
            reader.readAsDataURL(file);
        });
        
        loadModel();
    </script>
</body>
</html>"""
        
        html_path = export_path / "index.html"
        with open(html_path, 'w') as f:
            f.write(html_example)
        
        print(f"✅ Exemple HTML: {html_path}")
        
    except ImportError:
        print("⚠️  tensorflowjs non installé. Installez avec: pip install tensorflowjs")
        print("   Export TensorFlow.js ignoré.")

def create_inference_example():
    """Crée un exemple de code d'inférence"""
    example_code = '''"""
Exemple d'utilisation du modèle exporté
"""

import tensorflow as tf
import numpy as np
from PIL import Image

# Charger le modèle
model = tf.keras.models.load_model('path/to/model')

# Charger et prétraiter l'image
img = Image.open('food.jpg').resize((224, 224))
img_array = np.array(img) / 255.0
img_array = np.expand_dims(img_array, axis=0)

# Prédiction
classification, nutrition = model.predict(img_array)

# Résultats
food_classes = [...]  # Charger depuis metadata
predicted_class = food_classes[np.argmax(classification[0])]

print(f"Plat: {predicted_class}")
print(f"Calories: {nutrition[0][0]:.1f} kcal")
print(f"Protéines: {nutrition[0][1]:.1f} g")
print(f"Lipides: {nutrition[0][2]:.1f} g")
print(f"Glucides: {nutrition[0][3]:.1f} g")
'''
    
    example_path = EXPORT_DIR / "inference_example.py"
    with open(example_path, 'w') as f:
        f.write(example_code)
    
    print(f"\n📝 Exemple d'inférence créé: {example_path}")

def main():
    parser = argparse.ArgumentParser(description='Exporte le modèle')
    parser.add_argument('--format', type=str, default='all',
                       choices=['all', 'savedmodel', 'tflite', 'onnx', 'tfjs'],
                       help='Format d\'export')
    parser.add_argument('--model', type=str, default=None,
                       help='Chemin du modèle à exporter')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("📦 EXPORT DU MODÈLE")
    print("=" * 60)
    
    # Charger le modèle
    if args.model:
        model_path = Path(args.model)
    else:
        model_path = MODEL_DIR / "nutrition_model_final.h5"
    
    if not model_path.exists():
        print(f"❌ Modèle non trouvé: {model_path}")
        return
    
    print(f"\n📦 Chargement du modèle: {model_path}")
    model = load_model(model_path)
    print("✅ Modèle chargé")
    
    # Exporter selon le format
    if args.format in ['all', 'savedmodel']:
        savedmodel_path = EXPORT_DIR / "savedmodel"
        export_savedmodel(model, savedmodel_path)
    
    if args.format in ['all', 'tflite']:
        tflite_path = EXPORT_DIR / "nutrition_model.tflite"
        export_tflite(model, tflite_path)
    
    if args.format in ['all', 'onnx']:
        onnx_path = EXPORT_DIR / "nutrition_model.onnx"
        export_onnx(model, onnx_path)
    
    if args.format in ['all', 'tfjs']:
        tfjs_path = EXPORT_DIR / "tfjs_model"
        export_tfjs(model, tfjs_path)
    
    # Créer l'exemple d'inférence
    create_inference_example()
    
    print()
    print("=" * 60)
    print("✅ EXPORT TERMINÉ")
    print("=" * 60)
    print(f"📁 Modèles exportés dans: {EXPORT_DIR}")
    print()
    print("Formats disponibles:")
    print("   • SavedModel: Pour TensorFlow Serving")
    print("   • TFLite: Pour applications mobiles (Android/iOS)")
    print("   • ONNX: Pour interopérabilité (PyTorch, etc.)")
    print("   • TensorFlow.js: Pour applications web")

if __name__ == "__main__":
    main()