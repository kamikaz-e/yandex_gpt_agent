package dev.kamikaze.yandexgpttest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kamikaze.yandexgpttest.data.UserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val yandexApi = YandexApi

    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Добавляем сообщение пользователя
        val userMessage = UserMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = yandexApi.sendMessage(text)
                val assistantUserMessage = UserMessage(text = response, isUser = false)
                _messages.value = _messages.value + assistantUserMessage
            } catch (e: Exception) {
                val errorUserMessage = UserMessage(
                    text = "Ошибка: ${e.message}",
                    isUser = false
                )
                _messages.value = _messages.value + errorUserMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}