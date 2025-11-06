# Health App Microservices

## Architecture
- **API Gateway** (Port 8080): Point d'entrée unique
- **Config Service** (Port 8888): Configuration centralisée
- **Auth Service** (Port 8082): Authentification et autorisation
- **User Service** (Port 8083): Gestion des utilisateurs

## Démarrage

### Avec Docker Compose:
```bash
docker-compose up -d
```

### Développement local:
```bash
# Démarrer MongoDB
docker run -d -p 27017:27017 --name mongodb mongo:7.0

# Démarrer les services
cd config-service && mvn spring-boot:run &
cd auth-service && mvn spring-boot:run &
cd user-service && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &
```

## Endpoints

### Auth Service
- POST /api/v1/auth/register - Inscription
- POST /api/v1/auth/login - Connexion
- POST /api/v1/auth/refresh - Rafraîchir le token
- POST /api/v1/auth/logout - Déconnexion

### User Service
- GET /api/v1/user/profile - Profil utilisateur
- PUT /api/v1/user/profile - Mettre à jour le profil
- GET /api/v1/admin/users - Liste des utilisateurs (Admin)

## Technologies
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- MongoDB 7.0
- JWT (jjwt 0.12.3)
- Docker & Docker Compose