package dev.kamikaze.yandexgpttest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.domain.ChatInteractor
import dev.kamikaze.yandexgpttest.ui.UserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val chatInteractor: ChatInteractor) : ViewModel() {

    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    fun sendMessage(message: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Сохраняем сообщение пользователя
                val userMessage = UserMessage(
                    id = _messages.value.count(),
                    text = message,
                    isUser = true
                )
                _messages.value += userMessage

                val conversationHistory = _messages.value
                    .filter { it.id != userMessage.id } // Исключаем новое сообщение
                    .map {
                        MessageRequest.Message(
                            role = if (it.isUser) "user" else "assistant",
                            text = it.text
                        )
                    }

                val response = chatInteractor.sendMessage(message, conversationHistory)

                // Добавляем ответ в чат
                val assistantMessage = UserMessage(
                    id = _messages.value.count(),
                    text = response,
                    isUser = false
                )
                _messages.value += assistantMessage

            } catch (e: Exception) {
                // Обработка ошибки
                val errorMessage = UserMessage(
                    id = _messages.value.count(),
                    text = "Ошибка при анализе: ${e.message}",
                    isUser = false
                )
                _messages.value += errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showDeleteConfirmationDialog() {
        _showDeleteDialog.value = true
    }

    fun confirmDeleteChat() {
        clearChat()
        _showDeleteDialog.value = false
    }

    fun cancelDeleteChat() {
        _showDeleteDialog.value = false
    }

    private fun clearChat() {
        _messages.value = emptyList()
    }
}