# Utilise une image JDK 17
FROM eclipse-temurin:17-jdk-alpine

# Définit le répertoire de travail
WORKDIR /app

# Copie le fichier JAR
COPY target/discovery-service-1.0.0.jar app.jar

# Expose le port 8761
EXPOSE 8761

# Commande pour démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Variables d'environnement par défaut
ENV JAVA_OPTS="-Xmx512m -Xms256m"