package com.securedoc.ai.data.remote

import com.securedoc.ai.BuildConfig
import javax.inject.Inject

class GroqRepository @Inject constructor(
    private val api: GroqApi
) {
    suspend fun classifyDocument(text: String): String {
        android.util.Log.d("GROQ_DEBUG", "API Key: '${BuildConfig.GROQ_API_KEY}'")
        val prompt = """
            Phân loại tài liệu sau vào một trong các loại: HÓA ĐƠN, HỢP ĐỒNG, CMND, KHÁC.
            Chỉ trả về đúng một trong bốn từ đó, không giải thích thêm.
            
            Nội dung tài liệu:
            $text
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqRequest.Message(role = "user", content = prompt)
            )
        )

        val response = api.classify(
            authorization = "Bearer ${BuildConfig.GROQ_API_KEY}",
            request = request
        )

        return response.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?: "Không xác định"
    }
}