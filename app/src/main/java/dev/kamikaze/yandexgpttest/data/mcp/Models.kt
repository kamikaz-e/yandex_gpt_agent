package dev.kamikaze.yandexgpttest.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class CallToolParams(
    val name: String,
    val arguments: JsonObject
)

@Serializable
data class CallToolResult(
    val result: JsonElement
)