package dev.kamikaze.yandexgpttest.domain

import dev.kamikaze.yandexgpttest.data.ApiResponse
import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.UserProfile
import dev.kamikaze.yandexgpttest.data.YandexApi

class ChatInteractor(private val api: YandexApi) {

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<MessageRequest.Message>,
        userProfile: UserProfile? = null
    ): ApiResponse {
        return api.sendMessage(userMessage, conversationHistory, userProfile)
    }
}