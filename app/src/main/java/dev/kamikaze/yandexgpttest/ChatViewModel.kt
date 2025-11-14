package dev.kamikaze.yandexgpttest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kamikaze.yandexgpttest.data.CompactionConfig
import dev.kamikaze.yandexgpttest.data.CompactionStats
import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.TokenStats
import dev.kamikaze.yandexgpttest.data.YandexApi
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

    private val _totalTokenStats = MutableStateFlow(TokenStats())
    val totalTokenStats: StateFlow<TokenStats> = _totalTokenStats.asStateFlow()

    // ← По умолчанию ВЫКЛЮЧЕНО для сравнения
    private val _compactionConfig = MutableStateFlow(
        CompactionConfig(
            enabled = false,  // Изначально выключено
            messagesThreshold = 10
        )
    )
    val compactionConfig: StateFlow<CompactionConfig> = _compactionConfig.asStateFlow()

    private val _compactionStats = MutableStateFlow(CompactionStats())
    val compactionStats: StateFlow<CompactionStats> = _compactionStats.asStateFlow()

    fun sendMessage(message: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val userMessage = UserMessage(
                    id = _messages.value.count(),
                    text = message,
                    isUser = true,
                    tokens = null
                )
                _messages.value += userMessage

                // Формируем историю (с учетом включенной/выключенной компрессии)
                val conversationHistory = buildConversationHistory()

                val apiResponse = chatInteractor.sendMessage(message, conversationHistory)

                val assistantMessage = UserMessage(
                    id = _messages.value.count(),
                    text = apiResponse.text,
                    isUser = false,
                    tokens = apiResponse.tokens
                )
                _messages.value += assistantMessage

                updateTotalTokens(apiResponse.tokens)

                // ← ПРОВЕРЯЕМ только если компрессия ВКЛЮЧЕНА
                if (_compactionConfig.value.enabled) {
                    checkAndCompressHistory()
                }

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

    private fun buildConversationHistory(): List<MessageRequest.Message> {
        val history = mutableListOf<MessageRequest.Message>()

        _messages.value.forEach { message ->
            when {
                // Summary только если компрессия ВКЛЮЧЕНА
                message.isSummary && _compactionConfig.value.enabled -> {
                    history.add(
                        MessageRequest.Message(
                            role = "system",
                            text = "Контекст предыдущего диалога (резюме ${message.originalMessagesCount} сообщений):\n${message.text}"
                        )
                    )
                }
                // Обычные сообщения
                message.isUser && !message.isSummary -> {
                    history.add(
                        MessageRequest.Message(
                            role = "user",
                            text = message.text
                        )
                    )
                }

                !message.isUser && !message.isSummary -> {
                    history.add(
                        MessageRequest.Message(
                            role = "assistant",
                            text = message.text
                        )
                    )
                }
            }
        }

        return history
    }

    private suspend fun checkAndCompressHistory() {
        // Двойная проверка
        if (!_compactionConfig.value.enabled) return

        val regularMessages = _messages.value.filter { !it.isSummary }

        if (regularMessages.size >= _compactionConfig.value.messagesThreshold) {
            compressHistory()
        }
    }

    private suspend fun compressHistory() {
        val existingSummaries = _messages.value.filter { it.isSummary }
        val messagesToCompress = _messages.value.filter { !it.isSummary }

        if (messagesToCompress.size < _compactionConfig.value.messagesThreshold) return

        val messagesForSummary = messagesToCompress.take(_compactionConfig.value.messagesThreshold)
        val remainingMessages = messagesToCompress.drop(_compactionConfig.value.messagesThreshold)

        val tokensBeforeCompression = messagesForSummary
            .mapNotNull { it.tokens?.totalTokens }
            .sum()

        val historyForSummary = messagesForSummary.map {
            MessageRequest.Message(
                role = if (it.isUser) "user" else "assistant",
                text = it.text
            )
        }

        val summaryResponse = YandexApi.createSummary(historyForSummary)

        val summaryMessage = UserMessage(
            id = existingSummaries.size,
            text = summaryResponse.text,
            isUser = false,
            tokens = summaryResponse.tokens,
            isSummary = true,
            originalMessagesCount = messagesForSummary.size
        )

        _messages.value = existingSummaries + listOf(summaryMessage) + remainingMessages

        val tokensSaved = tokensBeforeCompression - (summaryResponse.tokens.totalTokens)

        _compactionStats.value = CompactionStats(
            originalMessages = _compactionStats.value.originalMessages + messagesForSummary.size,
            compressedMessages = _compactionStats.value.compressedMessages + 1,
            tokensSaved = _compactionStats.value.tokensSaved + tokensSaved
        )

        updateTotalTokens(summaryResponse.tokens)
    }

    // ← ОБНОВЛЕНО: теперь с сообщением в UI
    fun toggleCompaction() {
        val newState = !_compactionConfig.value.enabled
        _compactionConfig.value = _compactionConfig.value.copy(
            enabled = newState
        )

        // При включении компрессии - проверяем сразу
        if (newState) {
            viewModelScope.launch {
                checkAndCompressHistory()
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
        _totalTokenStats.value = TokenStats()
        _compactionStats.value = CompactionStats()
    }
}