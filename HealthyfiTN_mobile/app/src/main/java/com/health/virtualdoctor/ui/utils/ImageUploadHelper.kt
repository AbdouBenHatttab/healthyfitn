package com.health.virtualdoctor.ui.utils

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Helper pour uploader les images vers Cloudinary
 *
 * GRATUIT jusqu'à 25GB de stockage et 25GB de bande passante/mois
 *
 * Setup:
 * 1. Créer un compte sur cloudinary.com
 * 2. Récupérer vos credentials (cloud_name, upload_preset)
 * 3. Créer un "unsigned upload preset" dans les settings
 */
object ImageUploadHelper {

    // ⚙️ CONFIGURATION - À remplacer par vos valeurs Cloudinary
    private const val CLOUD_NAME = "dqvs55wsh"  // Ex: "dxyz123"
    private const val UPLOAD_PRESET = "doctor_profiles"  // Créer dans Cloudinary settings
    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val client = OkHttpClient()

    /**
     * Upload une image vers Cloudinary
     *
     * @param bitmap L'image à uploader
     * @param folder Dossier dans Cloudinary (ex: "doctors", "patients")
     * @return L'URL publique de l'image uploadée, ou null si échec
     */
    suspend fun uploadImage(
        bitmap: Bitmap,
        folder: String = "doctors"
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("ImageUpload", "🔄 Starting image upload...")

            // 1️⃣ Convertir le Bitmap en ByteArray
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()

            Log.d("ImageUpload", "📦 Image size: ${byteArray.size / 1024} KB")

            // 2️⃣ Créer la requête multipart
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("folder", folder)
                .addFormDataPart(
                    "file",
                    "profile.jpg",
                    byteArray.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            // 3️⃣ Envoyer la requête
            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")
                val imageUrl = json.optString("secure_url")

                Log.d("ImageUpload", "✅ Upload successful: $imageUrl")
                return@withContext imageUrl
            } else {
                Log.e("ImageUpload", "❌ Upload failed: ${response.code}")
                return@withContext null
            }

        } catch (e: IOException) {
            Log.e("ImageUpload", "❌ Network error: ${e.message}", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e("ImageUpload", "❌ Upload error: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Upload une image et afficher la progression
     */
    suspend fun uploadImageWithProgress(
        bitmap: Bitmap,
        folder: String = "doctors",
        onProgress: (Int) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        try {
            onProgress(10) // Début

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()

            onProgress(30) // Compression terminée

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("folder", folder)
                .addFormDataPart(
                    "file",
                    "profile.jpg",
                    byteArray.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            onProgress(50) // Requête préparée

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            onProgress(90) // Upload terminé

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")
                val imageUrl = json.optString("secure_url")

                onProgress(100) // Terminé
                return@withContext imageUrl
            } else {
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e("ImageUpload", "❌ Error: ${e.message}", e)
            return@withContext null
        }
    }
}