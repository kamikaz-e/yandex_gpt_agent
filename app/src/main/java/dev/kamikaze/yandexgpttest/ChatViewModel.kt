package dev.kamikaze.yandexgpttest

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kamikaze.yandexgpttest.data.*
import dev.kamikaze.yandexgpttest.data.mcp.McpClient
import dev.kamikaze.yandexgpttest.data.mcp.McpResult
import dev.kamikaze.yandexgpttest.data.mcp.Tool
import dev.kamikaze.yandexgpttest.domain.ChatInteractor
import dev.kamikaze.yandexgpttest.ui.UserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    private val _mcpTools = MutableStateFlow<List<Tool>>(emptyList())
    val mcpTools: StateFlow<List<Tool>> = _mcpTools.asStateFlow()

    private val _isLoadingMcpTools = MutableStateFlow(false)
    val isLoadingMcpTools: StateFlow<Boolean> = _isLoadingMcpTools.asStateFlow()

    private val _mcpStatus = MutableStateFlow("Не подключен")
    val mcpStatus: StateFlow<String> = _mcpStatus.asStateFlow()

    private val _showMcpToolsDialog = MutableStateFlow(false)
    val showMcpToolsDialog: StateFlow<Boolean> = _showMcpToolsDialog.asStateFlow()

    private val json by lazy { Json { ignoreUnknownKeys = true; isLenient = true } }
    private val mcpClient = McpClient()

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

                val isToolCall = tryHandleToolCall(apiResponse.text)
                if (!isToolCall) {
                    // Только если НЕ tool_call - добавляем как обычный ответ
                    val assistantMessage = UserMessage(
                        id = _messages.value.count(),
                        text = apiResponse.text,
                        isUser = false,
                        tokens = apiResponse.tokens
                    )
                    _messages.value += assistantMessage
                }

                updateTotalTokens(apiResponse.tokens)

                if (_compactionConfig.value.enabled) {
                    checkAndCompressHistory()
                }

                saveChatDataToMemory()

            } catch (e: Exception) {
                val errorMessage = UserMessage(
                    id = _messages.value.count(),
                    text = "Ошибка при анализе: ${e.message}",
                    isUser = false,
                    tokens = null
                )
                _messages.value += errorMessage
                saveChatDataToMemory()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMcpTools() {
        viewModelScope.launch {
            _isLoadingMcpTools.value = true
            _mcpStatus.value = "Подключение..."

            try {
                val pingResult = mcpClient.ping()
                if (pingResult is McpResult.Error) {
                    _mcpStatus.value = "Сервер недоступен"
                    _mcpTools.value = emptyList()
                    return@launch
                }

                when (val toolsResult = mcpClient.listTools()) {
                    is McpResult.Success<*> -> {
                        val tools = toolsResult.data as? List<Tool> ?: emptyList()
                        _mcpTools.value = tools
                        _mcpStatus.value = if (tools.isEmpty()) {
                            "Подключен (нет инструментов)"
                        } else {
                            "Подключен (${tools.size} инструментов)"
                        }
                        if (tools.isNotEmpty()) {
                            _showMcpToolsDialog.value = true
                        }
                    }

                    is McpResult.Error -> {
                        _mcpStatus.value = "Ошибка: ${toolsResult.message}"
                        _mcpTools.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                _mcpStatus.value = "Ошибка подключения: ${e.message}"
                _mcpTools.value = emptyList()
            } finally {
                _isLoadingMcpTools.value = false
            }
        }
    }


    fun hideMcpToolsDialog() {
        _showMcpToolsDialog.value = false
    }

    override fun onCleared() {
        super.onCleared()
        mcpClient.close()
    }

    private suspend fun tryHandleToolCall(assistantText: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val cleanText = assistantText.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val root = json.parseToJsonElement(cleanText)

                if (root is JsonObject && root.containsKey("tool_call")) {
                    val toolCall = root["tool_call"]!!.jsonObject
                    val name = toolCall["name"]?.jsonPrimitive?.contentOrNull
                        ?: return@withContext false
                    val args = toolCall["arguments"]?.jsonObject
                        ?: JsonObject(emptyMap())

                    // ← Показываем индикатор вызова
                    val loadingMsg = UserMessage(
                        id = _messages.value.count(),
                        text = "🔧 Вызываю инструмент $name...",
                        isUser = false
                    )
                    _messages.value += loadingMsg

                    when (val call = mcpClient.callTool(name, args)) {
                        is McpResult.Error -> {
                            _messages.value = _messages.value.dropLast(1)

                            val msg = UserMessage(
                                id = _messages.value.count(),
                                text = "❌ Ошибка: ${call.message}",
                                isUser = false
                            )
                            _messages.value += msg
                        }

                        is McpResult.Success -> {
                            _messages.value = _messages.value.dropLast(1)

                            // ← НОВОЕ: Парсим результат для красивого отображения
                            val mcpResultMessage = parseMcpResult(name, call.data.result)
                            _messages.value += mcpResultMessage
                        }
                    }

                    saveChatDataToMemory()
                    return@withContext true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            false
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

        val messagesForSummary =
            messagesToCompress.take(_compactionConfig.value.messagesThreshold)
        val remainingMessages =
            messagesToCompress.drop(_compactionConfig.value.messagesThreshold)

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

    // ← НОВЫЙ МЕТОД: Парсинг MCP результата
    private fun parseMcpResult(toolName: String, resultJson: JsonElement): UserMessage {
        return try {
            val resultObj = if (resultJson is JsonObject && resultJson.containsKey("result")) {
                resultJson["result"]!!.jsonObject
            } else {
                resultJson.jsonObject
            }

            // Извлекаем summary если есть
            val summaryText = resultObj["summary"]?.jsonPrimitive?.contentOrNull

            if (summaryText != null) {
                // Если есть summary - используем его
                UserMessage(
                    id = _messages.value.count(),
                    text = summaryText,
                    isUser = false,
                    isMcpResult = true,
                    mcpToolName = toolName
                )
            } else {
                // Форматируем JSON как текст
                val formattedText = formatMcpResult(toolName, resultObj)
                UserMessage(
                    id = _messages.value.count(),
                    text = formattedText,
                    isUser = false,
                    isMcpResult = true,
                    mcpToolName = toolName
                )
            }
        } catch (e: Exception) {
            // Fallback - показываем как есть
            UserMessage(
                id = _messages.value.count(),
                text = json.encodeToString(JsonElement.serializer(), resultJson),
                isUser = false
            )
        }
    }

    // ← НОВЫЙ МЕТОД: Форматирование результата
    private fun formatMcpResult(toolName: String, resultObj: JsonObject): String {
        return when (toolName) {
            "github_issue_count" -> {
                val owner = resultObj["owner"]?.jsonPrimitive?.contentOrNull ?: "?"
                val repo = resultObj["repo"]?.jsonPrimitive?.contentOrNull ?: "?"
                val issues = resultObj["open_issues"]?.jsonPrimitive?.contentOrNull ?: "0"
                """
            📊 Репозиторий: $owner/$repo
            🐛 Открытые issues: $issues
            """.trimIndent()
            }

            "github_repo_info" -> {
                val name = resultObj["name"]?.jsonPrimitive?.contentOrNull ?: "?"
                val desc = resultObj["description"]?.jsonPrimitive?.contentOrNull ?: "Нет описания"
                val stars = resultObj["stars"]?.jsonPrimitive?.contentOrNull ?: "0"
                val forks = resultObj["forks"]?.jsonPrimitive?.contentOrNull ?: "0"
                val language = resultObj["language"]?.jsonPrimitive?.contentOrNull ?: "Не указан"
                val issues = resultObj["open_issues"]?.jsonPrimitive?.contentOrNull ?: "0"

                """
            🏆 $name
            $desc
            
            ⭐ Звезды: $stars
            🍴 Форки: $forks
            🐛 Issues: $issues
            💻 Язык: $language
            """.trimIndent()
            }

            "github_search_repos" -> {
                val summary = resultObj["summary"]?.jsonPrimitive?.contentOrNull
                summary ?: "Результаты поиска получены"
            }

            "current_time" -> {
                val formatted = resultObj["formatted"]?.jsonPrimitive?.contentOrNull ?: "?"
                "🕐 Текущее время: $formatted"
            }

            else -> {
                // Для неизвестных инструментов - показываем все поля
                resultObj.entries.joinToString("\n") { (key, value) ->
                    "$key: ${value.jsonPrimitive.contentOrNull ?: value}"
                }
            }
        }
    }
}