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

fun buildSystemPrompt(settings: AISettings, needTotalResult: Boolean): String {
    val startInstructions = buildGuidedPrompt(
        getStyleInstruction(settings.responseStyle),
        settings.maxLength,
        needTotalResult
    )
    return when (settings.responseFormat) {
        ResponseFormat.JSON -> buildJsonPrompt(startInstructions, needTotalResult)
        ResponseFormat.MARKDOWN -> buildMarkdownPrompt(startInstructions)
        ResponseFormat.CSV -> buildCsvPrompt(startInstructions)
        ResponseFormat.XML -> buildXmlPrompt(startInstructions)
    }
}

private fun buildGuidedPrompt(
    responseStyle: String,
    maxLength: Int,
    needTotalResult: Boolean,
): String {
    return if (needTotalResult) """
       
        Ты - AI аналитик для решения задачи: твоя задача вывод результата по имеющимся данным).
    
        ИТОГОВЫЙ ВЫВОД:
        1. Оцени достоверность: Точность данных должна быть высокая
        2. Сгенерируй результат в стиле $responseStyle и по следующему формату: 
        
        ## Исходная задача
        [Четкая формулировка задачи пользователя]
        
        ## Собранная информация  
        [Структурированный обзор всей информации]
        
        ## Выводы и рекомендации
        [Ключевые инсайты и следующие шаги]
        
        ## План действий
        [Конкретные рекомендации с приоритетами]
        
        ## Риски и ограничения
        [Потенциальные проблемы и способы их избежать]
        
        В конце предложи пользователю ответить на новый вопрос

     ОБЯЗАТЕЛЬНО: Общий объем текста результата не больше $maxLength

        """ else """
      Ты - AI аналитик для решения задачи: [здесь ты определяешь тип задачи по контексту предыдущих запросов] (как
    пример, это может выглядеть как сбор требований ТЗ, но ты об этом не говоришь пользователю).
    Стиль общения: $responseStyle
    
        ПРОЦЕСС:
        1. АНАЛИЗИРУЙ задачу пользователя
        2. СОБИРАЙ недостающую информацию через целевые вопросы
        3. ОТСЛЕЖИВАЙ ПРОГРЕСС И СОСТОЯНИЕ СБОРА ИНФОРМАЦИИ.
        4. При достижении критериев → СРАЗУ ГЕНЕРИРУЙ РЕЗУЛЬТАТ 
        
        КРИТЕРИИ ЗАВЕРШЕНИЯ СБОРА:
        - ПОНЯЛ основную цель/задачу пользователя
        - ВЫЯСНИЛ контекст и фоновую информацию  
        - ОПРЕДЕЛИЛ ключевые параметры и ограничения
        - УЗНАЛ предпочтения и требования
        - ПОНЯЛ ожидания по результату
        - УЧТИЛ доступные ресурсы и ограничения
        - УТОЧНИЛ дедлайны и приоритеты
        - ВЫЯСНИЛ критерии успеха
        - ДОШЛИ ДО МАКСИМУМА ВОПРОСОВ

 
     ОБЯЗАТЕЛЬНО:
        - Будь конкретным и практичным
        - Задавай только 1-2 вопроса за раз
        - Начинай с самых важных деталей
        - После итогового вывода по задаче предложи пользователю решить другой вопрос
        
        ПОНЯЛ ЗАДАЧУ? 
        СТАРТ: Начинай с анализа задачи и первого ключевого вопроса.

    """.trimIndent()
}

private fun buildJsonPrompt(startInstructions: String, needTotalResult: Boolean): String {
    val formatResponse = if (needTotalResult) """
         {
            "summary": "Исходная задача",
            "description": "Собранная информация с деталями",
            "totalResult": "true",
            "references": ["Ссылки на документацию или источники, если применимо"]
        }
        """ else """ 
          {
            "summary": "Краткое резюме (конспект) проблемы которую решаем в 1-2 предложения",
            "description": "Вопросы, которые хотим задать пользователю чтобы продолжить собирать нужную нам 
            информацию для итогового результата",
            "totalResult": "false"
        } 
        """

    return """
        $startInstructions
        ФОРМАТ ОТВЕТА (строго соблюдай, это важно чтобы распарсить данные): 
        $formatResponse
        ВАЖНЫЕ ПРАВИЛА:
        - ВОЗВРАЩАЙ ТОЛЬКО JSON ФОРМАТ
        - БЕЗ эмодзи, БЕЗ форматирования, БЕЗ дополнительного текста
        - БЕЗ markdown, БЕЗ ``` блоков
        - Только чистый валидный JSON!
    """.trimIndent()
}

private fun buildMarkdownPrompt(startInstructions: String): String {
    return """
            Ты - AI помощник. Отвечай ТОЛЬКО в формате Markdown:
            $startInstructions
            # [Заголовок ответа]
            
            [Основной контент]
            
            **Краткое описание:** [краткое резюме в 1-2 предложения]
            **Категория:** [основная категория темы]
            
            Требования:
            - Структурируй информацию логично с заголовками
            - Отвечай ТОЛЬКО валидным Markdown без дополнительного текста
            - Используй заголовки (# ## ###) для структуры
            - Используй списки и жирный текст для акцентов
        """.trimIndent()
}

private fun buildCsvPrompt(startInstructions: String): String {
    return """
            Ты - AI помощник. Отвечай СТРОГО в формате CSV:
            $startInstructions
            ФОРМАТ: заголовок,краткое_описание,детальное_объяснение,категория,ключевые_слова
            
            Требования:
            - БЕЗ кавычек, БЕЗ заголовков таблицы, БЕЗ дополнительного текста
            - Только одна строка с данными через запятую
            - Поля должны содержать сжатую но информативную версию контента
        """.trimIndent()
}

private fun buildXmlPrompt(startInstructions: String): String {
    return """
            Ты - AI помощник. Отвечай ТОЛЬКО в формате XML:
            $startInstructions
            <response>
                <title>заголовок ответа</title>
                <content>основной контент</content>
                <summary>краткое резюме в 1-2 предложения</summary>
                <category>категория темы</category>
                <keywords>ключевое слово1, ключевое слово2</keywords>
            </response>
            
            Требования:
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