package dev.kamikaze.yandexgpttest

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kamikaze.yandexgpttest.data.*
import dev.kamikaze.yandexgpttest.domain.ChatInteractor
import dev.kamikaze.yandexgpttest.ui.UserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatInteractor: ChatInteractor,
    private val applicationContext: Context  // Добавляем Context для работы с файлами
) : ViewModel() {

    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // ← НОВОЕ: состояние диалога очистки памяти
    private val _showClearMemoryDialog = MutableStateFlow(false)
    val showClearMemoryDialog: StateFlow<Boolean> = _showClearMemoryDialog.asStateFlow()

    private val _totalTokenStats = MutableStateFlow(TokenStats())
    val totalTokenStats: StateFlow<TokenStats> = _totalTokenStats.asStateFlow()

    private val _compactionConfig = MutableStateFlow(
        CompactionConfig(
            enabled = false,
            messagesThreshold = 10
        )
    )
    val compactionConfig: StateFlow<CompactionConfig> = _compactionConfig.asStateFlow()

    private val _compactionStats = MutableStateFlow(CompactionStats())
    val compactionStats: StateFlow<CompactionStats> = _compactionStats.asStateFlow()

    // Состояния для работы с памятью
    private val _hasSavedData = MutableStateFlow(false)
    val hasSavedData: StateFlow<Boolean> = _hasSavedData.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    private val _isLoadingFromMemory = MutableStateFlow(false)
    val isLoadingFromMemory: StateFlow<Boolean> = _isLoadingFromMemory.asStateFlow()

    init {
        // При инициализации пытаемся загрузить сохраненные данные
        loadChatDataFromMemory()
    }

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

                if (_compactionConfig.value.enabled) {
                    checkAndCompressHistory()
                }

                // Автоматическое сохранение после каждого изменения
                saveChatDataToMemory()

            } catch (e: Exception) {
                val errorMessage = UserMessage(
                    id = _messages.value.count(),
                    text = "Ошибка при анализе: ${e.message}",
                    isUser = false,
                    tokens = null
                )
                _messages.value += errorMessage

                // Сохраняем даже при ошибке
                saveChatDataToMemory()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildConversationHistory(): List<MessageRequest.Message> {
        val history = mutableListOf<MessageRequest.Message>()

        _messages.value.forEach { message ->
            when {
                message.isSummary && _compactionConfig.value.enabled -> {
                    history.add(
                        MessageRequest.Message(
                            role = "system",
                            text = "Контекст предыдущего диалога (резюме ${message.originalMessagesCount} сообщений):\n${message.text}"
                        )
                    )
                }
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

        // Сохраняем после компрессии
        saveChatDataToMemory()
    }

    fun toggleCompaction() {
        val newState = !_compactionConfig.value.enabled
        _compactionConfig.value = _compactionConfig.value.copy(
            enabled = newState
        )

        if (newState) {
            viewModelScope.launch {
                checkAndCompressHistory()
            }
        }

        // Сохраняем настройки
        saveChatDataToMemory()
    }

    // ← МЕТОДЫ для работы с памятью

    /**
     * Загружает данные из JSON файла
     */
    private fun loadChatDataFromMemory() {
        viewModelScope.launch {
            _isLoadingFromMemory.value = true

            try {
                val savedData = DataManager.loadChatData(applicationContext)

                if (savedData != null) {
                    _messages.value = savedData.messages
                    _totalTokenStats.value = savedData.totalTokenStats
                    _compactionConfig.value = savedData.compactionConfig
                    _compactionStats.value = savedData.compactionStats

                    _hasSavedData.value = true
                } else {
                    _hasSavedData.value = false
                }

                // Обновляем информацию о хранилище
                updateStorageInfo()

            } catch (e: Exception) {
                e.printStackTrace()
                _hasSavedData.value = false
            } finally {
                _isLoadingFromMemory.value = false
            }
        }
    }

    /**
     * Сохраняет текущие данные в JSON файл (автоматически)
     */
    private fun saveChatDataToMemory() {
        try {
            DataManager.saveChatData(
                context = applicationContext,
                messages = _messages.value,
                totalTokenStats = _totalTokenStats.value,
                compactionConfig = _compactionConfig.value,
                compactionStats = _compactionStats.value
            )

            updateStorageInfo()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Показывает диалог подтверждения очистки памяти
     */
    fun showClearMemoryDialog() {
        _showClearMemoryDialog.value = true
    }

    /**
     * Подтверждает очистку памяти
     */
    fun confirmClearMemory() {
        DataManager.clearChatData(applicationContext)
        _hasSavedData.value = false
        updateStorageInfo()
        _showClearMemoryDialog.value = false
    }

    /**
     * Отменяет очистку памяти
     */
    fun cancelClearMemory() {
        _showClearMemoryDialog.value = false
    }

    /**
     * Обновляет информацию о хранилище
     */
    private fun updateStorageInfo() {
        _storageInfo.value = DataManager.getStorageInfo(applicationContext)
        _hasSavedData.value = DataManager.hasSavedData(applicationContext)
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

        // Очищаем память при очистке чата
        clearSavedMemory()
    }

    /**
     * Очищает сохраненные данные
     */
    private fun clearSavedMemory() {
        DataManager.clearChatData(applicationContext)
        _hasSavedData.value = false
        updateStorageInfo()
    }
}