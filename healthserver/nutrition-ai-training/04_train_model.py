"""
04_train_model.py
Entraîne le modèle de nutrition sur Food-101

IMPORTANT: Exécuter sur Google Colab avec GPU pour de meilleures performances

Utilisation:
    python 04_train_model.py

Durée estimée:
    - GPU T4 (Colab gratuit): ~3-4 heures
    - CPU: ~24-48 heures (non recommandé)
"""

import tensorflow as tf
from tensorflow import keras
import numpy as np
import json
from pathlib import Path
import matplotlib.pyplot as plt
from datetime import datetime
import os

# Configuration
BASE_DIR = Path("./nutrition_ai_data")
DATA_DIR = BASE_DIR / "food-101"
NUTRITION_FILE = BASE_DIR / "nutrition_database.json"
MODEL_DIR = BASE_DIR / "models"
LOGS_DIR = BASE_DIR / "logs"

MODEL_DIR.mkdir(parents=True, exist_ok=True)
LOGS_DIR.mkdir(parents=True, exist_ok=True)

IMG_SIZE = 224
BATCH_SIZE = 32
EPOCHS = 30
LEARNING_RATE = 0.001

# Charger la base nutritionnelle
with open(NUTRITION_FILE) as f:
    NUTRITION_DB = json.load(f)

FOOD_CLASSES = sorted(NUTRITION_DB.keys())
NUM_CLASSES = len(FOOD_CLASSES)
CLASS_TO_IDX = {cls: idx for idx, cls in enumerate(FOOD_CLASSES)}

print(f"📊 {NUM_CLASSES} classes chargées")

# ==================== DATA LOADING ====================

def load_food101_split():
    """Charge les splits train/test de Food-101"""
    meta_dir = DATA_DIR / "food-101" / "meta"
    
    with open(meta_dir / "train.json") as f:
        train_data = json.load(f)
    
    with open(meta_dir / "test.json") as f:
        test_data = json.load(f)
    
    # Créer les listes de chemins
    images_dir = DATA_DIR / "food-101" / "images"
    
    train_paths = []
    train_labels = []
    
    for class_name, image_names in train_data.items():
        for img_name in image_names:
            img_path = images_dir / f"{img_name}.jpg"
            if img_path.exists():
                train_paths.append(str(img_path))
                train_labels.append(class_name)
    
    test_paths = []
    test_labels = []
    
    for class_name, image_names in test_data.items():
        for img_name in image_names:
            img_path = images_dir / f"{img_name}.jpg"
            if img_path.exists():
                test_paths.append(str(img_path))
                test_labels.append(class_name)
    
    print(f"✅ Train: {len(train_paths)} images")
    print(f"✅ Test: {len(test_paths)} images")
    
    return train_paths, train_labels, test_paths, test_labels

def get_nutrition_values(food_name):
    """Récupère les valeurs nutritionnelles pour un plat"""
    nutrition = NUTRITION_DB[food_name]
    return np.array([
        nutrition['calories'],
        nutrition['protein'],
        nutrition['fat'],
        nutrition['carbs'],
        nutrition['fiber'],
        nutrition['sugars'],
        nutrition['sodium']
    ], dtype=np.float32)

def create_dataset(image_paths, labels, is_training=True):
    """Crée un tf.data.Dataset"""
    
    def load_and_preprocess(image_path, label):
        # Charger l'image
        image = tf.io.read_file(image_path)
        image = tf.image.decode_jpeg(image, channels=3)
        image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
        
        # Data augmentation (seulement pour training)
        if is_training:
            image = tf.image.random_flip_left_right(image)
            image = tf.image.random_brightness(image, max_delta=0.2)
            image = tf.image.random_contrast(image, lower=0.8, upper=1.2)
            image = tf.image.random_saturation(image, lower=0.8, upper=1.2)
        
        # Normalisation [0, 255] -> [0, 1]
        image = image / 255.0
        
        # Label classification (one-hot)
        class_idx = CLASS_TO_IDX[label.numpy().decode('utf-8')]
        class_label = tf.one_hot(class_idx, NUM_CLASSES)
        
        # Label nutrition (valeurs continues)
        nutrition_values = tf.py_function(
            lambda x: get_nutrition_values(x.numpy().decode('utf-8')),
            [label],
            tf.float32
        )
        nutrition_values.set_shape([7])
        
        return image, {'food_classification': class_label, 'nutrition_values': nutrition_values}
    
    # Créer le dataset
    dataset = tf.data.Dataset.from_tensor_slices((image_paths, labels))
    
    if is_training:
        dataset = dataset.shuffle(buffer_size=10000)
    
    dataset = dataset.map(
        load_and_preprocess,
        num_parallel_calls=tf.data.AUTOTUNE
    )
    
    dataset = dataset.batch(BATCH_SIZE)
    dataset = dataset.prefetch(tf.data.AUTOTUNE)
    
    return dataset

# ==================== CALLBACKS ====================

def create_callbacks():
    """Crée les callbacks pour l'entraînement"""
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # ModelCheckpoint - sauvegarde le meilleur modèle
    checkpoint_path = MODEL_DIR / f"best_model_{timestamp}.h5"
    checkpoint = keras.callbacks.ModelCheckpoint(
        filepath=str(checkpoint_path),
        monitor='val_food_classification_accuracy',
        mode='max',
        save_best_only=True,
        verbose=1
    )
    
    # EarlyStopping - arrête si pas d'amélioration
    early_stop = keras.callbacks.EarlyStopping(
        monitor='val_loss',
        patience=5,
        restore_best_weights=True,
        verbose=1
    )
    
    # ReduceLROnPlateau - réduit le learning rate
    reduce_lr = keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=3,
        min_lr=1e-7,
        verbose=1
    )
    
    # TensorBoard
    log_dir = LOGS_DIR / f"tensorboard_{timestamp}"
    tensorboard = keras.callbacks.TensorBoard(
        log_dir=str(log_dir),
        histogram_freq=1
    )
    
    # CSV Logger
    csv_log = LOGS_DIR / f"training_log_{timestamp}.csv"
    csv_logger = keras.callbacks.CSVLogger(str(csv_log))
    
    return [checkpoint, early_stop, reduce_lr, tensorboard, csv_logger]

# ==================== TRAINING ====================

def train_model(model, train_dataset, val_dataset, epochs=EPOCHS):
    """Entraîne le modèle"""
    
    print("\n" + "=" * 60)
    print("🎓 DÉBUT DE L'ENTRAÎNEMENT")
    print("=" * 60)
    print()
    
    callbacks = create_callbacks()
    
    history = model.fit(
        train_dataset,
        validation_data=val_dataset,
        epochs=epochs,
        callbacks=callbacks,
        verbose=1
    )
    
    return history

def plot_training_history(history, save_path):
    """Visualise l'historique d'entraînement"""
    
    fig, axes = plt.subplots(2, 2, figsize=(15, 10))
    
    # Classification Accuracy
    axes[0, 0].plot(history.history['food_classification_accuracy'], label='Train')
    axes[0, 0].plot(history.history['val_food_classification_accuracy'], label='Val')
    axes[0, 0].set_title('Classification Accuracy')
    axes[0, 0].set_xlabel('Epoch')
    axes[0, 0].set_ylabel('Accuracy')
    axes[0, 0].legend()
    axes[0, 0].grid(True)
    
    # Classification Loss
    axes[0, 1].plot(history.history['food_classification_loss'], label='Train')
    axes[0, 1].plot(history.history['val_food_classification_loss'], label='Val')
    axes[0, 1].set_title('Classification Loss')
    axes[0, 1].set_xlabel('Epoch')
    axes[0, 1].set_ylabel('Loss')
    axes[0, 1].legend()
    axes[0, 1].grid(True)
    
    # Nutrition MAE
    axes[1, 0].plot(history.history['nutrition_values_mae'], label='Train')
    axes[1, 0].plot(history.history['val_nutrition_values_mae'], label='Val')
    axes[1, 0].set_title('Nutrition MAE')
    axes[1, 0].set_xlabel('Epoch')
    axes[1, 0].set_ylabel('MAE')
    axes[1, 0].legend()
    axes[1, 0].grid(True)
    
    # Total Loss
    axes[1, 1].plot(history.history['loss'], label='Train')
    axes[1, 1].plot(history.history['val_loss'], label='Val')
    axes[1, 1].set_title('Total Loss')
    axes[1, 1].set_xlabel('Epoch')
    axes[1, 1].set_ylabel('Loss')
    axes[1, 1].legend()
    axes[1, 1].grid(True)
    
    plt.tight_layout()
    plt.savefig(save_path)
    print(f"✅ Graphiques sauvegardés: {save_path}")
    
    plt.show()

# ==================== MAIN ====================

def main():
    print("=" * 60)
    print("🚀 ENTRAÎNEMENT DU MODÈLE DE NUTRITION")
    print("=" * 60)
    print()
    
    # Vérifier GPU
    gpus = tf.config.list_physical_devices('GPU')
    if gpus:
        print(f"✅ GPU détecté: {gpus}")
        print(f"   Utilisation du GPU pour l'entraînement")
    else:
        print("⚠️  Pas de GPU - L'entraînement sera lent!")
        print("   Conseil: Utilisez Google Colab avec GPU T4")
    print()
    
    # Charger les données
    print("📂 Chargement des données...")
    train_paths, train_labels, test_paths, test_labels = load_food101_split()
    
    # Créer une validation split (10% du train)
    from sklearn.model_selection import train_test_split
    train_paths, val_paths, train_labels, val_labels = train_test_split(
        train_paths, train_labels, test_size=0.1, random_state=42
    )
    
    print(f"📊 Train: {len(train_paths)}")
    print(f"📊 Val: {len(val_paths)}")
    print(f"📊 Test: {len(test_paths)}")
    print()
    
    # Créer les datasets
    print("🔄 Création des datasets...")
    train_dataset = create_dataset(train_paths, train_labels, is_training=True)
    val_dataset = create_dataset(val_paths, val_labels, is_training=False)
    
    print(f"✅ Datasets créés")
    print(f"   • Batch size: {BATCH_SIZE}")
    print(f"   • Steps per epoch: {len(train_paths) // BATCH_SIZE}")
    print()
    
    # Charger le modèle
    print("📦 Chargement du modèle...")
    from tensorflow.keras.models import load_model
    
    model_path = MODEL_DIR / "nutrition_model_untrained.h5"
    if not model_path.exists():
        print("❌ Modèle non trouvé!")
        print("   Exécutez d'abord: python 03_build_model.py")
        return
    
    model = load_model(model_path)
    print("✅ Modèle chargé")
    print()
    
    # Entraîner
    history = train_model(model, train_dataset, val_dataset, epochs=EPOCHS)
    
    # Sauvegarder le modèle final
    final_model_path = MODEL_DIR / "nutrition_model_final.h5"
    model.save(final_model_path)
    print(f"\n✅ Modèle final sauvegardé: {final_model_path}")
    
    # Visualiser
    plot_path = LOGS_DIR / "training_history.png"
    plot_training_history(history, plot_path)
    
    # Résumé final
    print()
    print("=" * 60)
    print("✅ ENTRAÎNEMENT TERMINÉ")
    print("=" * 60)
    print(f"📁 Modèle: {final_model_path}")
    print(f"📊 Logs: {LOGS_DIR}")
    print()
    print("Meilleure validation accuracy:", 
          max(history.history['val_food_classification_accuracy']))
    print()
    print("Prochaine étape: python 05_evaluate_model.py")

if __name__ == "__main__":
    main()