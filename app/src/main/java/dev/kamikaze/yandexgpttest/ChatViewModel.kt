package dev.kamikaze.yandexgpttest

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.ParsedResponse
import dev.kamikaze.yandexgpttest.domain.ChatInteractor
import dev.kamikaze.yandexgpttest.ui.AISettings
import dev.kamikaze.yandexgpttest.ui.UserMessage
import dev.kamikaze.yandexgpttest.ui.theme.isValidJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ChatViewModel(private val chatInteractor: ChatInteractor) : ViewModel() {

    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _settings = MutableStateFlow(AISettings())
    val settings: StateFlow<AISettings> = _settings.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private var userMessageCount = 0

    fun toggleSettingsSheet() {
        _showSettingsSheet.value = !_showSettingsSheet.value
    }

    fun updateSettings(newSettings: AISettings) {
        _settings.value = newSettings
    }

    private val maxMessages = 20 // Максимум сообщений
    private val maxTextLength = 5000 // Максимум символов в одном сообщении

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = UserMessage(
            text = text.take(maxTextLength),
            isUser = true,
            id = _messages.value.count()
        )
        _messages.value += userMessage
        userMessageCount++

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val conversationHistory = _messages.value.map { message ->
                    MessageRequest.Message(
                        role = if (message.isUser) "user" else "assistant",
                        text = message.text
                    )
                }

                val needTotalResult = userMessageCount > _settings.value.maxQuestions
                Log.d("mike_t", "userMessageCount: $userMessageCount")
                Log.d("mike_t", "_settings.value.maxQuestions: ${_settings.value.maxQuestions}")
                Log.d("mike_t", "conversationHistory: ${conversationHistory}")
                if (needTotalResult) {
                    userMessageCount = 0
                }
                val response = chatInteractor.sendMessage(conversationHistory, _settings.value, needTotalResult)
                val botMessage = UserMessage(
                    text = truncateText(response),
                    isUser = false,
                    id = _messages.value.count()
                )
                _messages.value = (_messages.value + botMessage).takeLast(maxMessages)
            } catch (e: Exception) {
                val errorMessage = UserMessage(
                    text = "Ошибка: ${e.message}",
                    isUser = false,
                    id = _messages.value.count()
                )
                _messages.value = (_messages.value + errorMessage).takeLast(maxMessages)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun truncateText(text: String): String {
        return if (text.length > maxTextLength) {
            text.take(maxTextLength) + "\n[Урезано...]"
        } else {
            text
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
        userMessageCount = 0
        _messages.value = emptyList()
    }

    // Улучшенная функция анализа AI сообщения
    fun analyzeAIMessage(text: String): AIDisplayType {
        return try {
            // Пробуем извлечь JSON из текста
            val jsonText = extractJsonFromText(text)
            val textToParse = jsonText ?: text

            if (isValidJson(textToParse)) {
                val parsedResponse = Json.decodeFromString<ParsedResponse>(textToParse)
                AIDisplayType.StructuredJson(parsedResponse)
            } else {
                AIDisplayType.RegularText(text)
            }
        } catch (e: Exception) {
            AIDisplayType.RegularText(text)
        }
    }

    private fun extractJsonFromText(text: String): String? {
        val jsonPattern = """\{[\s\S]*\}""".toRegex()
        val match = jsonPattern.find(text)

        return match?.value?.replace("```json", "")?.replace("```", "")?.trim()
    }
}

sealed class AIDisplayType {
    data class StructuredJson(val parsedResponse: ParsedResponse) : AIDisplayType()
    data class RegularText(val text: String) : AIDisplayType()
}