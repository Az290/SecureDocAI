package com.securedoc.ai.data.remote

import com.securedoc.ai.BuildConfig
import javax.inject.Inject

class DocumentRepository @Inject constructor(
    private val classifierApi: ClassifierApi,
    private val groqApi: GroqApi
) {

    // Bước 1: Phân loại bằng Python ML
    suspend fun classifyDocument(text: String): ClassifyResponse {
        return classifierApi.classify(ClassifyRequest(text))
    }

    // Bước 2: Tóm tắt bằng Groq API
    suspend fun summarizeDocument(text: String, docType: String): String {
        val prompt = buildPrompt(docType, text)

        val request = GroqRequest(
            messages = listOf(
                GroqRequest.Message(role = "user", content = prompt)
            )
        )

        val response = groqApi.classify(
            authorization = "Bearer ${BuildConfig.GROQ_API_KEY}",
            request = request
        )

        return response.choices?.firstOrNull()?.message?.content
            ?: "Không thể tóm tắt"
    }

    private fun buildPrompt(docType: String, text: String): String {
        return when(docType) {
            "HÓA ĐƠN" -> """
                Trích xuất thông tin hóa đơn sau dưới dạng:
                - Tên công ty:
                - Mã số thuế:
                - Số hóa đơn:
                - Ngày:
                - Tổng tiền:
                - Thuế VAT:
                
                Nội dung:
                $text
            """.trimIndent()

            "HỢP ĐỒNG" -> """
                Tóm tắt hợp đồng sau:
                - Loại hợp đồng:
                - Bên A (người bán/cho thuê):
                - Bên B (người mua/thuê):
                - Thời hạn:
                - Giá trị/lương:
                - Điều khoản chính:
                
                Nội dung:
                $text
            """.trimIndent()

            "CMND" -> """
                Trích xuất thông tin CMND/CCCD:
                - Số CMND/CCCD:
                - Họ và tên:
                - Ngày sinh:
                - Giới tính:
                - Quê quán:
                - Nơi thường trú:
                - Ngày cấp:
                - Nơi cấp:
                
                Nội dung:
                $text
            """.trimIndent()

            else -> """
                Tóm tắt nội dung chính của tài liệu sau:
                $text
            """.trimIndent()
        }
    }

    suspend fun checkServerHealth(): Boolean {
        return try {
            val response = classifierApi.health()
            response.status == "healthy"
        } catch (e: Exception) {
            false
        }
    }
}