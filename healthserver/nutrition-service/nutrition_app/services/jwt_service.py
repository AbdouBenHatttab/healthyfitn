import jwt
from functools import wraps
from flask import request, jsonify
from nutrition_app.config import Config

class JWTService:
    @staticmethod
    def extract_token():
        """Extract JWT token from Authorization header"""
        auth_header = request.headers.get('Authorization')
        print(f"[DEBUG] Authorization header: {auth_header[:50] if auth_header else 'None'}...")
        
        if not auth_header:
            print("[DEBUG] No Authorization header found")
            return None
        
        parts = auth_header.split()
        if len(parts) != 2 or parts[0].lower() != 'bearer':
            print(f"[DEBUG] Invalid Authorization format. Parts: {len(parts)}")
            return None
        
        token = parts[1]
        print(f"[DEBUG] Token extracted successfully (length: {len(token)})")
        return token
    
    @staticmethod
    def validate_token(token):
        """
        Validate JWT token and extract user information
        Returns: dict with user_id, email, roles
        """
        try:
            # DEBUG: Afficher l'algorithme du token
            header = jwt.get_unverified_header(token)
            print(f"[DEBUG] Token algorithm: {header.get('alg')}")
            print(f"[DEBUG] Allowed algorithms: {Config.JWT_ALGORITHMS}")
            print(f"[DEBUG] JWT_SECRET exists: {Config.JWT_SECRET is not None}")
            print(f"[DEBUG] JWT_SECRET length: {len(Config.JWT_SECRET) if Config.JWT_SECRET else 0}")
            
            # Décoder le payload sans vérification pour voir ce qu'il contient
            unverified_payload = jwt.decode(token, options={"verify_signature": False})
            print(f"[DEBUG] Token payload (unverified): {unverified_payload}")
            
            # Maintenant décoder avec vérification
            payload = jwt.decode(
                token,
                Config.JWT_SECRET,
                algorithms=Config.JWT_ALGORITHMS,
                options={"verify_signature": True}
            )
            
            print(f"[DEBUG] Token validated successfully for user: {payload.get('email')}")
            
            return {
                'user_id': payload.get('user_id'),
                'email': payload.get('email'),
                'roles': payload.get('roles', []),
                'is_activated': payload.get('is_activated', False)
            }
        except jwt.ExpiredSignatureError:
            print("[DEBUG] Token has expired")
            raise Exception('Token has expired')
        except jwt.InvalidTokenError as e:
            print(f"[DEBUG] JWT Error: {str(e)}")
            print(f"[DEBUG] Error type: {type(e).__name__}")
            raise Exception(f'Invalid token: {str(e)}')
        except Exception as e:
            print(f"[DEBUG] Unexpected error: {str(e)}")
            raise Exception(f'Token validation error: {str(e)}')
    
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
            print(f"[DEBUG] require_auth called for endpoint: {request.endpoint}")
            user = JWTService.get_current_user()
            request.current_user = user
            print(f"[DEBUG] Authentication successful for user: {user.get('email')}")
            return f(*args, **kwargs)
        except Exception as e:
            print(f"[DEBUG] Authentication failed: {str(e)}")
            return jsonify({
                'success': False,
                'message': str(e)
            }), 401
    
    return decorated_function


def require_roles(*allowed_roles):
    """Decorator to require specific roles"""
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            try:
                user = JWTService.get_current_user()
                request.current_user = user
                
                user_roles = user.get('roles', [])
                if not any(role in user_roles for role in allowed_roles):
                    return jsonify({
                        'success': False,
                        'message': 'Insufficient permissions'
                    }), 403
                
                return f(*args, **kwargs)
            except Exception as e:
                return jsonify({
                    'success': False,
                    'message': str(e)
                }), 401
        
        return decorated_function
    return decorator