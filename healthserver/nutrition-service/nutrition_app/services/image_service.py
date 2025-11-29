import cv2
import numpy as np
from PIL import Image
import io

class ImageService:
    @staticmethod
    def preprocess_image(image_data, target_size=(224, 224)):
        """
        Preprocess image for ML model
        
        Args:
            image_data: bytes or PIL Image
            target_size: tuple (width, height)
        
        Returns:
            numpy array ready for ML model
        """
        try:
            # Convert to PIL Image if bytes
            if isinstance(image_data, bytes):
                image = Image.open(io.BytesIO(image_data))
            else:
                image = image_data
            
            # Convert to RGB if needed
            if image.mode != 'RGB':
                image = image.convert('RGB')
            
            # Resize
            image = image.resize(target_size, Image.Resampling.LANCZOS)
            
            # Convert to numpy array
            img_array = np.array(image)
            
            # Normalize pixel values to [0, 1]
            img_array = img_array.astype('float32') / 255.0
            
            # Add batch dimension
            img_array = np.expand_dims(img_array, axis=0)
            
            return img_array
        
        except Exception as e:
            raise Exception(f"Error preprocessing image: {str(e)}")
    
    @staticmethod
    def enhance_image(image_data):
        """
        Enhance image quality (brightness, contrast, sharpness)
        
        Returns:
            enhanced image as bytes
        """
        try:
            # Convert to PIL Image
            if isinstance(image_data, bytes):
                image = Image.open(io.BytesIO(image_data))
            else:
                image = image_data
            
            # Convert to OpenCV format
            img_cv = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)
            
            # Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
            lab = cv2.cvtColor(img_cv, cv2.COLOR_BGR2LAB)
            l, a, b = cv2.split(lab)
            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
            l = clahe.apply(l)
            lab = cv2.merge([l, a, b])
            img_enhanced = cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)
            
            # Apply sharpening
            kernel = np.array([[-1,-1,-1],
                             [-1, 9,-1],
                             [-1,-1,-1]])
            img_sharpened = cv2.filter2D(img_enhanced, -1, kernel)
            
            # Convert back to PIL Image
            img_final = Image.fromarray(cv2.cvtColor(img_sharpened, cv2.COLOR_BGR2RGB))
            
            # Convert to bytes
            buffer = io.BytesIO()
            img_final.save(buffer, format='JPEG', quality=95)
            return buffer.getvalue()
        
        except Exception as e:
            raise Exception(f"Error enhancing image: {str(e)}")
    
    @staticmethod
    def validate_image(image_data, max_size_mb=10):
        """
        Validate image format and size
        
        Returns:
            tuple (is_valid, error_message)
        """
        try:
            if isinstance(image_data, bytes):
                image = Image.open(io.BytesIO(image_data))
                size_mb = len(image_data) / (1024 * 1024)
            else:
                image = image_data
                buffer = io.BytesIO()
                image.save(buffer, format='JPEG')
                size_mb = len(buffer.getvalue()) / (1024 * 1024)
            
            # Check size
            if size_mb > max_size_mb:
                return False, f"Image size ({size_mb:.2f}MB) exceeds maximum ({max_size_mb}MB)"
            
            # Check format
            if image.format not in ['JPEG', 'JPG', 'PNG']:
                return False, f"Unsupported format: {image.format}. Use JPEG or PNG"
            
            # Check dimensions
            width, height = image.size
            if width < 100 or height < 100:
                return False, f"Image too small ({width}x{height}). Minimum 100x100 pixels"
            
            return True, None
        
        except Exception as e:
            return False, f"Invalid image: {str(e)}"
    
    @staticmethod
    def compress_image(image_data, quality=85, max_dimension=1920):
        """
        Compress image to reduce size
        
        Returns:
            compressed image as bytes
        """
        try:
            if isinstance(image_data, bytes):
                image = Image.open(io.BytesIO(image_data))
            else:
                image = image_data
            
            # Resize if too large
            width, height = image.size
            if max(width, height) > max_dimension:
                ratio = max_dimension / max(width, height)
                new_size = (int(width * ratio), int(height * ratio))
                image = image.resize(new_size, Image.Resampling.LANCZOS)
            
            # Compress
            buffer = io.BytesIO()
            image.save(buffer, format='JPEG', quality=quality, optimize=True)
            return buffer.getvalue()
        
        except Exception as e:
            raise Exception(f"Error compressing image: {str(e)}")