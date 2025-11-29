import requests
from nutrition_app.config import Config
import time

class NutritionAPIService:
    def __init__(self):
        self.openfoodfacts_url = Config.OPENFOODFACTS_API
        self.cache = {}  # Simple in-memory cache
    
    def get_nutrition_info(self, food_name, portion_multiplier=1.0):
        """
        Get nutrition information from Open Food Facts API
        
        Args:
            food_name: string
            portion_multiplier: float (adjust for portion size)
        
        Returns:
            dict with nutrition information
        """
        try:
            # Check cache first
            cache_key = f"{food_name}_{portion_multiplier}"
            if cache_key in self.cache:
                return self.cache[cache_key]
            
            # Search for product
            search_url = f"{self.openfoodfacts_url}/search"
            params = {
                'search_terms': food_name,
                'search_simple': 1,
                'action': 'process',
                'json': 1,
                'page_size': 1
            }
            
            response = requests.get(search_url, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()
            
            if data.get('count', 0) > 0 and len(data.get('products', [])) > 0:
                product = data['products'][0]
                nutrition = self._extract_nutrition_data(product, portion_multiplier)
                
                # Cache result
                self.cache[cache_key] = nutrition
                return nutrition
            else:
                # Fallback to default nutrition data
                return self._get_default_nutrition(food_name, portion_multiplier)
        
        except requests.RequestException as e:
            print(f"⚠️ API request failed: {e}. Using default values.")
            return self._get_default_nutrition(food_name, portion_multiplier)
        except Exception as e:
            print(f"❌ Error getting nutrition info: {e}")
            return self._get_default_nutrition(food_name, portion_multiplier)
    
    def _extract_nutrition_data(self, product, multiplier):
        """Extract and format nutrition data from API response"""
        nutriments = product.get('nutriments', {})
        
        return {
            'food_name': product.get('product_name', 'Unknown'),
            'brand': product.get('brands', ''),
            'serving_size': product.get('serving_size', '100g'),
            'calories': round(nutriments.get('energy-kcal_100g', 0) * multiplier, 1),
            'proteins': round(nutriments.get('proteins_100g', 0) * multiplier, 1),
            'carbohydrates': round(nutriments.get('carbohydrates_100g', 0) * multiplier, 1),
            'fats': round(nutriments.get('fat_100g', 0) * multiplier, 1),
            'fiber': round(nutriments.get('fiber_100g', 0) * multiplier, 1),
            'sugars': round(nutriments.get('sugars_100g', 0) * multiplier, 1),
            'sodium': round(nutriments.get('sodium_100g', 0) * multiplier * 1000, 1),  # Convert to mg
            'vitamins': {
                'vitamin_a': round(nutriments.get('vitamin-a_100g', 0) * multiplier, 2),
                'vitamin_c': round(nutriments.get('vitamin-c_100g', 0) * multiplier, 2),
                'calcium': round(nutriments.get('calcium_100g', 0) * multiplier, 2),
                'iron': round(nutriments.get('iron_100g', 0) * multiplier, 2)
            },
            'nutrition_grade': product.get('nutrition_grades', 'unknown'),
            'image_url': product.get('image_url', ''),
            'source': 'Open Food Facts'
        }
    
    def _get_default_nutrition(self, food_name, multiplier):
        """
        Fallback nutrition data when API fails
        Based on USDA average values for common foods
        """
        default_values = {
            'apple': {'calories': 52, 'proteins': 0.3, 'carbs': 14, 'fats': 0.2},
            'banana': {'calories': 89, 'proteins': 1.1, 'carbs': 23, 'fats': 0.3},
            'bread': {'calories': 265, 'proteins': 9, 'carbs': 49, 'fats': 3.2},
            'broccoli': {'calories': 34, 'proteins': 2.8, 'carbs': 7, 'fats': 0.4},
            'burger': {'calories': 295, 'proteins': 17, 'carbs': 24, 'fats': 14},
            'chicken': {'calories': 165, 'proteins': 31, 'carbs': 0, 'fats': 3.6},
            'egg': {'calories': 155, 'proteins': 13, 'carbs': 1.1, 'fats': 11},
            'fish': {'calories': 206, 'proteins': 22, 'carbs': 0, 'fats': 12},
            'pasta': {'calories': 131, 'proteins': 5, 'carbs': 25, 'fats': 1.1},
            'pizza': {'calories': 266, 'proteins': 11, 'carbs': 33, 'fats': 10},
            'rice': {'calories': 130, 'proteins': 2.7, 'carbs': 28, 'fats': 0.3},
            'salad': {'calories': 15, 'proteins': 1.2, 'carbs': 3, 'fats': 0.2},
        }
        
        food_lower = food_name.lower()
        values = default_values.get(food_lower, {'calories': 100, 'proteins': 5, 'carbs': 15, 'fats': 3})
        
        return {
            'food_name': food_name,
            'brand': '',
            'serving_size': '100g',
            'calories': round(values['calories'] * multiplier, 1),
            'proteins': round(values['proteins'] * multiplier, 1),
            'carbohydrates': round(values['carbs'] * multiplier, 1),
            'fats': round(values['fats'] * multiplier, 1),
            'fiber': round(2.5 * multiplier, 1),
            'sugars': round(5 * multiplier, 1),
            'sodium': round(150 * multiplier, 1),
            'vitamins': {
                'vitamin_a': 0,
                'vitamin_c': 0,
                'calcium': 0,
                'iron': 0
            },
            'nutrition_grade': 'unknown',
            'image_url': '',
            'source': 'Default Database'
        }
    
    def get_dietary_recommendations(self, total_nutrition, user_profile):
        """
        Generate personalized dietary recommendations
        
        Args:
            total_nutrition: dict with totals
            user_profile: dict with user info (age, weight, activity_level, goals)
        
        Returns:
            dict with recommendations
        """
        recommendations = []
        warnings = []
        
        # Calculate daily recommended values based on user profile
        tdee = self._calculate_tdee(user_profile)
        
        # Calorie analysis
        calories = total_nutrition.get('calories', 0)
        if calories > tdee * 0.4:  # More than 40% of daily needs in one meal
            warnings.append("High calorie content for a single meal")
        elif calories < tdee * 0.15:
            warnings.append("Low calorie content - consider adding more food")
        
        # Protein analysis
        proteins = total_nutrition.get('proteins', 0)
        if proteins < 15:
            recommendations.append("Add more protein sources (chicken, fish, eggs, legumes)")
        elif proteins > 50:
            warnings.append("Very high protein content")
        
        # Carbs analysis
        carbs = total_nutrition.get('carbohydrates', 0)
        if carbs > 80:
            warnings.append("High carbohydrate content")
            recommendations.append("Consider reducing refined carbs and adding vegetables")
        
        # Fat analysis
        fats = total_nutrition.get('fats', 0)
        if fats > 30:
            warnings.append("High fat content")
        
        # Fiber analysis
        fiber = total_nutrition.get('fiber', 0)
        if fiber < 5:
            recommendations.append("Add more fiber (whole grains, vegetables, fruits)")
        
        # Sodium analysis
        sodium = total_nutrition.get('sodium', 0)
        if sodium > 800:
            warnings.append("High sodium content - stay hydrated")
        
        return {
            'tdee': tdee,
            'meal_percentage': round((calories / tdee) * 100, 1) if tdee > 0 else 0,
            'recommendations': recommendations,
            'warnings': warnings,
            'health_score': self._calculate_health_score(total_nutrition)
        }
    
    def _calculate_tdee(self, profile):
        """
        Calculate Total Daily Energy Expenditure
        Using Mifflin-St Jeor equation
        """
        age = profile.get('age', 30)
        weight = profile.get('weight', 70)  # kg
        height = profile.get('height', 170)  # cm
        gender = profile.get('gender', 'male')
        activity = profile.get('activity_level', 'moderate')
        
        # Base Metabolic Rate (BMR)
        if gender == 'male':
            bmr = 10 * weight + 6.25 * height - 5 * age + 5
        else:
            bmr = 10 * weight + 6.25 * height - 5 * age - 161
        
        # Activity multipliers
        activity_multipliers = {
            'sedentary': 1.2,
            'light': 1.375,
            'moderate': 1.55,
            'active': 1.725,
            'very_active': 1.9
        }
        
        tdee = bmr * activity_multipliers.get(activity, 1.55)
        return round(tdee)
    
    def _calculate_health_score(self, nutrition):
        """
        Calculate a simple health score (0-100)
        Based on nutritional balance
        """
        score = 100
        
        # Penalize high calories
        calories = nutrition.get('calories', 0)
        if calories > 800:
            score -= 10
        
        # Reward good protein content
        proteins = nutrition.get('proteins', 0)
        if 15 <= proteins <= 40:
            score += 5
        
        # Reward fiber
        fiber = nutrition.get('fiber', 0)
        if fiber >= 5:
            score += 5
        
        # Penalize high fat
        fats = nutrition.get('fats', 0)
        if fats > 30:
            score -= 10
        
        # Penalize high sodium
        sodium = nutrition.get('sodium', 0)
        if sodium > 1000:
            score -= 10
        
        return max(0, min(100, score))