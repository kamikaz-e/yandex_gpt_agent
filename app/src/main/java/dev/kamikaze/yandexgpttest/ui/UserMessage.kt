package dev.kamikaze.yandexgpttest.ui

import dev.kamikaze.yandexgpttest.data.TokenStats

data class UserMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val tokens: TokenStats? = null
)