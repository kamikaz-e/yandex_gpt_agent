package dev.kamikaze.yandexgpttest.domain

import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.ParsedResponse
import dev.kamikaze.yandexgpttest.data.YandexApi
import dev.kamikaze.yandexgpttest.data.prompt.ResponseFormat
import dev.kamikaze.yandexgpttest.ui.AISettings
import kotlinx.serialization.json.Json

class ChatInteractor(private val api: YandexApi) {

    suspend fun sendMessage(
        conversationHistory: List<MessageRequest.Message>,
        settings: AISettings,
        needTotalResult: Boolean,
    ): String {
        val responseText = api.sendMessage(conversationHistory, settings, needTotalResult)
        return settings.responseFormat.parse(responseText)?.let { parsed ->
            formatParsedResponse(parsed, settings.responseFormat)
        } ?: responseText
    }

    private fun formatParsedResponse(parsed: ParsedResponse, format: ResponseFormat): String {
        return when (format) {
            ResponseFormat.JSON -> {
                if (parsed.summary.isNotEmpty() || parsed.description.isNotEmpty()) {
                    Json.encodeToString(parsed)
                } else {
                    parsed.description.ifEmpty { "Контент в формате ${format.displayName}" }
                }
            }

            ResponseFormat.MARKDOWN -> {
                // Если есть парсинг Markdown, используем его, иначе возвращаем как есть
                parsed.description.ifEmpty { "Контент в формате ${format.displayName}" }
            }

            ResponseFormat.CSV -> {
                // Показываем CSV в читаемом виде
                if (parsed.summary.isNotEmpty() || parsed.description.isNotEmpty()) {
                    """
                    📊 **(${format.displayName}):**
                    
                    **📋 Заголовок:** ${parsed.summary}
                    **📝 Контент:** ${parsed.description}
                    **🏷️ Категория:** ${parsed.metadata["category"] ?: "не указана"}
                    """.trimIndent()
                } else {
                    parsed.description.ifEmpty { "Данные в формате ${format.displayName}" }
                }
            }

            ResponseFormat.XML -> {
                if (parsed.summary.isNotEmpty() || parsed.description.isNotEmpty()) {
                    """
                    🏗️ **XML структура (${format.displayName}):**
                    
                    **📋 Заголовок:** ${parsed.metadata["title"] ?: parsed.summary}
                    **📝 Контент:** ${parsed.description}
                    **📄 Кратко:** ${parsed.summary}
                    """.trimIndent()
                } else {
                    parsed.description.ifEmpty { "Структура в формате ${format.displayName}" }
                }
            }
        }
    }
}