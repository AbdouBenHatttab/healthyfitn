"""
JWT Service - MODIFIÉ pour supporter Keycloak
Utilise KeycloakJWTService pour la validation des tokens
"""

from functools import wraps
from flask import request, jsonify
from nutrition_app.services.keycloak_service import get_keycloak_jwt_service
import logging

logger = logging.getLogger(__name__)


class JWTService:
    """Service JWT compatible Keycloak"""

    @staticmethod
    def extract_token():
        """Extract JWT token from Authorization header"""
        auth_header = request.headers.get('Authorization')

        if not auth_header:
            logger.debug("❌ No Authorization header found")
            return None

        parts = auth_header.split()
        if len(parts) != 2 or parts[0].lower() != 'bearer':
            logger.debug(f"❌ Invalid Authorization format. Parts: {len(parts)}")
            return None

        token = parts[1]
        logger.debug(f"✅ Token extracted (length: {len(token)})")
        return token

    @staticmethod
    def validate_token(token):
        """
        Validate JWT token using Keycloak
        Returns: dict with user_id, email, roles
        """
        try:
            keycloak_service = get_keycloak_jwt_service()
            user_info = keycloak_service.validate_token(token)

            logger.info(f"✅ Token validé pour: {user_info.get('email')}")
            return user_info

        except Exception as e:
            logger.error(f"❌ Validation token échouée: {str(e)}")
            raise

    @staticmethod
    def get_current_user():
        """Get current user from JWT token"""
        token = JWTService.extract_token()
        if not token:
            raise Exception('No token provided')

        return JWTService.validate_token(token)


def require_auth(f):
    """Decorator to require authentication"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        try:
            logger.debug(f"🔐 require_auth - Endpoint: {request.endpoint}")

            user = JWTService.get_current_user()
            request.current_user = user

            logger.info(f"✅ Authentication OK: {user.get('email')}")
            return f(*args, **kwargs)

        except Exception as e:
            logger.warning(f"❌ Authentication failed: {str(e)}")
            return jsonify({
                'success': False,
                'message': str(e)
            }), 401

    return decorated_function


def require_roles(*allowed_roles):
    """
    Decorator to require specific roles

    Usage:
        @require_roles('ADMIN', 'DOCTOR')
        def my_endpoint():
            ...
    """
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            try:
                logger.debug(f"🔐 require_roles - Endpoint: {request.endpoint}, Required: {allowed_roles}")

                user = JWTService.get_current_user()
                request.current_user = user

                user_roles = user.get('roles', [])

                # Vérifier si l'utilisateur a au moins un des rôles requis
                has_required_role = any(role.upper() in [r.upper() for r in user_roles] for role in allowed_roles)

                if not has_required_role:
                    logger.warning(f"❌ Insufficient permissions. User roles: {user_roles}, Required: {allowed_roles}")
                    return jsonify({
                        'success': False,
                        'message': 'Insufficient permissions'
                    }), 403

                logger.info(f"✅ Authorization OK: {user.get('email')} - Roles: {user_roles}")
                return f(*args, **kwargs)

            except Exception as e:
                logger.warning(f"❌ Authentication/Authorization failed: {str(e)}")
                return jsonify({
                    'success': False,
                    'message': str(e)
                }), 401

        return decorated_function
    return decorator