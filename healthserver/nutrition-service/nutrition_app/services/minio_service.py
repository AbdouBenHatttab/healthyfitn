from minio import Minio
from minio.error import S3Error
from nutrition_app.config import Config
import uuid
from datetime import timedelta
import io

class MinIOService:
    def __init__(self):
        self.client = Minio(
            Config.MINIO_ENDPOINT,
            access_key=Config.MINIO_ACCESS_KEY,
            secret_key=Config.MINIO_SECRET_KEY,
            secure=Config.MINIO_SECURE
        )
        self.bucket_name = Config.MINIO_BUCKET
        self._ensure_bucket_exists()
    
    def _ensure_bucket_exists(self):
        """Create bucket if it doesn't exist"""
        try:
            if not self.client.bucket_exists(self.bucket_name):
                self.client.make_bucket(self.bucket_name)
                print(f"✅ Bucket '{self.bucket_name}' created")
        except S3Error as e:
            print(f"❌ Error creating bucket: {e}")
    
    def upload_image(self, image_data, user_id, filename=None):
        """
        Upload image to MinIO
        
        Args:
            image_data: bytes or file-like object
            user_id: string
            filename: optional original filename
        
        Returns:
            dict with object_name and url
        """
        try:
            # Generate unique object name
            if filename:
                extension = filename.rsplit('.', 1)[-1] if '.' in filename else 'jpg'
            else:
                extension = 'jpg'
            
            object_name = f"{user_id}/{uuid.uuid4()}.{extension}"
            
            # Convert to bytes if needed
            if isinstance(image_data, bytes):
                image_stream = io.BytesIO(image_data)
                length = len(image_data)
            else:
                image_stream = image_data
                image_data.seek(0, 2)  # Seek to end
                length = image_data.tell()
                image_data.seek(0)  # Seek back to start
            
            # Upload to MinIO
            self.client.put_object(
                self.bucket_name,
                object_name,
                image_stream,
                length,
                content_type='image/jpeg'
            )
            
            # Generate presigned URL (valid for 7 days)
            url = self.client.presigned_get_object(
                self.bucket_name,
                object_name,
                expires=timedelta(days=7)
            )
            
            return {
                'object_name': object_name,
                'url': url,
                'bucket': self.bucket_name
            }
        
        except S3Error as e:
            raise Exception(f"Error uploading image: {str(e)}")
    
    def get_image(self, object_name):
        """
        Get image from MinIO
        
        Returns:
            bytes
        """
        try:
            response = self.client.get_object(self.bucket_name, object_name)
            return response.read()
        except S3Error as e:
            raise Exception(f"Error retrieving image: {str(e)}")
        finally:
            if 'response' in locals():
                response.close()
                response.release_conn()
    
    def delete_image(self, object_name):
        """Delete image from MinIO"""
        try:
            self.client.remove_object(self.bucket_name, object_name)
            return True
        except S3Error as e:
            raise Exception(f"Error deleting image: {str(e)}")
    
    def get_presigned_url(self, object_name, expires_days=7):
        """Get presigned URL for image"""
        try:
            url = self.client.presigned_get_object(
                self.bucket_name,
                object_name,
                expires=timedelta(days=expires_days)
            )
            return url
        except S3Error as e:
            raise Exception(f"Error generating URL: {str(e)}")