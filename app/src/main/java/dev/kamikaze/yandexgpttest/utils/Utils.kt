package dev.kamikaze.yandexgpttest.utils

import dev.kamikaze.yandexgpttest.ParsedResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

// Парсинг функций
fun parseJSON(text: String): ParsedResponse? {
    return try {
        val json = Json.decodeFromString<Map<String, JsonElement>>(text)

        ParsedResponse(
            summary = json["summary"]?.toString()?.trim('"') ?: "",
            explanation = json["explanation"]?.toString()?.trim('"') ?: "",
            code = json["code"]?.toString()?.trim('"') ?: "",
            references = json["references"]?.let { element ->
                Json.decodeFromJsonElement<List<String>>(element)
            } ?: emptyList(),
            metadata = mapOf()
        )
    } catch (e: Exception) {
        println("JSON parsing error: ${e.message}")
        null
    }
}

fun parseMarkdown(text: String): ParsedResponse? {
    return try {
        val lines = text.lines()
        var title = ""
        var content = ""
        var summary = ""
        var category = ""

        lines.forEach { line ->
            when {
                line.startsWith("# ") -> title = line.removePrefix("# ").trim()
                line.startsWith("**Краткое описание:**") -> summary = line.removePrefix("**Краткое описание:**").trim()
                line.startsWith("**Категория:**") -> category = line.removePrefix("**Категория:**").trim()
                line.isNotBlank() && !line.startsWith("#") -> content += line + "\n"
            }
        }

        ParsedResponse(
            summary = summary,
            explanation = content.trim(),
            code = "",
            references = emptyList(),
            metadata = mapOf("category" to category, "title" to title)
        )
    } catch (e: Exception) {
        println("Markdown parsing error: ${e.message}")
        null
    }
}

fun parseCSV(text: String): ParsedResponse? {
    return try {
        val parts = text.split(",")
        if (parts.size >= 2) {
            ParsedResponse(
                summary = parts.getOrNull(0) ?: "",
                explanation = parts.getOrNull(1) ?: "",
                code = "",
                references = emptyList(),
                metadata = emptyMap()
            )
        } else null
    } catch (e: Exception) {
        println("CSV parsing error: ${e.message}")
        null
    }
}

fun parseXML(text: String): ParsedResponse? {
    return try {
        val summary = """<summary>([^<]+)</summary>""".toRegex().find(text)?.groupValues?.get(1) ?: ""
        val explanation = """<content>([^<]+)</content>""".toRegex().find(text)?.groupValues?.get(1) ?: ""
        val title = """<title>([^<]+)</title>""".toRegex().find(text)?.groupValues?.get(1) ?: ""

        ParsedResponse(
            summary = summary,
            explanation = explanation,
            code = "",
            references = emptyList(),
            metadata = mapOf("title" to title)
        )
    } catch (e: Exception) {
        println("XML parsing error: ${e.message}")
        null
    }
}