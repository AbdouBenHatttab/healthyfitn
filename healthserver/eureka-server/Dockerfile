# Étape 1 : utiliser une image Maven pour compiler le projet
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers de configuration Maven
COPY pom.xml .

# Télécharger les dépendances (cache efficace)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler le projet et créer le JAR
RUN mvn clean package -DskipTests

# Étape 2 : utiliser une image JDK légère pour exécuter l'application
FROM eclipse-temurin:17-jdk-alpine

# Répertoire de travail
WORKDIR /app

# Copier le jar depuis le builder
COPY --from=builder /app/target/*.jar app.jar

# Exposer le port d’Eureka
EXPOSE 8761

# Démarrer le serveur Eureka
ENTRYPOINT ["java", "-jar", "app.jar"]
