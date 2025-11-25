import requests
import time
import threading
import logging
from datetime import datetime

class EurekaClient:
    def __init__(self, eureka_url="http://localhost:8761/eureka", app_name="model-ai-service", port=8000):
        self.eureka_url = eureka_url
        self.app_name = app_name.upper()
        self.port = port
        self.instance_id = f"{app_name}:{port}"
        self.is_registered = False

        # Configuration logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

    def register_with_eureka(self):
        """Enregistre le service auprès d'Eureka"""
        registration_data = {
            "instance": {
                "instanceId": self.instance_id,
                "app": self.app_name,
                "hostName": "localhost",
                "ipAddr": "127.0.0.1",
                "status": "UP",
                "port": {
                    "$": self.port,
                    "@enabled": "true"
                },
                "securePort": {
                    "$": 443,
                    "@enabled": "false"
                },
                "healthCheckUrl": f"http://localhost:{self.port}/health",
                "statusPageUrl": f"http://localhost:{self.port}/health",
                "homePageUrl": f"http://localhost:{self.port}/",
                "vipAddress": self.app_name.lower(),
                "dataCenterInfo": {
                    "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                    "name": "MyOwn"
                },
                "leaseInfo": {
                    "renewalIntervalInSecs": 30,
                    "durationInSecs": 90
                },
                "metadata": {
                    "service": "health-ai",
                    "version": "2.0.0",
                    "language": "python"
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
                self.is_registered = True
                self.logger.info(f"✅ Service {self.app_name} enregistré avec succès auprès d'Eureka")
                return True
            else:
                self.logger.error(f"❌ Échec de l'enregistrement: {response.status_code} - {response.text}")
                return False

        except Exception as e:
            self.logger.error(f"❌ Erreur lors de l'enregistrement Eureka: {e}")
            return False

    def send_heartbeat(self):
        """Envoie un heartbeat à Eureka"""
        if not self.is_registered:
            return

        try:
            response = requests.put(
                f"{self.eureka_url}/apps/{self.app_name}/{self.instance_id}"
            )

            if response.status_code == 200:
                self.logger.debug(f"❤️ Heartbeat envoyé à {datetime.now()}")
            else:
                self.logger.warning(f"⚠️ Heartbeat échoué: {response.status_code}")
                # Tentative de ré-enregistrement
                if response.status_code == 404:
                    self.is_registered = False
                    self.register_with_eureka()

        except Exception as e:
            self.logger.error(f"❌ Erreur heartbeat: {e}")

    def start_heartbeat(self):
        """Démarre les heartbeats périodiques"""
        def heartbeat_loop():
            while True:
                time.sleep(30)  # Toutes les 30 secondes
                self.send_heartbeat()

        heartbeat_thread = threading.Thread(target=heartbeat_loop)
        heartbeat_thread.daemon = True
        heartbeat_thread.start()
        self.logger.info("🔄 Démarrage des heartbeats Eureka (30s)")

    def unregister(self):
        """Désenregistre le service d'Eureka"""
        try:
            response = requests.delete(
                f"{self.eureka_url}/apps/{self.app_name}/{self.instance_id}"
            )
            if response.status_code == 200:
                self.logger.info("✅ Service désenregistré d'Eureka")
            else:
                self.logger.warning(f"⚠️ Échec désenregistrement: {response.status_code}")
        except Exception as e:
            self.logger.error(f"❌ Erreur désenregistrement: {e}")

# Instance globale
eureka_client = EurekaClient()