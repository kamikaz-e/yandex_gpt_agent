package dev.kamikaze.yandexgpttest.data.prompt

import dev.kamikaze.yandexgpttest.data.ParsedResponse
import dev.kamikaze.yandexgpttest.ui.AISettings
import dev.kamikaze.yandexgpttest.utils.parseCSV
import dev.kamikaze.yandexgpttest.utils.parseJSON
import dev.kamikaze.yandexgpttest.utils.parseMarkdown
import dev.kamikaze.yandexgpttest.utils.parseXML

enum class ResponseFormat(val displayName: String) {
    JSON("JSON") {
        override fun parse(text: String) = parseJSON(text)
    },
    MARKDOWN("Markdown") {
        override fun parse(text: String) = parseMarkdown(text)
    },
    CSV("CSV") {
        override fun parse(text: String) = parseCSV(text)
    },
    XML("XML") {
        override fun parse(text: String) = parseXML(text)
    };

    abstract fun parse(text: String): ParsedResponse?
}

enum class ResponseStyle(val displayName: String) {
    FORMAL("Формальный"),
    INFORMAL("Неформальный"),
    NEUTRAL("Нейтральный"),
    CREATIVE("Креативный"),
    TECHNICAL("Технический")
}

fun buildSystemPrompt(settings: AISettings): String {
    return when (settings.responseFormat) {
        ResponseFormat.JSON -> buildJsonPrompt(settings)
        ResponseFormat.MARKDOWN -> buildMarkdownPrompt(settings)
        ResponseFormat.CSV -> buildCsvPrompt(settings)
        ResponseFormat.XML -> buildXmlPrompt(settings)
    }
}

private fun buildJsonPrompt(settings: AISettings): String {
    val styleInstruction = getStyleInstruction(settings.responseStyle)

    return """
        Ты - AI помощник. ОТВЕЧАЙ ТОЛЬКО ЧИСТЫМ JSON БЕЗ ДОПОЛНИТЕЛЬНОГО ФОРМАТИРОВАНИЯ!
        
        Проанализируй текущее сообщение и все предыдущие по этой теме и задай сеебе вопрос хватит ли тебе эти данных 
        для решения вопроса пользователя?  Если из переписки тебе уже достаточно данных (тоесть получил ответы на нужные вопросы),
        то выдавай  ответ по следующей форме.
        
        ФОРМАТ ОТВЕТА:
        {
            "summary": "Краткое резюме ответа в 1-2 предложения",
            "explanation": "Подробное объяснение с техническими деталями",
            "references": ["Ссылки на документацию или источники, если применимо"]
        }
        
        ВАЖНЫЕ ПРАВИЛА:
        - ВОЗВРАЩАЙ ТОЛЬКО JSON НАЧИНАЮЩИЙСЯ С {
        - БЕЗ эмодзи, БЕЗ форматирования, БЕЗ дополнительного текста
        - БЕЗ markdown, БЕЗ ``` блоков
        - Только чистый валидный JSON!
        - Общий объем: не более ${settings.maxLength} символов
        
        СТИЛЬ ОБЩЕНИЯ:
        $styleInstruction
    """.trimIndent()
}

private fun buildMarkdownPrompt(settings: AISettings): String {
    val styleInstruction = getStyleInstruction(settings.responseStyle)

    return """
            Ты - AI помощник. Отвечай ТОЛЬКО в формате Markdown:
            
            # [Заголовок ответа]
            
            [Основной контент]
            
            **Краткое описание:** [краткое резюме в 1-2 предложения]
            **Категория:** [основная категория темы]
            
            Требования:
            - Общий объем текста не более ${settings.maxLength} символов
            - Структурируй информацию логично с заголовками
            $styleInstruction
            - Отвечай ТОЛЬКО валидным Markdown без дополнительного текста
            - Используй заголовки (# ## ###) для структуры
            - Используй списки и жирный текст для акцентов
        """.trimIndent()
}

private fun buildCsvPrompt(settings: AISettings): String {
    val styleInstruction = getStyleInstruction(settings.responseStyle)

    return """
            Ты - AI помощник. Отвечай СТРОГО в формате CSV:
            
            ФОРМАТ: заголовок,краткое_описание,детальное_объяснение,категория,ключевые_слова
            
            Требования:
            - Общий объем текста не более ${settings.maxLength} символов
            $styleInstruction
            - БЕЗ кавычек, БЕЗ заголовков таблицы, БЕЗ дополнительного текста
            - Только одна строка с данными через запятую
            - Поля должны содержать сжатую но информативную версию контента
        """.trimIndent()
}

private fun buildXmlPrompt(settings: AISettings): String {
    val styleInstruction = getStyleInstruction(settings.responseStyle)

    return """
            Ты - AI помощник. Отвечай ТОЛЬКО в формате XML:
            
            <response>
                <title>заголовок ответа</title>
                <content>основной контент</content>
                <summary>краткое резюме в 1-2 предложения</summary>
                <category>категория темы</category>
                <keywords>ключевое слово1, ключевое слово2</keywords>
            </response>
            
            Требования:
            - Общий объем текста не более ${settings.maxLength} символов
            $styleInstruction
            - БЕЗ дополнительного текста, только валидный XML
            - Структурируй информацию логично между тегами
            - Используй самозакрывающиеся теги где возможно
        """.trimIndent()
}


private fun getStyleInstruction(style: ResponseStyle): String {
    return when (style) {
        ResponseStyle.FORMAL -> """
            Тон: официальный деловой
            Лексика: точная, корректная, профессиональная
            Структура: логичная, систематизированная
            Избегай: сокращений, жаргона, эмоциональных оценок
        """.trimIndent()

        ResponseStyle.INFORMAL -> """
            Тон: дружеский, разговорный
            Лексика: простые понятные слова, допустимы сокращения
            Структура: свободная, живая
            Можно: легкий юмор, ирония, метафоры
        """.trimIndent()

        ResponseStyle.NEUTRAL -> """
            Тон: объективный, сбалансированный
            Лексика: нейтральная, фактическая
            Структура: стандартная деловая
            Избегай: эмоциональных оценок, субъективности
        """.trimIndent()

        ResponseStyle.CREATIVE -> """
            Тон: креативный, оригинальный
            Лексика: яркая, образная, выразительная
            Структура: нестандартная, игривая
            Можно: метафоры, сравнения, неожиданные подходы
        """.trimIndent()

        ResponseStyle.TECHNICAL -> """
            Тон: профессиональный технический
            Лексика: точная терминология области
            Структура: четкая, детализированная
            Обязательно: конкретные примеры, ссылки на стандарты
        """.trimIndent()
    }
}