package com.health.virtualdoctor.ui.data.api

import retrofit2.Response
import retrofit2.http.*

interface WebRTCApiService {

    @POST("api/webrtc/initiate")
    suspend fun initiateCall(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<CallSessionResponse>

    @POST("api/webrtc/calls/{callId}/offer")
    suspend fun sendOffer(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body sdp: Map<String, String>
    ): Response<Void>

    @POST("api/webrtc/calls/{callId}/answer")
    suspend fun sendAnswer(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body sdp: Map<String, String>
    ): Response<Void>

    @POST("/calls/{callId}/ice-candidate")
    suspend fun sendIceCandidate(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body candidateData: Map<String, Any>  // ✅ NO WILDCARD!
    ): Response<Void>
    @GET("api/webrtc/calls/{callId}")
    suspend fun getCallSession(
        @Header("Authorization") token: String,
        @Path("callId") callId: String
    ): Response<CallSessionResponse>

    @GET("api/webrtc/calls/{callId}/quality")
    suspend fun getCallQuality(
        @Header("Authorization") token: String,
        @Path("callId") callId: String
    ): Response<Map<String, Any>>
    @POST("api/webrtc/calls/{callId}/end")
    suspend fun endCall(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body reason: Map<String, String>
    ): Response<Void>
    /**
     * Get existing call session for an appointment
     * This allows joining an ongoing call
     */
    @GET("api/webrtc/calls/appointment/{appointmentId}")
    suspend fun getExistingCallSession(
        @Header("Authorization") token: String,
        @Path("appointmentId") appointmentId: String
    ): Response<CallSessionResponse>
}

// Data classes

data class CallSessionResponse(
    val callId: String,
    val appointmentId: String,
    val doctorId: String,
    val doctorEmail: String,
    val patientId: String,
    val patientEmail: String,
    val callType: String,
    val status: String,
    val initiatorRole: String,
    val iceServers: String?,
    val offerSdp: String?,
    val answerSdp: String?,
    val createdAt: String
)