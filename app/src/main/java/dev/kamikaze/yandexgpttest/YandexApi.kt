package dev.kamikaze.yandexgpttest

import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.MessageResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object YandexApi {

    private const val FOLDER_ID = "b1g2tlrstcpe0emue6gs"
    private const val API_KEY = "AQVNxrs2XGfN19ibnrn8Ned2GRggIklo0Gw43ZpQ"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
        }
    }

    suspend fun sendMessage(message: String, settings: AISettings): String {
        val systemPrompt = buildSystemPrompt(settings)

        return try {
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        modelUri = "gpt://$FOLDER_ID/yandexgpt/latest",
                        completionOptions = MessageRequest.CompletionOptions(
                            maxTokens = settings.maxLength.toInt()
                        ),
                        messages = listOf(
                            MessageRequest.Message(role = "system", text = systemPrompt),
                            MessageRequest.Message(role = "user", text = message)
                        ),
                        json_object = settings.responseFormat == ResponseFormat.JSON
                    )
                )
            }

            val responseText = response.body<MessageResponse>()
                .result
                ?.alternatives
                ?.firstOrNull()
                ?.message?.text
                ?: "Нет ответа"

            // Парсим ответ согласно настройкам
            settings.responseFormat.parse(responseText)?.let { parsed ->
                formatParsedResponse(parsed, settings.responseFormat)
            } ?: responseText

        } catch (e: Exception) {
            "Ошибка: ${e.message}"
        }
    }

    private fun buildSystemPrompt(settings: AISettings): String {
        return when (settings.responseFormat) {
            ResponseFormat.JSON -> buildJsonPrompt(settings)
            ResponseFormat.MARKDOWN -> buildMarkdownPrompt(settings)
            ResponseFormat.CSV -> buildCsvPrompt(settings)
            ResponseFormat.XML -> buildXmlPrompt(settings)
        }
    }

    private fun buildJsonPrompt(settings: AISettings): String {
        val styleInstruction = getStyleInstruction(settings.responseStyle)

        return """
        Ты - AI помощник. ОТВЕЧАЙ ТОЛЬКО ЧИСТЫМ JSON БЕЗ ДОПОЛНИТЕЛЬНОГО ФОРМАТИРОВАНИЯ!
        
        ФОРМАТ ОТВЕТА:
        {
            "summary": "Краткое резюме ответа в 1-2 предложения",
            "explanation": "Подробное объяснение с техническими деталями",
            "code": "Примеры кода, если применимо",
            "references": ["Ссылки на документацию или источники, если применимо"]
        }
        
        ВАЖНЫЕ ПРАВИЛА:
        - ВОЗВРАЩАЙ ТОЛЬКО JSON НАЧИНАЮЩИЙСЯ С {
        - БЕЗ эмодзи, БЕЗ форматирования, БЕЗ дополнительного текста
        - БЕЗ markdown, БЕЗ ``` блоков
        - Только чистый валидный JSON!
        - Общий объем: не более ${settings.maxLength} символов
        
        СТИЛЬ ОБЩЕНИЯ:
        $styleInstruction
    """.trimIndent()
    }

    private fun buildMarkdownPrompt(settings: AISettings): String {
        val styleInstruction = getStyleInstruction(settings.responseStyle)

        return """
            Ты - AI помощник. Отвечай ТОЛЬКО в формате Markdown:
            
            # [Заголовок ответа]
            
            [Основной контент]
            
            **Краткое описание:** [краткое резюме в 1-2 предложения]
            **Категория:** [основная категория темы]
            
            Требования:
            - Общий объем текста не более ${settings.maxLength} символов
            - Структурируй информацию логично с заголовками
            $styleInstruction
            - Отвечай ТОЛЬКО валидным Markdown без дополнительного текста
            - Используй заголовки (# ## ###) для структуры
            - Используй списки и жирный текст для акцентов
        """.trimIndent()
    }

    private fun buildCsvPrompt(settings: AISettings): String {
        val styleInstruction = getStyleInstruction(settings.responseStyle)

        return """
            Ты - AI помощник. Отвечай СТРОГО в формате CSV:
            
            ФОРМАТ: заголовок,краткое_описание,детальное_объяснение,категория,ключевые_слова
            
            Требования:
            - Общий объем текста не более ${settings.maxLength} символов
            $styleInstruction
            - БЕЗ кавычек, БЕЗ заголовков таблицы, БЕЗ дополнительного текста
            - Только одна строка с данными через запятую
            - Поля должны содержать сжатую но информативную версию контента
        """.trimIndent()
    }

    private fun buildXmlPrompt(settings: AISettings): String {
        val styleInstruction = getStyleInstruction(settings.responseStyle)

        return """
            Ты - AI помощник. Отвечай ТОЛЬКО в формате XML:
            
            <response>
                <title>заголовок ответа</title>
                <content>основной контент</content>
                <summary>краткое резюме в 1-2 предложения</summary>
                <category>категория темы</category>
                <keywords>ключевое слово1, ключевое слово2</keywords>
            </response>
            
            Требования:
            - Общий объем текста не более ${settings.maxLength} символов
            $styleInstruction
            - БЕЗ дополнительного текста, только валидный XML
            - Структурируй информацию логично между тегами
            - Используй самозакрывающиеся теги где возможно
        """.trimIndent()
    }

    private fun formatParsedResponse(parsed: ParsedResponse, format: ResponseFormat): String {
        return when (format) {
            ResponseFormat.JSON -> {
                if (parsed.summary.isNotEmpty() || parsed.explanation.isNotEmpty()) {
                    Json.encodeToString(parsed)
                } else {
                    parsed.explanation.ifEmpty { "Контент в формате ${format.displayName}" }
                }
            }
            ResponseFormat.MARKDOWN -> {
                // Если есть парсинг Markdown, используем его, иначе возвращаем как есть
                parsed.explanation.ifEmpty { "Контент в формате ${format.displayName}" }
            }
            ResponseFormat.CSV -> {
                // Показываем CSV в читаемом виде
                if (parsed.summary.isNotEmpty() || parsed.explanation.isNotEmpty()) {
                    """
                    📊 **Структурированные данные (${format.displayName}):**
                    
                    **📋 Заголовок:** ${parsed.summary}
                    **📝 Контент:** ${parsed.explanation}
                    **🏷️ Категория:** ${parsed.metadata["category"] ?: "не указана"}
                    """.trimIndent()
                } else {
                    parsed.explanation.ifEmpty { "Данные в формате ${format.displayName}" }
                }
            }
            ResponseFormat.XML -> {
                if (parsed.summary.isNotEmpty() || parsed.explanation.isNotEmpty()) {
                    """
                    🏗️ **XML структура (${format.displayName}):**
                    
                    **📋 Заголовок:** ${parsed.metadata["title"] ?: parsed.summary}
                    **📝 Контент:** ${parsed.explanation}
                    **📄 Кратко:** ${parsed.summary}
                    """.trimIndent()
                } else {
                    parsed.explanation.ifEmpty { "Структура в формате ${format.displayName}" }
                }
            }
        }
    }

    private fun getStyleInstruction(style: ResponseStyle): String {
        return when (style) {
            ResponseStyle.FORMAL -> """
            Тон: официальный деловой
            Лексика: точная, корректная, профессиональная
            Структура: логичная, систематизированная
            Избегай: сокращений, жаргона, эмоциональных оценок
        """.trimIndent()

            ResponseStyle.INFORMAL -> """
            Тон: дружеский, разговорный
            Лексика: простые понятные слова, допустимы сокращения
            Структура: свободная, живая
            Можно: легкий юмор, ирония, метафоры
        """.trimIndent()

            ResponseStyle.NEUTRAL -> """
            Тон: объективный, сбалансированный
            Лексика: нейтральная, фактическая
            Структура: стандартная деловая
            Избегай: эмоциональных оценок, субъективности
        """.trimIndent()

            ResponseStyle.CREATIVE -> """
            Тон: креативный, оригинальный
            Лексика: яркая, образная, выразительная
            Структура: нестандартная, игривая
            Можно: метафоры, сравнения, неожиданные подходы
        """.trimIndent()

            ResponseStyle.TECHNICAL -> """
            Тон: профессиональный технический
            Лексика: точная терминология области
            Структура: четкая, детализированная
            Обязательно: конкретные примеры, ссылки на стандарты
        """.trimIndent()
        }
    }
}
