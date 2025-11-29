import requests
import threading
import time
import atexit
import logging
from flask import Flask

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class EurekaClient:
    def __init__(self, app: Flask = None):
        self.app = app
        self.eureka_url = "http://localhost:8761/eureka"
        self.app_name = "nutrition-service"
        self.instance_id = None
        self.heartbeat_thread = None
        self.running = False

    def init_app(self, app: Flask):
        self.app = app
        # Configuration depuis app.config ou valeurs par défaut
        self.eureka_url = app.config.get('EUREKA_URL', 'http://localhost:8761/eureka')
        self.app_name = app.config.get('APP_NAME', 'nutrition-service')
        self.hostname = app.config.get('HOSTNAME', 'localhost')
        self.port = app.config.get('PORT', 8086)
        self.instance_id = f"{self.hostname}:{self.app_name}:{self.port}"

    def register_with_eureka(self):
        """Enregistre le service auprès d'Eureka"""
        registration_data = {
            "instance": {
                "instanceId": self.instance_id,
                "app": self.app_name.upper(),
                "appGroupName": "HEALTH-APP",
                "ipAddr": self.hostname,
                "status": "UP",
                "overriddenstatus": "UNKNOWN",
                "port": {
                    "$": self.port,
                    "@enabled": "true"
                },
                "securePort": {
                    "$": 8443,
                    "@enabled": "false"
                },
                "countryId": 1,
                "dataCenterInfo": {
                    "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                    "name": "MyOwn"
                },
                "hostName": self.hostname,
                "statusPageUrl": f"http://{self.hostname}:{self.port}/info",
                "healthCheckUrl": f"http://{self.hostname}:{self.port}/health",
                "vipAddress": self.app_name,
                "secureVipAddress": self.app_name,
                "homePageUrl": f"http://{self.hostname}:{self.port}/",
                "metadata": {
                    "management.port": str(self.port)
                }
            }
        }

        try:
            response = requests.post(
                f"{self.eureka_url}/apps/{self.app_name}",
                json=registration_data,
                headers={"Content-Type": "application/json"}
            )
            if response.status_code in [200, 204]:
                logger.info(f"Service {self.app_name} enregistré avec succès auprès d'Eureka")
                self.start_heartbeat()
                return True
            else:
                logger.error(f"Échec de l'enregistrement: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            logger.error(f"Erreur lors de l'enregistrement Eureka: {str(e)}")
            return False

    def send_heartbeat(self):
        """Envoie un heartbeat à Eureka"""
        try:
            response = requests.put(
                f"{self.eureka_url}/apps/{self.app_name}/{self.instance_id}"
            )
            if response.status_code == 200:
                logger.debug("Heartbeat envoyé avec succès")
            else:
                logger.warning(f"Échec du heartbeat: {response.status_code}")
        except Exception as e:
            logger.error(f"Erreur lors de l'envoi du heartbeat: {str(e)}")

    def start_heartbeat(self):
        """Démarre le thread d'envoi des heartbeats"""
        self.running = True
        self.heartbeat_thread = threading.Thread(target=self._heartbeat_worker, daemon=True)
        self.heartbeat_thread.start()

    def _heartbeat_worker(self):
        """Worker pour envoyer les heartbeats périodiquement"""
        while self.running:
            self.send_heartbeat()
            time.sleep(30)  # Envoi toutes les 30 secondes

    def deregister(self):
        """Désenregistre le service d'Eureka"""
        self.running = False
        if self.heartbeat_thread:
            self.heartbeat_thread.join(timeout=5)

        try:
            response = requests.delete(
                f"{self.eureka_url}/apps/{self.app_name}/{self.instance_id}"
            )
            if response.status_code == 200:
                logger.info(f"Service {self.app_name} désenregistré avec succès")
            else:
                logger.warning(f"Échec du désenregistrement: {response.status_code}")
        except Exception as e:
            logger.error(f"Erreur lors du désenregistrement: {str(e)}")

# Instance globale
eureka_client = EurekaClient()