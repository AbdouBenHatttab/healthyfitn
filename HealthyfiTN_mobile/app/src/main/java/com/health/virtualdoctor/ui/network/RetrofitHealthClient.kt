package com.health.virtualdoctor.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.health.virtualdoctor.BuildConfig

object RetrofitHealthClient {

    val cloudflared = BuildConfig.CLOUDFLARED_URL
    private val BASE_URL = "$cloudflared/model-ai-service/" //PORT 8000

    // 📝 Logging interceptor pour debug
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 🌐 Configuration OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 🚀 Instance Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 📡 Service API
    val apiHealthService: ApiHealthService by lazy {
        retrofit.create(ApiHealthService::class.java)
    }
}