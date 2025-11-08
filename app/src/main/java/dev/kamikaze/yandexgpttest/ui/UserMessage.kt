package dev.kamikaze.yandexgpttest.ui

data class UserMessage(
    val id : Int,
    val text: String,
    val isUser: Boolean
)