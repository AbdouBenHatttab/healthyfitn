from pymongo import MongoClient, DESCENDING
from datetime import datetime
from nutrition_app.config import Config
import uuid

class NutritionAnalysisModel:
    def __init__(self):
        self.client = MongoClient(Config.MONGO_URI)
        self.db = self.client[Config.MONGO_DATABASE]
        self.collection = self.db['nutrition_analyses']
        self._create_indexes()
    
    def _create_indexes(self):
        """Create indexes for better query performance"""
        self.collection.create_index([('user_id', DESCENDING), ('created_at', DESCENDING)])
        self.collection.create_index('analysis_id')
        self.collection.create_index('created_at')
    
    def create_analysis(self, user_id, image_url, detected_foods, total_nutrition, recommendations):
        """
        Create a new nutrition analysis record
        
        Returns:
            str: analysis_id
        """
        analysis_id = str(uuid.uuid4())
        
        document = {
            'analysis_id': analysis_id,
            'user_id': user_id,
            'image_url': image_url,
            'detected_foods': detected_foods,
            'total_nutrition': total_nutrition,
            'recommendations': recommendations,
            'created_at': datetime.utcnow(),
            'updated_at': datetime.utcnow()
        }
        
        self.collection.insert_one(document)
        return analysis_id
    
    def get_analysis_by_id(self, analysis_id):
        """Get analysis by ID"""
        return self.collection.find_one({'analysis_id': analysis_id}, {'_id': 0})
    
    def get_user_analyses(self, user_id, limit=20, skip=0):
        """
        Get all analyses for a user
        
        Returns:
            list of analyses
        """
        cursor = self.collection.find(
            {'user_id': user_id},
            {'_id': 0}
        ).sort('created_at', DESCENDING).skip(skip).limit(limit)
        
        return list(cursor)
    
    def get_user_nutrition_history(self, user_id, days=7):
        """
        Get nutrition history for last N days
        
        Returns:
            dict with daily totals
        """
        from datetime import timedelta
        start_date = datetime.utcnow() - timedelta(days=days)
        
        pipeline = [
            {
                '$match': {
                    'user_id': user_id,
                    'created_at': {'$gte': start_date}
                }
            },
            {
                '$group': {
                    '_id': {
                        '$dateToString': {
                            'format': '%Y-%m-%d',
                            'date': '$created_at'
                        }
                    },
                    'total_calories': {'$sum': '$total_nutrition.calories'},
                    'total_proteins': {'$sum': '$total_nutrition.proteins'},
                    'total_carbs': {'$sum': '$total_nutrition.carbohydrates'},
                    'total_fats': {'$sum': '$total_nutrition.fats'},
                    'meal_count': {'$sum': 1}
                }
            },
            {
                '$sort': {'_id': 1}
            }
        ]
        
        results = list(self.collection.aggregate(pipeline))
        
        return {
            'user_id': user_id,
            'period_days': days,
            'daily_totals': [
                {
                    'date': item['_id'],
                    'calories': round(item['total_calories'], 1),
                    'proteins': round(item['total_proteins'], 1),
                    'carbohydrates': round(item['total_carbs'], 1),
                    'fats': round(item['total_fats'], 1),
                    'meal_count': item['meal_count']
                }
                for item in results
            ]
        }
    
    def get_user_statistics(self, user_id):
        """
        Get overall statistics for a user
        
        Returns:
            dict with statistics
        """
        pipeline = [
            {
                '$match': {'user_id': user_id}
            },
            {
                '$group': {
                    '_id': None,
                    'total_analyses': {'$sum': 1},
                    'avg_calories': {'$avg': '$total_nutrition.calories'},
                    'avg_proteins': {'$avg': '$total_nutrition.proteins'},
                    'avg_carbs': {'$avg': '$total_nutrition.carbohydrates'},
                    'avg_fats': {'$avg': '$total_nutrition.fats'},
                    'total_calories': {'$sum': '$total_nutrition.calories'}
                }
            }
        ]
        
        result = list(self.collection.aggregate(pipeline))
        
        if result:
            stats = result[0]
            return {
                'total_analyses': stats['total_analyses'],
                'average_per_meal': {
                    'calories': round(stats['avg_calories'], 1),
                    'proteins': round(stats['avg_proteins'], 1),
                    'carbohydrates': round(stats['avg_carbs'], 1),
                    'fats': round(stats['avg_fats'], 1)
                },
                'total_calories_tracked': round(stats['total_calories'], 1)
            }
        else:
            return {
                'total_analyses': 0,
                'average_per_meal': {
                    'calories': 0,
                    'proteins': 0,
                    'carbohydrates': 0,
                    'fats': 0
                },
                'total_calories_tracked': 0
            }
    
    def delete_analysis(self, analysis_id, user_id):
        """Delete an analysis (only if user owns it)"""
        result = self.collection.delete_one({
            'analysis_id': analysis_id,
            'user_id': user_id
        })
        return result.deleted_count > 0
    
    def update_analysis(self, analysis_id, user_id, updates):
        """Update an analysis"""
        updates['updated_at'] = datetime.utcnow()
        
        result = self.collection.update_one(
            {'analysis_id': analysis_id, 'user_id': user_id},
            {'$set': updates}
        )
        return result.modified_count > 0