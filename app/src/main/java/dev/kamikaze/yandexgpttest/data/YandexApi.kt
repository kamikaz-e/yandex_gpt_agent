package dev.kamikaze.yandexgpttest.data

import dev.kamikaze.yandexgpttest.data.MessageRequest.CompletionOptions
import dev.kamikaze.yandexgpttest.data.MessageRequest.Message
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
            requestTimeoutMillis = 20000
        }
    }

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
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        modelUri = "gpt://$FOLDER_ID/yandexgpt/latest",
                        completionOptions = CompletionOptions(
                            temperature = 0.3f
                        ),
                        messages = listOf(
                            Message(role = "system", text = "Ты - эксперт по созданию кратких, но информативных резюме диалогов."),
                            Message(role = "user", text = summaryPrompt)
                        )
                    )
                )
            }

            val messageResponse = response.body<MessageResponse>()
            val text = messageResponse.result?.alternatives?.firstOrNull()?.message?.text
                ?: "Не удалось создать резюме"
            val tokens = TokenStats.fromUsage(messageResponse.result?.usage)

            ApiResponse(text = text, tokens = tokens)

        } catch (e: Exception) {
            ApiResponse(
                text = "Ошибка создания резюме: ${e.message}",
                tokens = TokenStats()
            )
        }
    }

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message> = emptyList(),
    ): ApiResponse {
        val systemPrompt = """
    Ты - эксперт-ассистент с доступом к инструментам GitHub API через Model Context Protocol (MCP).

    📋 ДОСТУПНЫЕ ИНСТРУМЕНТЫ:

    1) github_issue_count
       Получить количество открытых issues в репозитории
       Параметры: owner (владелец), repo (название)
       Пример: owner="facebook", repo="react"

    2) github_repo_info
       Получить полную информацию о репозитории (звезды, форки, язык, etc.)
       Параметры: owner, repo

    3) github_search_repos
       Поиск репозиториев по запросу
       Параметры: query (поисковый запрос)

    4) current_time
       Получить текущее время
       Параметры: нет

    🎯 ПРАВИЛА ИСПОЛЬЗОВАНИЯ:

    Если вопрос требует данных из GitHub (например: "сколько issues", "информация о репозитории", "найди репозиторий"):
    1. Определи нужный инструмент
    2. Извлеки параметры из вопроса
    3. Верни ТОЛЬКО JSON в формате:
    {
      "tool_call": {
        "name": "имя_инструмента",
        "arguments": { "param1": "value1", "param2": "value2" }
      }
    }

    ПРИМЕРЫ:

    Вопрос: "Сколько открытых issues в facebook/react?"
    Ответ:
    {
      "tool_call": {
        "name": "github_issue_count",
        "arguments": { "owner": "facebook", "repo": "react" }
      }
    }

    Вопрос: "Расскажи про репозиторий kotlin/kotlinx.coroutines"
    Ответ:
    {
      "tool_call": {
        "name": "github_repo_info",
        "arguments": { "owner": "kotlin", "repo": "kotlinx.coroutines" }
      }
    }

    Вопрос: "Найди популярные репозитории по kotlin"
    Ответ:
    {
      "tool_call": {
        "name": "github_search_repos",
        "arguments": { "query": "kotlin language:kotlin stars:>1000" }
      }
    }

    ⚠️ ВАЖНО:
    - Если инструмент НЕ нужен - отвечай обычным текстом
    - JSON должен быть валидным
    - Не добавляй markdown форматирование к JSON
    """.trimIndent()

        return try {
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        modelUri = "gpt://$FOLDER_ID/yandexgpt/latest",
                        completionOptions = CompletionOptions(
                            temperature = 0.3f
                        ),
                        messages = listOf(
                            Message(role = "system", text = systemPrompt)
                        ) + conversationHistory + listOf(
                            Message(role = "user", text = userMessage)
                        )
                    )
                )
            }

            val messageResponse = response.body<MessageResponse>()
            val text = messageResponse.result?.alternatives?.firstOrNull()?.message?.text
                ?: "Нет ответа"
            val tokens = TokenStats.fromUsage(messageResponse.result?.usage)

            ApiResponse(text = text, tokens = tokens)

        } catch (e: Exception) {
            ApiResponse(
                text = "Ошибка: ${e.message}",
                tokens = TokenStats()
            )
        }
    }
}