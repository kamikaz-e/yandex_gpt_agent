package dev.kamikaze.yandexgpttest.data.mcp

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.UUID

class McpClient {
    companion object {
        // Для эмулятора Android → локальный хост: 10.0.2.2
        private const val MCP_SERVER_URL = "http://10.0.2.2:8080"
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) { level = LogLevel.ALL }
        install(HttpTimeout) { requestTimeoutMillis = 15000 }
    }

    private var isInitialized = false

    suspend fun initialize(): McpResult<InitializeResult> {
        return try {
            val request = McpRequest(
                id = generateId(),
                method = "initialize",
                params = InitializeParams(
                    protocolVersion = "2024-11-05",
                    capabilities = ClientCapabilities(),
                    clientInfo = ClientInfo("YandexGPT Android Client", "1.0.0")
                )
            )
            val httpResp = client.post("$MCP_SERVER_URL/rpc") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val resp = httpResp.body<McpResponse<InitializeResult>>()
            if (resp.error != null) McpResult.Error(resp.error.message)
            else {
                isInitialized = true
                McpResult.Success(resp.result!!)
            }
        } catch (e: Exception) {
            McpResult.Error("init error: ${e.message}")
        }
    }

    suspend fun listTools(): McpResult<List<Tool>> {
        if (!isInitialized) {
            when (val init = initialize()) {
                is McpResult.Error -> return McpResult.Error("init failed: ${init.message}")
                is McpResult.Success -> {}
            }
        }
        return try {
            val request = McpRequest<Unit>(
                id = generateId(),
                method = "tools/list",
                params = null
            )
            val httpResp = client.post("$MCP_SERVER_URL/rpc") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val resp = httpResp.body<McpResponse<ListToolsResult>>()
            if (resp.error != null) McpResult.Error(resp.error.message)
            else McpResult.Success(resp.result!!.tools)
        } catch (e: Exception) {
            McpResult.Error("list error: ${e.message}")
        }
    }

    suspend fun callTool(name: String, arguments: JsonObject): McpResult<CallToolResult> {
        if (!isInitialized) {
            when (val init = initialize()) {
                is McpResult.Error -> return McpResult.Error("init failed: ${init.message}")
                is McpResult.Success -> {}
            }
        }
        return try {
            val request = McpRequest(
                id = generateId(),
                method = "tools/call",
                params = CallToolParams(name = name, arguments = arguments)
            )
            val httpResp = client.post("$MCP_SERVER_URL/rpc") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val resp = httpResp.body<McpResponse<CallToolResult>>()
            if (resp.error != null) McpResult.Error(resp.error.message)
            else McpResult.Success(resp.result!!)
        } catch (e: Exception) {
            McpResult.Error("call error: ${e.message}")
        }
    }

    suspend fun ping(): McpResult<String> {
        return try {
            val httpResp = client.get("$MCP_SERVER_URL/health")
            if (httpResp.status == HttpStatusCode.OK) McpResult.Success("ok")
            else McpResult.Error("status ${httpResp.status}")
        } catch (e: Exception) {
            McpResult.Error("ping error: ${e.message}")
        }
    }

    private fun generateId(): String = UUID.randomUUID().toString()
    fun close() = client.close()
}

sealed class McpResult<out T> {
    data class Success<T>(val data: T) : McpResult<T>()
    data class Error(val message: String) : McpResult<Nothing>()
}