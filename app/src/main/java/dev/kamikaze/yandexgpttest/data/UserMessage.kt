package dev.kamikaze.yandexgpttest.data

data class UserMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)