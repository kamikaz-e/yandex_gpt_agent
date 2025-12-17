package dev.kamikaze.yandexgpttest.data

import android.util.Log
import dev.kamikaze.yandexgpttest.data.MessageRequest.Message
import dev.kamikaze.yandexgpttest.data.prompt.buildPersonalizedSystemPrompt
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class LocalLlmApi(private val baseUrl: String, private val model: String) {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
                encodeDefaults = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("LocalLlmApi", message)
                }
            }
            level = LogLevel.ALL
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000 // 2 минуты для локальных моделей
            connectTimeoutMillis = 30000
        }

        // Важно: отключаем ожидание 100-Continue для Ollama
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    @Serializable
    data class OllamaChatRequest(
        val model: String,
        val messages: List<OllamaMessage>,
        val stream: Boolean = false,
        val options: OllamaOptions? = null
    )

    @Serializable
    data class OllamaMessage(
        val role: String,
        val content: String
    )

    @Serializable
    data class OllamaOptions(
        val temperature: Float = 0.7f,
        val num_ctx: Int = 8192
    )

    @Serializable
    data class OllamaChatResponse(
        val model: String? = null,
        val created_at: String? = null,
        val message: OllamaMessage? = null,
        val done: Boolean? = null,
        val total_duration: Long? = null,
        val prompt_eval_count: Int? = null,
        val eval_count: Int? = null,
        val error: String? = null
    )

    suspend fun createSummary(
        messages: List<Message>
    ): ApiResponse {
        val summaryPrompt = """
        Создай краткое резюме следующего диалога, сохраняя ключевые факты, вопросы и ответы.
        Структура резюме:
        - Основные темы обсуждения
        - Ключевые вопросы пользователя
        - Важные факты и выводы
        - Контекст для продолжения разговора

        ВАЖНО: Резюме должно быть максимально компактным, но содержать всю критическую информацию для продолжения диалога.

        Диалог для анализа:
        ${messages.joinToString("\n") { "${it.role}: ${it.text}" }}
    """.trimIndent()

        return try {
            Log.d("LocalLlmApi", "Создание резюме. URL: $baseUrl, Модель: $model")

            val response = client.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaChatRequest(
                        model = model,
                        messages = listOf(
                            OllamaMessage(role = "system", content = "Ты - эксперт по созданию кратких, но информативных резюме диалогов."),
                            OllamaMessage(role = "user", content = summaryPrompt)
                        ),
                        options = OllamaOptions(temperature = 0.3f)
                    )
                )
            }

            Log.d("LocalLlmApi", "Получен ответ от Ollama, статус: ${response.status}")

            val ollamaResponse = response.body<OllamaChatResponse>()

            // Проверяем наличие ошибки
            if (ollamaResponse.error != null) {
                return ApiResponse(
                    text = "Ошибка Ollama: ${ollamaResponse.error}",
                    tokens = TokenStats()
                )
            }

            val text = ollamaResponse.message?.content ?: "Нет ответа от модели"

            val tokens = TokenStats(
                inputTokens = ollamaResponse.prompt_eval_count ?: 0,
                outputTokens = ollamaResponse.eval_count ?: 0,
                totalTokens = (ollamaResponse.prompt_eval_count ?: 0) + (ollamaResponse.eval_count ?: 0)
            )

            ApiResponse(text = text, tokens = tokens)

        } catch (e: Exception) {
            e.printStackTrace()
            ApiResponse(
                text = "Ошибка создания резюме: ${e.message}\n\nПроверьте:\n1. Запущен ли Ollama сервер\n2. Доступен ли по адресу $baseUrl\n3. Загружена ли модель $model",
                tokens = TokenStats()
            )
        }
    }

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message> = emptyList(),
        userProfile: UserProfile? = null
    ): ApiResponse {
        val systemPrompt = buildPersonalizedSystemPrompt(userProfile)

        return try {
            Log.d("LocalLlmApi", "Отправка сообщения. URL: $baseUrl, Модель: $model")
            Log.d("LocalLlmApi", "Сообщение пользователя: ${userMessage.take(100)}...")

            val ollamaMessages = mutableListOf<OllamaMessage>()

            // Добавляем системный промпт
            ollamaMessages.add(OllamaMessage(role = "system", content = systemPrompt))

            // Добавляем историю диалога
            conversationHistory.forEach { message ->
                ollamaMessages.add(
                    OllamaMessage(
                        role = message.role,
                        content = message.text
                    )
                )
            }

            // Добавляем текущее сообщение пользователя
            ollamaMessages.add(OllamaMessage(role = "user", content = userMessage))

            Log.d("LocalLlmApi", "Отправка ${ollamaMessages.size} сообщений к Ollama")

            val response = client.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaChatRequest(
                        model = model,
                        messages = ollamaMessages,
                        options = OllamaOptions(temperature = 0.7f, num_ctx = 8192)
                    )
                )
            }

            Log.d("LocalLlmApi", "Получен ответ от Ollama, статус: ${response.status}")

            val ollamaResponse = response.body<OllamaChatResponse>()

            // Проверяем наличие ошибки
            if (ollamaResponse.error != null) {
                return ApiResponse(
                    text = "Ошибка Ollama: ${ollamaResponse.error}",
                    tokens = TokenStats()
                )
            }

            val text = ollamaResponse.message?.content ?: "Нет ответа от модели"

            val tokens = TokenStats(
                inputTokens = ollamaResponse.prompt_eval_count ?: 0,
                outputTokens = ollamaResponse.eval_count ?: 0,
                totalTokens = (ollamaResponse.prompt_eval_count ?: 0) + (ollamaResponse.eval_count ?: 0)
            )

            ApiResponse(text = text, tokens = tokens)

        } catch (e: Exception) {
            e.printStackTrace()
            ApiResponse(
                text = "Ошибка подключения к локальной LLM: ${e.message}\n\nПроверьте:\n1. Запущен ли Ollama сервер по адресу: $baseUrl\n2. Загружена ли модель: $model\n3. Доступен ли сервер из Android эмулятора\n\nДля проверки выполните:\ncurl $baseUrl/api/version",
                tokens = TokenStats()
            )
        }
    }
}
