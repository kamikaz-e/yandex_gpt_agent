package dev.kamikaze.yandexgpttest.data

import kotlinx.serialization.Serializable

@Serializable
data class MessageRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>,
    val json_object: Boolean,
) {
    @Serializable
    data class CompletionOptions(
        val stream: Boolean = false,
        val temperature: Float = 0.3f,
        val maxTokens: Int = 500,
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
    val message: String? = null
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


@Serializable
data class ParsedResponse(
    val summary: String = "",
    val explanation: String = "",
    val references: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)