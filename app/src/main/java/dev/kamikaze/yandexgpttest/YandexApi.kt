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
    private const val API_KEY = "AQVN3Q6zjle3-gU2mklR5vtDsU30RQLLx9SWidwm"

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
            requestTimeoutMillis = 10000
        }
    }

    suspend fun sendMessage(message: String): String {
        return try {
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        modelUri = "gpt://$FOLDER_ID/yandexgpt/latest",
                        completionOptions = MessageRequest.CompletionOptions(
                            temperature = 0.33f,
                            maxTokens = 512
                        ),
                        messages = listOf(
                            MessageRequest.Message(role = "user", text = message)
                        )
                    )
                )
            }

            response.body<MessageResponse>().result?.alternatives?.firstOrNull()?.message?.text ?: "Нет ответа"
        } catch (e: Exception) {
            "Ошибка: ${e.message}"
        }
    }
}