package dev.kamikaze.yandexgpttest.data

import dev.kamikaze.yandexgpttest.data.MessageRequest.Message

interface ApiClient {
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message> = emptyList(),
        userProfile: UserProfile? = null
    ): ApiResponse

    suspend fun createSummary(
        messages: List<Message>
    ): ApiResponse
}

class ApiClientFactory {
    companion object {
        fun create(settings: ApiSettings): ApiClient {
            return when (settings.environment) {
                ApiEnvironment.YANDEX_GPT -> YandexApiClient()
                ApiEnvironment.LOCAL_LLM -> LocalLlmApiClient(
                    baseUrl = settings.localLlmUrl,
                    model = settings.localLlmModel
                )
            }
        }
    }
}

class YandexApiClient : ApiClient {
    override suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message>,
        userProfile: UserProfile?
    ): ApiResponse {
        return YandexApi.sendMessage(userMessage, conversationHistory, userProfile)
    }

    override suspend fun createSummary(messages: List<Message>): ApiResponse {
        return YandexApi.createSummary(messages)
    }
}

class LocalLlmApiClient(
    baseUrl: String,
    model: String
) : ApiClient {
    private val localLlmApi = LocalLlmApi(baseUrl, model)

    override suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Message>,
        userProfile: UserProfile?
    ): ApiResponse {
        return localLlmApi.sendMessage(userMessage, conversationHistory, userProfile)
    }

    override suspend fun createSummary(messages: List<Message>): ApiResponse {
        return localLlmApi.createSummary(messages)
    }
}
