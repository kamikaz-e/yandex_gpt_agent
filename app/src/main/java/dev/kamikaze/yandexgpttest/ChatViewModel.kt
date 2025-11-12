package dev.kamikaze.yandexgpttest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.TokenStats
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

    // ← Добавляем накопительную статистику токенов
    private val _totalTokenStats = MutableStateFlow(TokenStats())
    val totalTokenStats: StateFlow<TokenStats> = _totalTokenStats.asStateFlow()

    fun sendMessage(message: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Сохраняем сообщение пользователя
                val userMessage = UserMessage(
                    id = _messages.value.count(),
                    text = message,
                    isUser = true,
                    tokens = null  // У пользовательского сообщения нет токенов
                )
                _messages.value += userMessage

                val conversationHistory = _messages.value
                    .filter { it.id != userMessage.id }
                    .map {
                        MessageRequest.Message(
                            role = if (it.isUser) "user" else "assistant",
                            text = it.text
                        )
                    }

                // Получаем ответ с токенами
                val apiResponse = chatInteractor.sendMessage(message, conversationHistory)

                // Добавляем ответ в чат с токенами
                val assistantMessage = UserMessage(
                    id = _messages.value.count(),
                    text = apiResponse.text,
                    isUser = false,
                    tokens = apiResponse.tokens
                )
                _messages.value += assistantMessage

                // Обновляем накопительную статистику
                updateTotalTokens(apiResponse.tokens)

            } catch (e: Exception) {
                val errorMessage = UserMessage(
                    id = _messages.value.count(),
                    text = "Ошибка при анализе: ${e.message}",
                    isUser = false,
                    tokens = null
                )
                _messages.value += errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateTotalTokens(newTokens: TokenStats) {
        _totalTokenStats.value = TokenStats(
            inputTokens = _totalTokenStats.value.inputTokens + newTokens.inputTokens,
            outputTokens = _totalTokenStats.value.outputTokens + newTokens.outputTokens,
            totalTokens = _totalTokenStats.value.totalTokens + newTokens.totalTokens
        )
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
        _totalTokenStats.value = TokenStats()  // Сбрасываем статистику
    }
}