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

    private val temperatures = listOf(0f, 0.7f, 1.2f)

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

    // МЕТОД для отправки с разными температурами
    suspend fun sendMessageWithTemperature(
        userMessage: String,
        temperature: Float,
        conversationHistory: List<Message> = emptyList(),
    ): String {
        // Просто отправляем с темперэкспертатурой, никаких подсказок!
        val systemPrompt = "Ты -  по искусственному интеллекту и анализу, точная аналитическая модель. Дай конкретный, детерминированный ответ"
        return try {
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        modelUri = "gpt://$FOLDER_ID/yandexgpt/latest",
                        completionOptions = CompletionOptions(
                            temperature = temperature
                        ),
                        messages = listOf(
                            Message(role = "system", text = systemPrompt),
                            Message(role = "user", text = userMessage)
                        ) + conversationHistory
                    )
                )
            }

            val messageResponse = response.body<MessageResponse>()
            messageResponse.result?.alternatives?.firstOrNull()?.message?.text
                ?: "Нет ответа"

        } catch (e: Exception) {
            "Ошибка: ${e.message}"
        }
    }

    // РАСШИРЕННЫЙ МЕТОД - показываем ответы + анализ
    suspend fun analyzeTemperatures(
        userMessage: String,
        responses: Map<Float, String>,
    ): String {
        val analysisPrompt = """
            Твоя задача: проанализировать три ответа на один запрос с разными настройками температуры.

            ИСХОДНЫЙ ВОПРОС: "$userMessage"

            ОТВЕТЫ С РАЗНЫМИ ТЕМПЕРАТУРАМИ:
            
            1. Температура 0:
            ${responses[0f]}
            
            2. Температура 0.7:
            ${responses[0.7f]}
            
            3. Температура 1.2:
            ${responses[1.2f]}

            ДЕЙСТВУЙ ПО ПЛАНУ:
            1. Сначала перечисли все три ответа (каждый под номером)
            2. Затем проанализируй различия в стиле, содержании и подходе
            3. Выбери ЛУЧШИЙ ответ и обоснуй почему
            4. Укажи плюсы и минусы каждой температуры

            Структура ответа:
            Анализ по температуре:
            Температура 0: 
            [ответ]
            
            Температура 0.7: 
            [ответ] 
            
            Температура 1.2: 
            [ответ]

            === АНАЛИЗ ПО ПРЕДЫДУЩИМ ОТВЕТАМ ===
            [детальное сравнение различий]

            === ЛУЧШИЙ ОТВЕТ ===
            [номер + почему именно он лучший]

            === ПЛЮСЫ И МИНУСЫ ===
            Температура 0:
            Плюсы: [...]
            Минусы: [...]

            Температура 0.7:
            Плюсы: [...]
            Минусы: [...]

            Температура 1.2:
            Плюсы: [...]
            Минусы: [...]
            
            ВАЖНО: НЕ ИСПОЛЬЗУЙ ЗВЕЗДОЧКИ * И ДРУГИЕ СИМВОЛЫ ПРИ ИТОГОВОМ ВЫВОДЕ
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
                            temperature = 0.3f  // Средняя температура для анализа
                        ),
                        messages = listOf(
                            Message(role = "system", text = "Ты - эксперт по сравнению текстов и выбору лучших вариантов. Дай четкий, структурированный анализ."),
                            Message(role = "user", text = analysisPrompt)
                        )
                    )
                )
            }

            val messageResponse = response.body<MessageResponse>()
            messageResponse.result?.alternatives?.firstOrNull()?.message?.text
                ?: "Анализ недоступен"

        } catch (e: Exception) {
            "Ошибка анализа: ${e.message}"
        }
    }

    // ГЛАВНЫЙ МЕТОД - собираем всё вместе
    suspend fun compareTemperatures(
        userMessage: String,
        conversationHistory: List<Message>,
    ): String {
        val responses = mutableMapOf<Float, String>()

        // 1. Получаем ответы с разными температурами
        temperatures.forEach { temp ->
            val response = sendMessageWithTemperature(userMessage, temp, conversationHistory)
            responses[temp] = response
        }

        // 2. Даем нейросети проанализировать и показать всё
        return analyzeTemperatures(userMessage, responses)
    }
}