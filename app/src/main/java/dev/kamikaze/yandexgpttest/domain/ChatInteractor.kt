package dev.kamikaze.yandexgpttest.domain

import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.YandexApi

class ChatInteractor(private val api: YandexApi) {

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<MessageRequest.Message>,
    ): String {
        val apiHistory = conversationHistory.map { message ->
            MessageRequest.Message(
                role = message.role,
                text = message.text
            )
        }

        // Делаем сравнение
        return api.compareTemperatures(userMessage, apiHistory)
    }
}