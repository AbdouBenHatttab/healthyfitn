package com.health.virtualdoctor.ui.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

/**
 * ViewModel partagé entre UserMetricsActivity et ses fragments
 */
class HealthDataViewModel : ViewModel() {

    // LiveData pour les données de santé du jour
    private val _healthData = MutableLiveData<JSONObject?>()
    val healthData: LiveData<JSONObject?> = _healthData

    // LiveData pour l'état de chargement
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData pour les erreurs
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Met à jour les données de santé
     */
    fun updateHealthData(data: JSONObject?) {
        _healthData.value = data
    }

    /**
     * Définit l'état de chargement
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * Définit un message d'erreur
     */
    fun setError(message: String?) {
        _errorMessage.value = message
    }

    /**
     * Efface le message d'erreur
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Retourne les données actuelles (synchrone)
     */
    fun getCurrentHealthData(): JSONObject? = _healthData.value
}