package dev.kamikaze.yandexgpttest.ui

import dev.kamikaze.yandexgpttest.data.TokenStats
import kotlinx.serialization.Serializable

@Serializable
data class UserMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val tokens: TokenStats? = null,
    val isSummary: Boolean = false,
    val originalMessagesCount: Int = 0
)