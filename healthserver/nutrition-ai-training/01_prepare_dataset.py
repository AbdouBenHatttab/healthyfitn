"""
01_prepare_dataset.py
Télécharge et prépare le dataset Food-101

Utilisation:
    python 01_prepare_dataset.py

Durée estimée: 5-10 minutes
"""

import os
import requests
import tarfile
from pathlib import Path
from tqdm import tqdm
import json

# Configuration
BASE_DIR = Path("./nutrition_ai_data")
DATA_DIR = BASE_DIR / "food-101"
DATA_DIR.mkdir(parents=True, exist_ok=True)

FOOD101_URL = "http://data.vision.ee.ethz.ch/cvl/food-101.tar.gz"
FOOD101_TAR = BASE_DIR / "food-101.tar.gz"

def download_file(url, destination):
    """Télécharge un fichier avec barre de progression"""
    print(f"📥 Téléchargement de {url}")
    
    response = requests.get(url, stream=True)
    total_size = int(response.headers.get('content-length', 0))
    
    with open(destination, 'wb') as file, tqdm(
        desc=destination.name,
        total=total_size,
        unit='B',
        unit_scale=True,
        unit_divisor=1024,
    ) as progress_bar:
        for chunk in response.iter_content(chunk_size=8192):
            size = file.write(chunk)
            progress_bar.update(size)
    
    print(f"✅ Téléchargement terminé: {destination}")

def extract_tar(tar_path, extract_to):
    """Extrait une archive tar.gz"""
    print(f"📦 Extraction de {tar_path.name}...")
    
    with tarfile.open(tar_path, 'r:gz') as tar:
        # Obtenir le nombre total de fichiers
        members = tar.getmembers()
        
        with tqdm(total=len(members), desc="Extraction") as progress_bar:
            for member in members:
                tar.extract(member, path=extract_to)
                progress_bar.update(1)
    
    print(f"✅ Extraction terminée dans: {extract_to}")

def verify_dataset():
    """Vérifie que le dataset est complet"""
    images_dir = BASE_DIR / "food-101" / "images"
    meta_dir = BASE_DIR / "food-101" / "meta"
    
    if not images_dir.exists():
        print("❌ Dossier images non trouvé")
        return False
    
    if not meta_dir.exists():
        print("❌ Dossier meta non trouvé")
        return False
    
    # Compter les classes
    classes = [d for d in images_dir.iterdir() if d.is_dir()]
    print(f"📊 {len(classes)} classes trouvées")
    
    # Compter les images totales
    total_images = sum(1 for _ in images_dir.rglob("*.jpg"))
    print(f"🖼️  {total_images} images trouvées")
    
    # Vérifier les fichiers meta
    train_json = meta_dir / "train.json"
    test_json = meta_dir / "test.json"
    
    if train_json.exists() and test_json.exists():
        with open(train_json) as f:
            train_data = json.load(f)
        with open(test_json) as f:
            test_data = json.load(f)
        
        n_train = sum(len(v) for v in train_data.values())
        n_test = sum(len(v) for v in test_data.values())
        
        print(f"📈 {n_train} images d'entraînement")
        print(f"📉 {n_test} images de test")
    
    return True

def main():
    print("=" * 60)
    print("🍕 PRÉPARATION DU DATASET FOOD-101")
    print("=" * 60)
    print()
    
    # Vérifier si déjà téléchargé
    if (BASE_DIR / "food-101" / "images").exists():
        print("⚠️  Dataset déjà présent. Vérification...")
        if verify_dataset():
            print("✅ Dataset valide. Aucun téléchargement nécessaire.")
            return
    
    # Télécharger
    if not FOOD101_TAR.exists():
        download_file(FOOD101_URL, FOOD101_TAR)
    else:
        print(f"⚠️  Archive déjà téléchargée: {FOOD101_TAR}")
    
    # Extraire
    extract_tar(FOOD101_TAR, BASE_DIR)
    
    # Vérifier
    print()
    verify_dataset()
    
    print()
    print("=" * 60)
    print("✅ PRÉPARATION TERMINÉE")
    print("=" * 60)
    print(f"📁 Données dans: {BASE_DIR}")
    print()
    print("Prochaine étape: python 02_create_nutrition_db.py")

if __name__ == "__main__":
    main()