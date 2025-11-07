package dev.kamikaze.yandexgpttest.data

import dev.kamikaze.yandexgpttest.data.MessageRequest.CompletionOptions
import dev.kamikaze.yandexgpttest.data.MessageRequest.Message
import dev.kamikaze.yandexgpttest.data.prompt.ResponseFormat
import dev.kamikaze.yandexgpttest.data.prompt.buildSystemPrompt
import dev.kamikaze.yandexgpttest.ui.AISettings
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
            requestTimeoutMillis = 12000
        }
    }

    suspend fun sendMessage(
        conversationHistory: List<Message>,
        settings: AISettings,
        needTotalResult: Boolean
    ): String {
        val systemPrompt = buildSystemPrompt(settings, needTotalResult)

        return try {
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        modelUri = "gpt://$FOLDER_ID/yandexgpt/latest",
                        completionOptions = CompletionOptions(
                            maxTokens = settings.maxLength
                        ),
                        messages = listOf(
                            Message(
                                role = "system",
                                text = systemPrompt
                            )
                        ) + conversationHistory,
                        json_object = settings.responseFormat == ResponseFormat.JSON
                    )
                )
            }

            response.body<MessageResponse>()
                .result
                ?.alternatives
                ?.firstOrNull()
                ?.message?.text
                ?: "Нет ответа"

        } catch (e: Exception) {
            "Ошибка: ${e.message}"
        }
    }
}