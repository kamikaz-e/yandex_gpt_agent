package dev.kamikaze.yandexgpttest.data

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    val result: Result? = null,
    val error: ErrorInfo? = null, // Добавляем обработку ошибок
    val code: Int? = null,
    val message: String? = null,
) {
    @Serializable
    data class Result(
        val alternatives: List<Alternative> = emptyList(),
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
    data class ErrorInfo(
        val code: String,
        val message: String,
        val details: String? = null,
    )
}