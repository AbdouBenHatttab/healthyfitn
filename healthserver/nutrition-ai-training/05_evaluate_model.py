"""
05_evaluate_model.py
Évalue le modèle entraîné sur le test set

Utilisation:
    python 05_evaluate_model.py

Durée estimée: 5-10 minutes
"""

import tensorflow as tf
from tensorflow.keras.models import load_model
import numpy as np
import json
from pathlib import Path
import matplotlib.pyplot as plt
from sklearn.metrics import confusion_matrix, classification_report
import seaborn as sns

BASE_DIR = Path("./nutrition_ai_data")
DATA_DIR = BASE_DIR / "food-101"
NUTRITION_FILE = BASE_DIR / "nutrition_database.json"
MODEL_DIR = BASE_DIR / "models"
RESULTS_DIR = BASE_DIR / "results"
RESULTS_DIR.mkdir(parents=True, exist_ok=True)

IMG_SIZE = 224
BATCH_SIZE = 32

# Charger les classes
with open(NUTRITION_FILE) as f:
    NUTRITION_DB = json.load(f)

FOOD_CLASSES = sorted(NUTRITION_DB.keys())
CLASS_TO_IDX = {cls: idx for idx, cls in enumerate(FOOD_CLASSES)}
IDX_TO_CLASS = {idx: cls for cls, idx in CLASS_TO_IDX.items()}

def load_test_data():
    """Charge les données de test"""
    meta_dir = DATA_DIR / "food-101" / "meta"
    images_dir = DATA_DIR / "food-101" / "images"
    
    with open(meta_dir / "test.json") as f:
        test_data = json.load(f)
    
    test_paths = []
    test_labels = []
    
    for class_name, image_names in test_data.items():
        for img_name in image_names:
            img_path = images_dir / f"{img_name}.jpg"
            if img_path.exists():
                test_paths.append(str(img_path))
                test_labels.append(class_name)
    
    print(f"✅ {len(test_paths)} images de test chargées")
    return test_paths, test_labels

def create_test_dataset(image_paths, labels):
    """Crée le dataset de test"""
    
    def load_and_preprocess(image_path, label):
        image = tf.io.read_file(image_path)
        image = tf.image.decode_jpeg(image, channels=3)
        image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
        image = image / 255.0
        
        class_idx = CLASS_TO_IDX[label.numpy().decode('utf-8')]
        class_label = tf.one_hot(class_idx, len(FOOD_CLASSES))
        
        nutrition = NUTRITION_DB[label.numpy().decode('utf-8')]
        nutrition_values = tf.constant([
            nutrition['calories'],
            nutrition['protein'],
            nutrition['fat'],
            nutrition['carbs'],
            nutrition['fiber'],
            nutrition['sugars'],
            nutrition['sodium']
        ], dtype=tf.float32)
        
        return image, {
            'food_classification': class_label,
            'nutrition_values': nutrition_values
        }
    
    dataset = tf.data.Dataset.from_tensor_slices((image_paths, labels))
    dataset = dataset.map(load_and_preprocess, num_parallel_calls=tf.data.AUTOTUNE)
    dataset = dataset.batch(BATCH_SIZE)
    dataset = dataset.prefetch(tf.data.AUTOTUNE)
    
    return dataset

def evaluate_model(model, test_dataset):
    """Évalue le modèle"""
    print("\n📊 Évaluation du modèle...")
    
    results = model.evaluate(test_dataset, verbose=1)
    
    print("\n" + "=" * 60)
    print("📈 RÉSULTATS D'ÉVALUATION")
    print("=" * 60)
    
    metrics_names = model.metrics_names
    for name, value in zip(metrics_names, results):
        print(f"   {name}: {value:.4f}")
    
    return dict(zip(metrics_names, results))

def detailed_classification_metrics(model, test_dataset, test_labels):
    """Calcule des métriques détaillées de classification"""
    print("\n🔍 Calcul des métriques détaillées...")
    
    # Prédictions
    y_true = []
    y_pred = []
    
    for images, labels in test_dataset:
        classification_pred, _ = model.predict(images, verbose=0)
        pred_classes = np.argmax(classification_pred, axis=1)
        true_classes = np.argmax(labels['food_classification'], axis=1)
        
        y_true.extend(true_classes)
        y_pred.extend(pred_classes)
    
    y_true = np.array(y_true)
    y_pred = np.array(y_pred)
    
    # Accuracy globale
    accuracy = np.mean(y_true == y_pred)
    
    # Top-5 accuracy
    y_true_labels = [test_labels[i] for i in range(len(test_labels))]
    
    print(f"\n✅ Accuracy: {accuracy:.4f}")
    
    # Classification report (top 10 classes pour ne pas surcharger)
    target_names = [IDX_TO_CLASS[i] for i in range(len(FOOD_CLASSES))]
    report = classification_report(
        y_true, y_pred, 
        target_names=target_names,
        output_dict=True,
        zero_division=0
    )
    
    # Sauvegarder le rapport
    report_file = RESULTS_DIR / "classification_report.json"
    with open(report_file, 'w') as f:
        json.dump(report, f, indent=2)
    
    print(f"✅ Rapport de classification sauvegardé: {report_file}")
    
    return y_true, y_pred, report

def plot_confusion_matrix(y_true, y_pred, class_names, save_path):
    """Affiche et sauvegarde la matrice de confusion (échantillon)"""
    print("\n📊 Génération de la matrice de confusion...")
    
    # Matrice de confusion pour un échantillon de classes
    sample_classes = list(range(0, len(class_names), len(class_names)//20))  # 20 classes
    
    mask = np.isin(y_true, sample_classes) & np.isin(y_pred, sample_classes)
    y_true_sample = y_true[mask]
    y_pred_sample = y_pred[mask]
    
    cm = confusion_matrix(y_true_sample, y_pred_sample, labels=sample_classes)
    
    plt.figure(figsize=(15, 12))
    sns.heatmap(
        cm, 
        annot=True, 
        fmt='d', 
        cmap='Blues',
        xticklabels=[class_names[i] for i in sample_classes],
        yticklabels=[class_names[i] for i in sample_classes]
    )
    plt.title('Matrice de Confusion (échantillon de 20 classes)')
    plt.ylabel('Vraie Classe')
    plt.xlabel('Classe Prédite')
    plt.xticks(rotation=90)
    plt.yticks(rotation=0)
    plt.tight_layout()
    plt.savefig(save_path, dpi=150)
    
    print(f"✅ Matrice de confusion sauvegardée: {save_path}")

def evaluate_nutrition_predictions(model, test_dataset):
    """Évalue les prédictions nutritionnelles"""
    print("\n🍎 Évaluation des prédictions nutritionnelles...")
    
    nutrition_names = ['calories', 'protein', 'fat', 'carbs', 'fiber', 'sugars', 'sodium']
    
    y_true_nutrition = []
    y_pred_nutrition = []
    
    for images, labels in test_dataset:
        _, nutrition_pred = model.predict(images, verbose=0)
        
        y_true_nutrition.append(labels['nutrition_values'].numpy())
        y_pred_nutrition.append(nutrition_pred)
    
    y_true_nutrition = np.vstack(y_true_nutrition)
    y_pred_nutrition = np.vstack(y_pred_nutrition)
    
    # Calculer MAE et MAPE pour chaque nutriment
    results = {}
    
    print("\n📊 Erreurs par nutriment:")
    print("-" * 60)
    
    for i, name in enumerate(nutrition_names):
        mae = np.mean(np.abs(y_true_nutrition[:, i] - y_pred_nutrition[:, i]))
        mape = np.mean(np.abs((y_true_nutrition[:, i] - y_pred_nutrition[:, i]) / 
                              (y_true_nutrition[:, i] + 1e-8))) * 100
        
        results[name] = {'MAE': mae, 'MAPE': mape}
        print(f"   {name:15s}: MAE={mae:8.2f}  MAPE={mape:6.2f}%")
    
    # Visualisation
    fig, axes = plt.subplots(2, 4, figsize=(20, 10))
    axes = axes.flatten()
    
    for i, name in enumerate(nutrition_names):
        axes[i].scatter(
            y_true_nutrition[:, i], 
            y_pred_nutrition[:, i], 
            alpha=0.3, s=1
        )
        axes[i].plot(
            [y_true_nutrition[:, i].min(), y_true_nutrition[:, i].max()],
            [y_true_nutrition[:, i].min(), y_true_nutrition[:, i].max()],
            'r--', lw=2
        )
        axes[i].set_xlabel('Valeur Réelle')
        axes[i].set_ylabel('Valeur Prédite')
        axes[i].set_title(f'{name}\nMAE={results[name]["MAE"]:.2f}')
        axes[i].grid(True, alpha=0.3)
    
    # Supprimer le 8e subplot vide
    fig.delaxes(axes[7])
    
    plt.tight_layout()
    plot_path = RESULTS_DIR / "nutrition_predictions.png"
    plt.savefig(plot_path, dpi=150)
    
    print(f"\n✅ Graphique des prédictions nutritionnelles: {plot_path}")
    
    return results

def main():
    print("=" * 60)
    print("📊 ÉVALUATION DU MODÈLE")
    print("=" * 60)
    print()
    
    # Charger le modèle
    model_path = MODEL_DIR / "nutrition_model_final.h5"
    if not model_path.exists():
        print(f"❌ Modèle non trouvé: {model_path}")
        print("   Exécutez d'abord: python 04_train_model.py")
        return
    
    print(f"📦 Chargement du modèle: {model_path}")
    model = load_model(model_path)
    print("✅ Modèle chargé")
    
    # Charger les données de test
    test_paths, test_labels = load_test_data()
    test_dataset = create_test_dataset(test_paths, test_labels)
    
    # Évaluation globale
    metrics = evaluate_model(model, test_dataset)
    
    # Métriques détaillées de classification
    y_true, y_pred, report = detailed_classification_metrics(
        model, test_dataset, test_labels
    )
    
    # Matrice de confusion
    cm_path = RESULTS_DIR / "confusion_matrix.png"
    plot_confusion_matrix(y_true, y_pred, FOOD_CLASSES, cm_path)
    
    # Évaluation nutrition
    nutrition_results = evaluate_nutrition_predictions(model, test_dataset)
    
    # Sauvegarder tous les résultats
    all_results = {
        'global_metrics': metrics,
        'classification_report': report,
        'nutrition_metrics': nutrition_results
    }
    
    results_file = RESULTS_DIR / "evaluation_results.json"
    with open(results_file, 'w') as f:
        json.dump(all_results, f, indent=2)
    
    print()
    print("=" * 60)
    print("✅ ÉVALUATION TERMINÉE")
    print("=" * 60)
    print(f"📁 Résultats dans: {RESULTS_DIR}")
    print()
    print("Prochaine étape: python 06_inference.py")

if __name__ == "__main__":
    main()