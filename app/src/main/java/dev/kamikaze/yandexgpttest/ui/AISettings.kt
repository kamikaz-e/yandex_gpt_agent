package dev.kamikaze.yandexgpttest.ui

import dev.kamikaze.yandexgpttest.data.prompt.ResponseFormat
import dev.kamikaze.yandexgpttest.data.prompt.ResponseStyle

data class AISettings(
    val responseFormat: ResponseFormat = ResponseFormat.JSON,
    val responseStyle: ResponseStyle = ResponseStyle.NEUTRAL,
    val maxLength: Int = 500,
    val maxQuestions: Int = 2,
)
