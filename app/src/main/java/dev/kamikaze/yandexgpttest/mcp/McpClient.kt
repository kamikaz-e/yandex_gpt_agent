package dev.kamikaze.yandexgpttest.mcp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.UUID

class McpClient {
    
    companion object {
        // Замените на адрес вашего MCP-сервера
        private const val MCP_SERVER_URL = "http://10.0.2.2:8080"
    }

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

    private var isInitialized = false

    /**
     * Инициализация соединения с MCP-сервером
     */
    suspend fun initialize(): McpResult<InitializeResult> {
        return try {
            val initializeParams = InitializeParams(
                protocolVersion = "2024-11-05",
                capabilities = ClientCapabilities(
                    roots = RootsCapability(listChanged = true),
                    sampling = SamplingCapability(enabled = true)
                ),
                clientInfo = ClientInfo(
                    name = "YandexGPT Android Client",
                    version = "1.0.0"
                )
            )

            val request = McpRequest(
                id = generateId(),
                method = "initialize",
                params = initializeParams
            )

            val response = client.post("$MCP_SERVER_URL/rpc") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val mcpResponse = response.body<McpResponse<InitializeResult>>()
            
            if (mcpResponse.error != null) {
                McpResult.Error("Ошибка инициализации: ${mcpResponse.error.message}")
            } else if (mcpResponse.result != null) {
                isInitialized = true
                McpResult.Success(mcpResponse.result)
            } else {
                McpResult.Error("Пустой ответ от сервера")
            }

        } catch (e: Exception) {
            McpResult.Error("Ошибка подключения: ${e.message}")
        }
    }

    /**
     * Получение списка доступных инструментов
     */
    suspend fun listTools(): McpResult<List<Tool>> {
        if (!isInitialized) {
            val initResult = initialize()
            if (initResult is McpResult.Error) {
                return McpResult.Error("Не удалось инициализировать соединение: ${initResult.message}")
            }
        }

        return try {
            val request = McpRequest<Unit>(
                id = generateId(),
                method = "tools/list",
                params = null
            )

            val response = client.post("$MCP_SERVER_URL/rpc") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val mcpResponse = response.body<McpResponse<ListToolsResult>>()

            if (mcpResponse.error != null) {
                McpResult.Error("Ошибка получения инструментов: ${mcpResponse.error.message}")
            } else if (mcpResponse.result != null) {
                McpResult.Success(mcpResponse.result.tools)
            } else {
                McpResult.Error("Пустой список инструментов")
            }

        } catch (e: Exception) {
            McpResult.Error("Ошибка запроса: ${e.message}")
        }
    }

    /**
     * Проверка доступности MCP-сервера
     */
    suspend fun ping(): McpResult<String> {
        return try {
            val response = client.get("$MCP_SERVER_URL/health") {
                timeout {
                    requestTimeoutMillis = 5000
                }
            }

            if (response.status == HttpStatusCode.OK) {
                McpResult.Success("MCP сервер доступен")
            } else {
                McpResult.Error("Сервер недоступен: ${response.status}")
            }
        } catch (e: Exception) {
            McpResult.Error("Сервер недоступен: ${e.message}")
        }
    }

    private fun generateId(): String = UUID.randomUUID().toString()

    fun close() {
        client.close()
    }
}

sealed class McpResult<out T> {
    data class Success<T>(val data: T) : McpResult<T>()
    data class Error(val message: String) : McpResult<Nothing>()
}