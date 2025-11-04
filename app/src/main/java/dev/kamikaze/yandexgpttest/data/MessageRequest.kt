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
        val temperature: Float = 0.7f,
        val maxTokens: Int = 512,
    )

    @Serializable
    data class Message(
        val role: String,
        val text: String,
    )
}
