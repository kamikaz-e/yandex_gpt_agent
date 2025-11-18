package dev.kamikaze.yandexgpttest.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class McpRequest<T>(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: T? = null
)

@Serializable
data class McpResponse<T>(
    val jsonrpc: String,
    val id: String? = null,
    val result: T? = null,
    val error: McpError? = null
)

@Serializable
data class McpError(
    val code: Int,
    val message: String,
    val data: String? = null
)

// Инициализация
@Serializable
data class InitializeParams(
    val protocolVersion: String,
    val capabilities: ClientCapabilities,
    val clientInfo: ClientInfo
)

@Serializable
data class ClientCapabilities(
    val roots: RootsCapability? = null,
    val sampling: SamplingCapability? = null
)

@Serializable
data class RootsCapability(
    val listChanged: Boolean = true
)

@Serializable
data class SamplingCapability(
    val enabled: Boolean = true
)

@Serializable
data class ClientInfo(
    val name: String,
    val version: String
)

@Serializable
data class InitializeResult(
    val protocolVersion: String,
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo
)

@Serializable
data class ServerCapabilities(
    val logging: LoggingCapability? = null,
    val prompts: PromptsCapability? = null,
    val resources: ResourcesCapability? = null,
    val tools: ToolsCapability? = null
)

@Serializable
data class LoggingCapability(
    val enabled: Boolean = true
)

@Serializable
data class PromptsCapability(
    val listChanged: Boolean = true
)

@Serializable
data class ResourcesCapability(
    val subscribe: Boolean = true,
    val listChanged: Boolean = true
)

@Serializable
data class ToolsCapability(
    val listChanged: Boolean = true
)

@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)

// Инструменты
@Serializable
data class Tool(
    val name: String,
    val description: String? = null,
    val inputSchema: ToolInputSchema
)

@Serializable
data class ToolInputSchema(
    val type: String,
    val properties: Map<String, JsonElement>? = null,
    val required: List<String>? = null
)

// Для списка инструментов
@Serializable
data class ListToolsResult(
    val tools: List<Tool>
)