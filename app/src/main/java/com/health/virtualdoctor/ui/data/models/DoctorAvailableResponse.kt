package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName

data class DoctorAvailableResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String?,

    @SerializedName("email")
    val email: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("fullName")
    val fullName: String?,

    @SerializedName("specialization")
    val specialization: String,

    @SerializedName("hospitalAffiliation")
    val hospitalAffiliation: String?,

    @SerializedName("yearsOfExperience")
    val yearsOfExperience: Int?,

    @SerializedName("isActivated")
    val isActivated: Boolean
)