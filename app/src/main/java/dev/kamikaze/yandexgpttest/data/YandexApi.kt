package dev.kamikaze.yandexgpttest.data

import dev.kamikaze.yandexgpttest.data.MessageRequest.CompletionOptions
import dev.kamikaze.yandexgpttest.data.MessageRequest.Message
import dev.kamikaze.yandexgpttest.data.prompt.AgentType
import dev.kamikaze.yandexgpttest.data.prompt.buildExpertSystemPrompt
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

    suspend fun sendMessages(
        userMessage: String,
        conversationHistory: List<Message>,
        agentType: AgentType,
    ): ExpertAgentResponse {
        return sendMessageToAgent(
            agentType = agentType,
            userMessage = userMessage,
            conversationHistory = conversationHistory
        )
    }

    private suspend fun sendMessageToAgent(
        agentType: AgentType,
        userMessage: String,
        conversationHistory: List<Message>,
    ): ExpertAgentResponse {
        val systemPrompt = buildExpertSystemPrompt(agentType)

        return try {
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        modelUri = when (agentType) {
                            AgentType.YANDEXGPT_RC -> "gpt://$FOLDER_ID/yandexgpt/rc"
                            AgentType.YANDEXGPT_LATEST -> "gpt://$FOLDER_ID/yandexgpt/latest"
                        },
                        completionOptions = CompletionOptions(),
                        messages = listOf(
                            Message(
                                role = "system",
                                text = systemPrompt
                            ),
                            Message(
                                role = "user",
                                text = userMessage
                            )
                        ) + conversationHistory
                    )
                )
            }

            val messageResponse = response.body<MessageResponse>()
            val result = messageResponse.result?.alternatives?.firstOrNull()?.message?.text
                ?: "Нет ответа от агента"

            ExpertAgentResponse(
                agentType = agentType,
                response = result
            )

        } catch (e: Exception) {
            ExpertAgentResponse(
                agentType = agentType,
                response = "Ошибка: ${e.message}"
            )
        }
    }
}

// Добавляем новые data classes
data class ExpertAgentResponse(
    val agentType: AgentType,
    val response: String,
)
