package com.securedoc.ai.data.remote

data class GroqRequest(
    val model: String = "llama-3.1-8b-instant",
    val messages: List<Message>
) {
    data class Message(
        val role: String,
        val content: String
    )
}

data class GroqResponse(
    val choices: List<Choice>?
) {
    data class Choice(
        val message: Message?
    )
    data class Message(
        val content: String?
    )
}