package dev.kamikaze.yandexgpttest.domain

import dev.kamikaze.yandexgpttest.data.ApiClient
import dev.kamikaze.yandexgpttest.data.ApiResponse
import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.UserProfile

class ChatInteractor(private val apiClient: ApiClient) {

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<MessageRequest.Message>,
        userProfile: UserProfile? = null
    ): ApiResponse {
        return apiClient.sendMessage(userMessage, conversationHistory, userProfile)
    }

    suspend fun createSummary(
        messages: List<MessageRequest.Message>
    ): ApiResponse {
        return apiClient.createSummary(messages)
    }
}