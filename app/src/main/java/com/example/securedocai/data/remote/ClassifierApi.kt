package com.securedoc.ai.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ClassifierApi {

    @POST("classify")
    suspend fun classify(@Body request: ClassifyRequest): ClassifyResponse

    @GET("health")
    suspend fun health(): HealthResponse
}

data class ClassifyRequest(
    val text: String
)

data class ClassifyResponse(
    val label: String,
    val confidence: Double,
    val all_probabilities: Map<String, Double>? = null
)

data class HealthResponse(
    val status: String
)