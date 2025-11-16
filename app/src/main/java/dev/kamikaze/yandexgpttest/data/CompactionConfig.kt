package dev.kamikaze.yandexgpttest.data

import kotlinx.serialization.Serializable

@Serializable
data class CompactionConfig(
    val enabled: Boolean = true,
    val messagesThreshold: Int = 10 // Каждые 10 сообщений делаем summary
)

@Serializable
data class CompactionStats(
    val originalMessages: Int = 0,
    val compressedMessages: Int = 0,
    val tokensSaved: Int = 0
)