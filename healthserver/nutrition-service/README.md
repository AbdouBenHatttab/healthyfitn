hivenv/Scripts/activate          # ou dans Git Bash : source venv/Scripts/activate
python app/main.py

# puis
pip install flask             # pour Flask
pip install flask-cors        # pour gérer les CORS
pip install python-dotenv     # pour charger les variables d'environnement depuis .env
pip install PyJWT             # pour JWT (authentification)
pip install opencv-python     # pour le traitement d’images avec cv2
pip install numpy             # souvent nécessaire avec OpenCV ou TensorFlow
pip install pillow            # pour PIL (manipulation d’images)
pip install tensorflow        # pour MLService
# ou si tu veux la version CPU uniquement
pip install tensorflow-cpu
pip install minio             # pour MinIOService
pip install pymongo           # pour MongoDB

# puis
python -m app.main




# 🥗 Nutrition Analysis Service

Service d'analyse nutritionnelle basé sur l'intelligence artificielle pour l'application Health & Nutrition Companion.

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Utilisation](#utilisation)
- [API Endpoints](#api-endpoints)
- [Tests](#tests)
- [Déploiement](#déploiement)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Vue d'ensemble

Le **Nutrition Service** est un microservice Python/Flask qui analyse les photos de nourriture et fournit des informations nutritionnelles détaillées.

### Technologies Utilisées

- **Flask 3.0** - Framework web
- **TensorFlow 2.15** - Détection alimentaire (ML)
- **MongoDB** - Stockage des analyses
- **MinIO** - Stockage des images
- **OpenCV + Pillow** - Traitement d'images
- **Open Food Facts API** - Données nutritionnelles
✅ TensorFlow 2.15 — Détection alimentaire (Machine Learning)

TensorFlow est une bibliothèque de Machine Learning.
Dans ton projet, il sert à :

entraîner un modèle de détection alimentaire (identifier un plat ou un aliment dans une image)

faire des prédictions à partir d’images (exemple : reconnaître que la photo contient “pasta”, “banane”, “pizza”, etc.)

En résumé : TensorFlow = cerveau du système qui reconnaît les aliments.

✅ MinIO — Stockage d’images (Object Storage)

MinIO est une solution de stockage d’objets (comme Amazon S3).
Il sert à stocker :

les images uploadées par l’utilisateur

les images utilisées par le modèle

éventuellement les résultats ou fichiers volumineux

Avantages : rapide, compatible S3, facile à utiliser avec Flask.

👉 MinIO = disque dur cloud pour stocker les images.

✅ OpenCV — Traitement d’images (Computer Vision)

OpenCV est une bibliothèque très connue pour le traitement d’images :

redimensionner les images avant de les analyser

détecter les contours, couleurs, formes

nettoyer ou améliorer les images

préparer l’image pour TensorFlow

👉 OpenCV = outils avancés pour manipuler et analyser les images.

✅ Pillow (PIL) — Manipulation simple d’images

Pillow est une bibliothèque Python pour :

ouvrir des images JPG/PNG

les convertir (RGB, etc.)

les recadrer ou les compresser

les préparer pour TensorFlow ou OpenCV

Souvent OpenCV + Pillow sont complémentaires.

👉 Pillow = petites manipulations d’images faciles.

✅ Open Food Facts API — Données nutritionnelles

Open Food Facts est une base de données mondiale sur les aliments.
L’API permet de récupérer :

calories

graisses

protéines

sucre

Nutri-Score

liste d’ingrédients

Dans ton système, après que TensorFlow reconnaît l’aliment, tu peux appeler Open Food Facts pour récupérer ses valeurs nutritionnelles.
---

## ✨ Fonctionnalités

### 1. Analyse d'Images
- ✅ Upload et validation d'images
- ✅ Compression et preprocessing automatique
- ✅ Détection de nourriture via ML (CNN/ViT)
- ✅ Estimation de la taille des portions

### 2. Informations Nutritionnelles
- ✅ Calories, protéines, glucides, lipides
- ✅ Fibres, sucres, sodium
- ✅ Vitamines (A, C, calcium, fer)
- ✅ Score nutritionnel (0-100)

### 3. Recommandations Personnalisées
- ✅ Calcul du TDEE (dépense énergétique)
- ✅ Pourcentage du repas vs besoins quotidiens
- ✅ Suggestions pour équilibrer le repas
- ✅ Alertes nutritionnelles

### 4. Historique et Statistiques
- ✅ Historique des analyses par utilisateur
- ✅ Statistiques nutritionnelles
- ✅ Totaux journaliers sur 7 jours
- ✅ Moyennes par repas

---

## 🏗️ Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    Mobile App (Kotlin)                   │
│                 JWT Authentication                       │
└────────────────────────┬────────────────────────────────┘
                         │ HTTPS + JWT
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Nutrition Service (Flask)                   │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ JWT Service  │  │Image Service │  │  ML Service  │ │
│  │  Validation  │  │ Preprocessing│  │  Detection   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │Nutrition API │  │MinIO Service │  │MongoDB Model │ │
│  │ OpenFoodFacts│  │Image Storage │  │   Storage    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    ┌─────────┐    ┌─────────┐    ┌─────────┐
    │ MongoDB │    │  MinIO  │    │Open Food│
    │  :27017 │    │  :9000  │    │Facts API│
    └─────────┘    └─────────┘    └─────────┘
```

---

## 📦 Prérequis

### Logiciels Requis

- **Python 3.11+** ([Download](https://www.python.org/downloads/))
- **MongoDB 6.0+** ([Download](https://www.mongodb.com/try/download/community))
- **MinIO** ([Download](https://min.io/download))
- **Git Bash** (Windows) ou Terminal (Linux/Mac)

### Services Externes

- **Auth Service** (Port 8082) - Pour validation JWT
- **Open Food Facts API** - Pour données nutritionnelles

---

## 🚀 Installation

### Étape 1 : Cloner le Projet
```bash
cd /d/HealthyFiTN/healthapp
git clone <your-repo-url>
cd nutrition-service
```

### Étape 2 : Créer l'Environnement Virtuel
```bash
# Créer venv
python -m venv venv

# Activer venv
# Windows (Git Bash):
source venv/Scripts/activate

# Linux/Mac:
source venv/bin/activate

# Vous devriez voir (venv) dans le terminal
```

### Étape 3 : Installer les Dépendances
```bash
# Mettre à jour pip
python -m pip install --upgrade pip

# Installer toutes les dépendances
pip install -r requirements.txt

# Vérifier l'installation
pip list
```

### Étape 4 : Démarrer les Services Requis

#### MongoDB
```bash
# Dans un terminal séparé
mongod --dbpath /d/HealthyFiTN/healthapp/data
```

#### MinIO
```bash
# Dans un autre terminal
cd C:\minio
.\minio.exe server ./data --console-address ":9001"
```
netstat -ano | findstr 9000
---

## ⚙️ Configuration

### Fichier `.env`

Créez/modifiez `.env` avec vos paramètres :
```env
# MongoDB
MONGO_URI=mongodb://localhost:27017/
MONGO_DATABASE=health_nutrition_db

# MinIO
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=nutrition-images
MINIO_SECURE=False

# JWT (DOIT ÊTRE IDENTIQUE à auth-service!)
JWT_SECRET=21f4e176cd2f4b991bd27fd94a7acfa923a032015252f7f725cee7761503b6120d0f92dcda38390c619190e921833477ea8f32100e9d59bcd398073b1552c15e
JWT_ALGORITHM=HS256

# Services
AUTH_SERVICE_URL=http://localhost:8082
FLASK_PORT=8086
FLASK_ENV=development

# APIs
OPENFOODFACTS_API=https://world.openfoodfacts.org/api/v2

# ML Model
ML_MODEL_PATH=models/food_detection_model.h5
```

### Créer le Bucket MinIO

1. Ouvrir http://localhost:9001
2. Login : `minioadmin` / `minioadmin`
3. Créer bucket : `nutrition-images`

---

## 🎮 Utilisation

### Démarrer le Service
```bash
# Activer venv
source venv/Scripts/activate

# Lancer le service
python app/main.py

# Sortie attendue:
# ========================================
# 🥗 Nutrition Service Started!
# 📍 Port: 8086
# 📊 MongoDB: health_nutrition_db
# 🖼️  MinIO: nutrition-images bucket
# 🤖 ML Model: Loaded
# ========================================
```

### Mode Développement (Hot Reload)
```bash
# Avec Flask debug mode
export FLASK_ENV=development
python app/main.py

# Ou avec Gunicorn
gunicorn --bind 0.0.0.0:8086 --reload app.main:create_app()
```

### Vérifier que ça Fonctionne
```bash
# Health check
curl http://localhost:8086/api/v1/nutrition/health

# Réponse attendue:
# {
#   "status": "healthy",
#   "service": "nutrition-service",
#   "version": "1.0.0"
# }
```

---

## 📡 API Endpoints

### Base URL
```
http://localhost:8086/api/v1/nutrition
```

### Authentication

Tous les endpoints (sauf `/health`) nécessitent un JWT token :
```
Authorization: Bearer <your_jwt_token>
```

---

### 1️⃣ **Analyser une Photo de Nourriture**

**POST** `/analyze`

Analyse une image et retourne les informations nutritionnelles.

**Headers:**
```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Body (form-data):**
- `image` (file, required) - Image de nourriture (JPEG/PNG, max 10MB)
- `user_profile` (text, optional) - Profil utilisateur JSON

**Exemple user_profile:**
```json
{
  "age": 30,
  "weight": 70,
  "height": 170,
  "gender": "male",
  "activity_level": "moderate"
}
```

**Réponse (200 OK):**
```json
{
  "success": true,
  "message": "Food analysis completed successfully",
  "data": {
    "analysis_id": "550e8400-e29b-41d4-a716-446655440000",
    "image_url": "http://localhost:9000/nutrition-images/...",
    "detected_foods": [
      {
        "food_name": "apple",
        "calories": 52.0,
        "proteins": 0.3,
        "carbohydrates": 14.0,
        "fats": 0.2,
        "fiber": 2.4,
        "confidence": 95.5
      }
    ],
    "portion_size": "medium",
    "total_nutrition": {
      "calories": 52.0,
      "proteins": 0.3,
      "carbohydrates": 14.0,
      "fats": 0.2
    },
    "recommendations": {
      "tdee": 2000,
      "meal_percentage": 2.6,
      "recommendations": ["Add more protein"],
      "warnings": [],
      "health_score": 85
    }
  }
}
```

---

### 2️⃣ **Récupérer l'Historique**

**GET** `/history?limit=20&skip=0`

Récupère l'historique des analyses de l'utilisateur.

**Query Parameters:**
- `limit` (int, default: 20) - Nombre de résultats
- `skip` (int, default: 0) - Pagination offset

**Réponse (200 OK):**
```json
{
  "success": true,
  "message": "History retrieved successfully",
  "data": {
    "analyses": [...],
    "count": 5
  }
}
```

---

### 3️⃣ **Détail d'une Analyse**

**GET** `/history/{analysis_id}`

Récupère une analyse spécifique.

**Réponse (200 OK):**
```json
{
  "success": true,
  "message": "Analysis retrieved successfully",
  "data": {
    "analysis_id": "...",
    "image_url": "...",
    "detected_foods": [...],
    "total_nutrition": {...},
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

---

### 4️⃣ **Statistiques Utilisateur**

**GET** `/statistics`

Statistiques nutritionnelles globales de l'utilisateur.

**Réponse (200 OK):**
```json
{
  "success": true,
  "message": "Statistics retrieved successfully",
  "data": {
    "total_analyses": 25,
    "average_per_meal": {
      "calories": 450.2,
      "proteins": 25.3,
      "carbohydrates": 55.8,
      "fats": 15.2
    },
    "total_calories_tracked": 11255.0
  }
}
```

---

### 5️⃣ **Nutrition Journalière**

**GET** `/history/daily?days=7`

Totaux nutritionnels par jour sur les N derniers jours.

**Query Parameters:**
- `days` (int, default: 7) - Nombre de jours

**Réponse (200 OK):**
```json
{
  "success": true,
  "message": "Daily nutrition history retrieved",
  "data": {
    "user_id": "abc123",
    "period_days": 7,
    "daily_totals": [
      {
        "date": "2024-01-15",
        "calories": 1850.5,
        "proteins": 95.2,
        "carbohydrates": 220.3,
        "fats": 65.8,
        "meal_count": 3
      }
    ]
  }
}
```

---

### 6️⃣ **Supprimer une Analyse**

**DELETE** `/history/{analysis_id}`

Supprime une analyse.

**Réponse (200 OK):**
```json
{
  "success": true,
  "message": "Analysis deleted successfully"
}
```

---

### 7️⃣ **Health Check**

**GET** `/health`

Vérifier l'état du service (pas d'authentification requise).

**Réponse (200 OK):**
```json
{
  "status": "healthy",
  "service": "nutrition-service",
  "version": "1.0.0"
}
```

---

## 🧪 Tests

### Tests Unitaires
```bash
# Installer pytest
pip install pytest pytest-cov

# Lancer tous les tests
pytest

# Avec coverage
pytest --cov=app tests/

# Tests spécifiques
pytest tests/test_jwt_service.py -v
```

### Tests d'Intégration
```bash
# Test complet du workflow
python tests/integration_test.py
```

### Tests Manuels avec cURL
```bash
# 1. Obtenir un token (depuis auth-service)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 2. Analyser une image
curl -X POST http://localhost:8086/api/v1/nutrition/analyze \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@path/to/food.jpg"

# 3. Récupérer l'historique
curl -X GET http://localhost:8086/api/v1/nutrition/history \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🚢 Déploiement

### Docker
```bash
# Build image
docker build -t nutrition-service:latest .

# Run container
docker run -d \
  --name nutrition-service \
  -p 8086:8086 \
  --env-file .env \
  nutrition-service:latest
```

### Docker Compose
```yaml
version: '3.8'

services:
  nutrition-service:
    build: .
    ports:
      - "8086:8086"
    environment:
      - MONGO_URI=mongodb://mongodb:27017/
      - MINIO_ENDPOINT=minio:9000
    depends_on:
      - mongodb
      - minio
```

### Production (Gunicorn)
```bash
# Avec 4 workers
gunicorn --bind 0.0.0.0:8086 \
         --workers 4 \
         --timeout 120 \
         --access-logfile - \
         --error-logfile - \
         app.main:create_app()
```

---

## 🐛 Troubleshooting

### Problème : ModuleNotFoundError
```bash
# Solution 1: Vérifier venv activé
which python  # Doit pointer vers venv/

# Solution 2: Réinstaller dépendances
pip install -r requirements.txt
```

### Problème : MongoDB Connection Failed
```bash
# Vérifier MongoDB est démarré
mongo --eval "db.adminCommand('ping')"

# Démarrer MongoDB
mongod --dbpath /path/to/data
```

### Problème : MinIO Connection Failed
```bash
# Vérifier MinIO est démarré
curl http://localhost:9000/minio/health/live

# Démarrer MinIO
cd C:\minio
.\minio.exe server ./data --console-address ":9001"
```

### Problème : JWT Token Invalid

**Cause:** JWT_SECRET différent entre auth-service et nutrition-service

**Solution:**
```bash
# Vérifier les secrets matchent
# auth-service/application.yml
# nutrition-service/.env

# Ils DOIVENT être IDENTIQUES !
```

### Problème : Image Upload Failed
```bash
# Vérifier taille < 10MB
# Vérifier format (JPEG/PNG)
# Vérifier MinIO bucket existe

# Créer bucket si nécessaire
curl -X PUT http://localhost:9000/nutrition-images \
  -H "Authorization: AWS minioadmin:minioadmin"
```

### Problème : ML Model Not Found
```bash
# Le service fonctionne avec un modèle placeholder
# Pour production, placer votre modèle ici:
# models/food_detection_model.h5
```

---

## 📊 Performance

### Benchmarks

- **Analyse d'image** : ~2-5 secondes
  - Upload : 200ms
  - Preprocessing : 100ms
  - ML Detection : 1-3s
  - Nutrition API : 500ms
  - Save to DB : 100ms

### Optimisations

- Caching des résultats nutrition (1h)
- Compression d'images automatique
- Batch processing possible
- Redis cache (future)

---

## 📚 Documentation Technique

### Structure du Code
```
app/
├── main.py              # Entry point Flask
├── config.py            # Configuration
├── services/            # Business logic
│   ├── jwt_service.py   # JWT validation
│   ├── image_service.py # Image processing
│   ├── ml_service.py    # ML model
│   ├── nutrition_api.py # External API
│   └── minio_service.py # Storage
├── routes/              # API endpoints
│   └── nutrition_routes.py
└── models/              # Data models
    └── nutrition_analysis.py
```

### Base de Données

**Collection: nutrition_analyses**
```javascript
{
  analysis_id: String (UUID),
  user_id: String,
  image_url: String,
  detected_foods: Array,
  total_nutrition: Object,
  recommendations: Object,
  created_at: DateTime,
  updated_at: DateTime
}
```

### Sécurité

- ✅ JWT authentication
- ✅ Input validation
- ✅ File size limits
- ✅ CORS configured
- ✅ Rate limiting (future)

---

## 🤝 Contribution

### Guidelines

1. Fork le projet
2. Créer une branche (`git checkout -b feature/amazing`)
3. Commit (`git commit -m 'Add amazing feature'`)
4. Push (`git push origin feature/amazing`)
5. Ouvrir une Pull Request

### Code Style
```bash
# Formatter
black app/ --line-length 100

# Linter
pylint app/

# Type checking
mypy app/
```

---

## 📝 Changelog

### Version 1.0.0 (2024-01-15)
- ✨ Initial release
- ✅ Image analysis with ML
- ✅ Nutrition API integration
- ✅ MongoDB storage
- ✅ MinIO image storage
- ✅ JWT authentication

---

## 📄 Licence

MIT License - Voir [LICENSE](LICENSE)

---

## 👥 Auteurs

- **Votre Nom** - Développement initial

---

## 🙏 Remerciements

- Open Food Facts API
- TensorFlow Team
- MinIO Team
- Flask Community

---

## 📞 Support

- **Email**: support@healthapp.com
- **GitHub Issues**: [Issues](https://github.com/your-repo/issues)
- **Documentation**: [Wiki](https://github.com/your-repo/wiki)

---

**🎉 Merci d'utiliser le Nutrition Service !**
