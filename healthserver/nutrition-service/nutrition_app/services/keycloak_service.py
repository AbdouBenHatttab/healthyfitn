"""
Keycloak JWT Service - Validation des tokens Keycloak
Compatible avec les tokens RS256 (clé publique) et HS256 (secret partagé)
"""

import jwt
import requests
from functools import lru_cache
from nutrition_app.config import Config
import logging

logger = logging.getLogger(__name__)


class KeycloakJWTService:
    """Service de validation des tokens JWT Keycloak"""

    def __init__(self):
        self.server_url = Config.KEYCLOAK_SERVER_URL
        self.realm = Config.KEYCLOAK_REALM
        self.client_id = Config.KEYCLOAK_CLIENT_ID
        self.client_secret = Config.KEYCLOAK_CLIENT_SECRET

        # URLs Keycloak
        self.realm_url = f"{self.server_url}/realms/{self.realm}"
        self.certs_url = f"{self.realm_url}/protocol/openid-connect/certs"
        self.token_introspection_url = f"{self.realm_url}/protocol/openid-connect/token/introspect"

        logger.info(f"🔐 KeycloakJWTService initialisé - Realm: {self.realm}")

    @lru_cache(maxsize=1)
    def get_public_key(self):
        """
        Récupère la clé publique de Keycloak pour valider les tokens RS256
        Mise en cache pour éviter les appels répétés
        """
        try:
            logger.debug(f"📡 Récupération clé publique depuis: {self.certs_url}")

            response = requests.get(self.certs_url, timeout=10)
            response.raise_for_status()

            jwks = response.json()

            # Keycloak retourne un JWKS (JSON Web Key Set)
            # On prend la première clé (généralement la clé active)
            if 'keys' in jwks and len(jwks['keys']) > 0:
                key = jwks['keys'][0]

                # Construire la clé publique PEM
                from jwt.algorithms import RSAAlgorithm
                public_key = RSAAlgorithm.from_jwk(key)

                logger.info("✅ Clé publique Keycloak récupérée")
                return public_key
            else:
                raise Exception("Aucune clé trouvée dans JWKS")

        except Exception as e:
            logger.error(f"❌ Erreur récupération clé publique: {str(e)}")
            raise Exception(f"Impossible de récupérer la clé publique Keycloak: {str(e)}")

    def validate_token(self, token):
        """
        Valide un token JWT Keycloak

        Méthodes de validation (dans l'ordre) :
        1. Validation avec clé publique (RS256) - Recommandé
        2. Validation avec secret partagé (HS256) - Fallback
        3. Token introspection API Keycloak - Dernier recours

        Args:
            token (str): Token JWT

        Returns:
            dict: User info (user_id, email, roles, etc.)

        Raises:
            Exception: Si le token est invalide
        """
        try:
            # ========================================
            # MÉTHODE 1: Validation avec clé publique (RS256)
            # ========================================
            try:
                logger.debug("🔍 Tentative validation RS256...")

                public_key = self.get_public_key()

                payload = jwt.decode(
                    token,
                    public_key,
                    algorithms=['RS256'],
                    audience=self.client_id,
                    options={
                        'verify_signature': True,
                        'verify_exp': True,
                        'verify_aud': True
                    }
                )

                logger.info("✅ Token validé avec RS256")
                return self._extract_user_info(payload)

            except jwt.InvalidAudienceError:
                # Audience mismatch, réessayer sans vérification audience
                logger.debug("⚠️  Audience mismatch, réessai sans vérification audience...")

                payload = jwt.decode(
                    token,
                    public_key,
                    algorithms=['RS256'],
                    options={
                        'verify_signature': True,
                        'verify_exp': True,
                        'verify_aud': False  # Désactiver vérification audience
                    }
                )

                logger.info("✅ Token validé avec RS256 (sans audience)")
                return self._extract_user_info(payload)

            except jwt.PyJWTError as e:
                logger.debug(f"⚠️  Validation RS256 échouée: {str(e)}, essai HS256...")

            # ========================================
            # MÉTHODE 2: Fallback avec secret partagé (HS256)
            # ========================================
            if Config.JWT_SECRET:
                try:
                    logger.debug("🔍 Tentative validation HS256...")

                    payload = jwt.decode(
                        token,
                        Config.JWT_SECRET,
                        algorithms=['HS256'],
                        options={
                            'verify_signature': True,
                            'verify_exp': True
                        }
                    )

                    logger.info("✅ Token validé avec HS256")
                    return self._extract_user_info(payload)

                except jwt.PyJWTError as e:
                    logger.debug(f"⚠️  Validation HS256 échouée: {str(e)}")

            # ========================================
            # MÉTHODE 3: Token Introspection API (dernier recours)
            # ========================================
            logger.debug("🔍 Tentative introspection API Keycloak...")
            return self._introspect_token(token)

        except jwt.ExpiredSignatureError:
            logger.warning("⏰ Token expiré")
            raise Exception('Token has expired')

        except Exception as e:
            logger.error(f"❌ Validation token échouée: {str(e)}")
            raise Exception(f'Invalid token: {str(e)}')

    def _introspect_token(self, token):
        """
        Utilise l'API d'introspection de Keycloak pour valider un token
        Plus lent mais plus fiable en cas de problème avec les autres méthodes
        """
        try:
            data = {
                'token': token,
                'client_id': self.client_id,
                'client_secret': self.client_secret
            }

            response = requests.post(
                self.token_introspection_url,
                data=data,
                timeout=10
            )
            response.raise_for_status()

            result = response.json()

            if not result.get('active', False):
                raise Exception('Token is not active')

            logger.info("✅ Token validé via introspection API")
            return self._extract_user_info(result)

        except Exception as e:
            logger.error(f"❌ Introspection échouée: {str(e)}")
            raise Exception(f'Token introspection failed: {str(e)}')

    def _extract_user_info(self, payload):
        """
        Extrait les informations utilisateur du payload JWT
        Compatible avec les différents formats de payload Keycloak
        """
        # Keycloak utilise 'sub' pour l'ID utilisateur
        user_id = payload.get('sub') or payload.get('user_id')

        # Email
        email = payload.get('email') or payload.get('preferred_username')

        # Rôles (plusieurs formats possibles dans Keycloak)
        roles = []

        # Format 1: realm_access.roles
        if 'realm_access' in payload and 'roles' in payload['realm_access']:
            roles.extend(payload['realm_access']['roles'])

        # Format 2: resource_access.{client_id}.roles
        if 'resource_access' in payload and self.client_id in payload['resource_access']:
            client_roles = payload['resource_access'][self.client_id].get('roles', [])
            roles.extend(client_roles)

        # Format 3: roles directement (pour compatibilité)
        if 'roles' in payload:
            roles.extend(payload['roles'])

        # Nettoyer les doublons et convertir en majuscules
        roles = list(set([role.upper() for role in roles]))

        # Informations supplémentaires
        first_name = payload.get('given_name', '')
        last_name = payload.get('family_name', '')

        user_info = {
            'user_id': user_id,
            'email': email,
            'roles': roles,
            'first_name': first_name,
            'last_name': last_name,
            'email_verified': payload.get('email_verified', False),
            'is_activated': True  # Si le token est valide, l'utilisateur est activé
        }

        logger.debug(f"👤 User info extrait: {email} - Roles: {roles}")

        return user_info


# Instance singleton
_keycloak_jwt_service = None


def get_keycloak_jwt_service():
    """Retourne l'instance singleton du service"""
    global _keycloak_jwt_service
    if _keycloak_jwt_service is None:
        _keycloak_jwt_service = KeycloakJWTService()
    return _keycloak_jwt_service