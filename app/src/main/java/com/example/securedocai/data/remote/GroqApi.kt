package com.securedoc.ai.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun classify(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): GroqResponse
}