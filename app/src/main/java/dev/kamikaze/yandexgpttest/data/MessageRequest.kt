package dev.kamikaze.yandexgpttest.data

import kotlinx.serialization.Serializable

@Serializable
data class MessageRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>,
) {
    @Serializable
    data class CompletionOptions(
        val stream: Boolean = false,
        val temperature: Float = 0.3f,
    )

    @Serializable
    data class Message(
        val role: String,
        val text: String,
    )
}

@Serializable
data class MessageResponse(
    val result: Result? = null,
    val error: ErrorInfo? = null,
    val code: Int? = null,
    val message: String? = null,
) {
    @Serializable
    data class Result(
        val alternatives: List<Alternative> = emptyList(),
        val usage: Usage? = null,  // ← ДОБАВЛЯЕМ информацию о токенах
        val modelVersion: String? = null
    )

    @Serializable
    data class Alternative(
        val message: Message,
        val status: String = "ALTERNATIVE_STATUS_OK",
    ) {
        @Serializable
        data class Message(
            val role: String,
            val text: String,
        )
    }

    @Serializable
    data class Usage(
        val inputTextTokens: String? = null,
        val completionTokens: String? = null,
        val totalTokens: String? = null
    )

    @Serializable
    data class ErrorInfo(
        val code: String? = null,
        val message: String? = null,
    )
}