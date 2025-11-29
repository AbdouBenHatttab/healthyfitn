
"""
Routes Nutrition - MODIFIÉES pour utiliser le vrai modèle AI
"""

from flask import Blueprint, request, jsonify
from nutrition_app.services.jwt_service import require_auth, require_roles
from nutrition_app.services.image_service import ImageService
from nutrition_app.services.ml_service import MLService
from nutrition_app.services.nutrition_api import NutritionAPIService
from nutrition_app.services.minio_service import MinIOService
from nutrition_app.models.nutrition_analysis import NutritionAnalysisModel
from werkzeug.utils import secure_filename
import logging

logger = logging.getLogger(__name__)

# Initialize services
image_service = ImageService()
ml_service = MLService()
nutrition_api = NutritionAPIService()
minio_service = MinIOService()
nutrition_model = NutritionAnalysisModel()

# Create Blueprint
nutrition_bp = Blueprint('nutrition', __name__)


@nutrition_bp.route('/analyze', methods=['POST'])
@require_auth
def analyze_food():
    """
    ⭐ ENDPOINT PRINCIPAL - Analyse nutritionnelle par image
    
    POST /api/v1/nutrition/analyze
    Headers: Authorization: Bearer <JWT_TOKEN>
    Body: multipart/form-data
        - image: fichier image (jpg, png)
        - use_ai: "true" pour utiliser le modèle AI (défaut: true)
    
    Returns:
        JSON avec nutrition complète + plat détecté
    """
    try:
        # Get current user
        user = request.current_user
        user_id = user['user_id']
        
        logger.info(f"🍽️  Analyse demandée par user: {user_id}")
        
        # Check image
        if 'image' not in request.files:
            return jsonify({
                'success': False,
                'message': 'No image file provided'
            }), 400
        
        image_file = request.files['image']
        
        if image_file.filename == '':
            return jsonify({
                'success': False,
                'message': 'No image selected'
            }), 400
        
        # Option: utiliser le modèle AI ou non
        use_ai = request.form.get('use_ai', 'true').lower() == 'true'
        
        # Read image
        image_data = image_file.read()
        
        # 1. VALIDATE
        is_valid, error_msg = image_service.validate_image(image_data)
        if not is_valid:
            return jsonify({
                'success': False,
                'message': error_msg
            }), 400
        
        # 2. COMPRESS
        compressed_image = image_service.compress_image(image_data)
        
        # 3. UPLOAD TO MINIO
        upload_result = minio_service.upload_image(
            compressed_image,
            user_id,
            secure_filename(image_file.filename)
        )
        image_url = upload_result['url']
        
        # 4. PREPROCESS FOR ML
        preprocessed_image = image_service.preprocess_image(compressed_image)
        
        # ========================================
        # ⭐ NOUVELLE LOGIQUE AVEC MODÈLE AI
        # ========================================
        
        if use_ai:
            try:
                logger.info("🤖 Utilisation du modèle AI...")
                
                # Utiliser la nouvelle méthode qui retourne tout
                ai_result = ml_service.get_nutrition_from_ai(preprocessed_image)
                
                # Extraire les infos
                detected_food = ai_result['detected_food']
                predicted_nutrition = ai_result['nutrition']
                
                # Formater pour la réponse
                nutrition_details = [{
                    'food_name': detected_food['name'],
                    'class_id': detected_food['class_id'],
                    'confidence': detected_food['confidence'],
                    'calories': predicted_nutrition['calories'],
                    'proteins': predicted_nutrition['protein'],
                    'carbohydrates': predicted_nutrition['carbs'],
                    'fats': predicted_nutrition['fat'],
                    'fiber': predicted_nutrition['fiber'],
                    'sugars': predicted_nutrition['sugars'],
                    'sodium': predicted_nutrition['sodium']
                }]
                
                # Total nutrition = prédiction directe
                total_nutrition = {
                    'calories': predicted_nutrition['calories'],
                    'proteins': predicted_nutrition['protein'],
                    'carbohydrates': predicted_nutrition['carbs'],
                    'fats': predicted_nutrition['fat'],
                    'fiber': predicted_nutrition['fiber'],
                    'sugars': predicted_nutrition['sugars'],
                    'sodium': predicted_nutrition['sodium']
                }
                
                # Portion info
                portion_info = {
                    'portion_size': 'standard',
                    'portion_multiplier': 1.0,
                    'note': 'Values for standard portion (100g or typical serving)'
                }
                
                # Top 5 alternatives
                alternatives = ai_result.get('top5_predictions', [])
                
                logger.info(f"✅ AI Détection: {detected_food['name']} ({detected_food['confidence']}%)")
                
            except Exception as e:
                logger.error(f"❌ Erreur modèle AI: {str(e)}")
                logger.info("⚠️  Fallback vers méthode classique")
                use_ai = False
        
        # ========================================
        # FALLBACK: Méthode classique (API externe)
        # ========================================
        
        if not use_ai:
            logger.info("🔄 Utilisation de la méthode classique...")
            
            # Détection classique
            detected_foods = ml_service.detect_food(preprocessed_image)
            
            if not detected_foods:
                return jsonify({
                    'success': False,
                    'message': 'No food detected in image'
                }), 400
            
            # Portion estimation
            portion_info = ml_service.estimate_portion_size(preprocessed_image)
            portion_multiplier = portion_info['portion_multiplier']
            
            # Get nutrition pour chaque food
            nutrition_details = []
            total_nutrition = {
                'calories': 0,
                'proteins': 0,
                'carbohydrates': 0,
                'fats': 0,
                'fiber': 0,
                'sugars': 0,
                'sodium': 0
            }
            
            for food in detected_foods:
                nutrition = nutrition_api.get_nutrition_info(
                    food['food_name'],
                    portion_multiplier
                )
                nutrition['confidence'] = food['confidence']
                nutrition_details.append(nutrition)
                
                # Sum totals
                total_nutrition['calories'] += nutrition['calories']
                total_nutrition['proteins'] += nutrition['proteins']
                total_nutrition['carbohydrates'] += nutrition['carbohydrates']
                total_nutrition['fats'] += nutrition['fats']
                total_nutrition['fiber'] += nutrition['fiber']
                total_nutrition['sugars'] += nutrition['sugars']
                total_nutrition['sodium'] += nutrition['sodium']
            
            # Round
            for key in total_nutrition:
                total_nutrition[key] = round(total_nutrition[key], 1)
            
            alternatives = []
        
        # ========================================
        # RECOMMANDATIONS
        # ========================================
        
        user_profile = request.form.get('user_profile', '{}')
        import json
        try:
            profile = json.loads(user_profile) if user_profile else {}
        except:
            profile = {}
        
        recommendations = nutrition_api.get_dietary_recommendations(
            total_nutrition,
            profile
        )
        
        # ========================================
        # SAVE TO DATABASE
        # ========================================
        
        analysis_id = nutrition_model.create_analysis(
            user_id=user_id,
            image_url=image_url,
            detected_foods=nutrition_details,
            total_nutrition=total_nutrition,
            recommendations=recommendations
        )
        
        # ========================================
        # RESPONSE
        # ========================================
        
        response_data = {
            'analysis_id': analysis_id,
            'image_url': image_url,
            'detected_foods': nutrition_details,
            'portion_size': portion_info.get('portion_size', 'standard'),
            'total_nutrition': total_nutrition,
            'recommendations': recommendations,
            'method': 'ai_model' if use_ai else 'api_fallback'
        }
        
        # Ajouter alternatives si disponibles
        if alternatives:
            response_data['alternatives'] = alternatives
        
        return jsonify({
            'success': True,
            'message': 'Food analysis completed successfully',
            'data': response_data
        }), 200
    
    except Exception as e:
        logger.error(f"❌ Erreur analyse: {str(e)}")
        return jsonify({
            'success': False,
            'message': f'Error analyzing food: {str(e)}'
        }), 500


@nutrition_bp.route('/model/status', methods=['GET'])
@require_auth
def get_model_status():
    """
    ⭐ NOUVEAU ENDPOINT - Statut du modèle AI
    
    GET /api/v1/nutrition/model/status
    """
    try:
        status = ml_service.get_model_status()
        
        return jsonify({
            'success': True,
            'data': status
        }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500


@nutrition_bp.route('/history', methods=['GET'])
@require_auth
def get_nutrition_history():
    """
    Get user's nutrition analysis history
    
    GET /api/v1/nutrition/history?limit=20&skip=0
    """
    try:
        user_id = request.current_user['user_id']
        
        limit = request.args.get('limit', 20, type=int)
        skip = request.args.get('skip', 0, type=int)
        
        analyses = nutrition_model.get_user_analyses(user_id, limit, skip)
        
        return jsonify({
            'success': True,
            'message': 'History retrieved successfully',
            'data': {
                'analyses': analyses,
                'count': len(analyses)
            }
        }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error retrieving history: {str(e)}'
        }), 500


@nutrition_bp.route('/history/<analysis_id>', methods=['GET'])
@require_auth
def get_analysis_by_id(analysis_id):
    """
    Get specific analysis by ID
    
    GET /api/v1/nutrition/history/{analysis_id}
    """
    try:
        analysis = nutrition_model.get_analysis_by_id(analysis_id)
        
        if not analysis:
            return jsonify({
                'success': False,
                'message': 'Analysis not found'
            }), 404
        
        # Check ownership
        if analysis['user_id'] != request.current_user['user_id']:
            return jsonify({
                'success': False,
                'message': 'Unauthorized access'
            }), 403
        
        return jsonify({
            'success': True,
            'message': 'Analysis retrieved successfully',
            'data': analysis
        }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error retrieving analysis: {str(e)}'
        }), 500


@nutrition_bp.route('/statistics', methods=['GET'])
@require_auth
def get_user_statistics():
    """
    Get user's nutrition statistics
    
    GET /api/v1/nutrition/statistics
    """
    try:
        user_id = request.current_user['user_id']
        stats = nutrition_model.get_user_statistics(user_id)
        
        return jsonify({
            'success': True,
            'message': 'Statistics retrieved successfully',
            'data': stats
        }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error retrieving statistics: {str(e)}'
        }), 500


@nutrition_bp.route('/history/daily', methods=['GET'])
@require_auth
def get_daily_nutrition():
    """
    Get daily nutrition history
    
    GET /api/v1/nutrition/history/daily?days=7
    """
    try:
        user_id = request.current_user['user_id']
        days = request.args.get('days', 7, type=int)
        
        history = nutrition_model.get_user_nutrition_history(user_id, days)
        
        return jsonify({
            'success': True,
            'message': 'Daily nutrition history retrieved',
            'data': history
        }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error retrieving daily nutrition: {str(e)}'
        }), 500


@nutrition_bp.route('/history/<analysis_id>', methods=['DELETE'])
@require_auth
def delete_analysis(analysis_id):
    """
    Delete a nutrition analysis
    
    DELETE /api/v1/nutrition/history/{analysis_id}
    """
    try:
        user_id = request.current_user['user_id']
        deleted = nutrition_model.delete_analysis(analysis_id, user_id)
        
        if deleted:
            return jsonify({
                'success': True,
                'message': 'Analysis deleted successfully'
            }), 200
        else:
            return jsonify({
                'success': False,
                'message': 'Analysis not found or unauthorized'
            }), 404
    
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error deleting analysis: {str(e)}'
        }), 500


@nutrition_bp.route('/health', methods=['GET'])
def health_check():
    """
    Health check endpoint
    
    GET /api/v1/nutrition/health
    """
    try:
        # Vérifier le statut du modèle
        model_status = ml_service.get_model_status()
        
        return jsonify({
            'status': 'healthy',
            'service': 'nutrition-service',
            'version': '1.0.0',
            'ai_model': model_status['status']
        }), 200
    except:
        return jsonify({
            'status': 'healthy',
            'service': 'nutrition-service',
            'version': '1.0.0',
            'ai_model': 'unknown'
        }), 200