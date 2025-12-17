package dev.kamikaze.yandexgpttest.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiSettings(
    val environment: ApiEnvironment = ApiEnvironment.YANDEX_GPT,
    val localLlmUrl: String = "http://10.0.2.2:11434",
    val localLlmModel: String = "qwen3:14b",
    val taskAssistantEnabled: Boolean = false
)
