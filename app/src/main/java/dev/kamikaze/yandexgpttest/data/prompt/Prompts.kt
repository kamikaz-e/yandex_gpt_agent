package dev.kamikaze.yandexgpttest.data.prompt

enum class AgentType(val displayName: String) {
    YANDEXGPT_RC("YandexGPT RC"),
    YANDEXGPT_LATEST("YandexGPT Latest")
}

// Основной промпт для работы с экспертами
fun buildExpertSystemPrompt(agentType: AgentType): String {
    return """
        ТЫ - ${agentType.displayName}, AI-агент, который организует работу группы экспертов для решения сложных задач.

        Твоя задача: Организовать работу ТРЕХ экспертов для анализа и решения предоставленной задачи.
        
        КАЖДЫЙ ЭКСПЕРТ ДОЛЖЕН ДАТЬ ОТВЕТЫ В ТРЕХ ФОРМАТАХ:
        
        1. ПРЯМОЙ ОТВЕТ - краткое, конкретное решение без объяснений
        2. ПОШАГОВОЕ РЕШЕНИЕ - детальный разбор с этапами и обоснованиями  
        3. ОТВЕТ НА CROSS-MODEL ПРОМПТ - решение, основанное на промпте от другой модели
        
        ФОРМАТ ОТВЕТА (строго соблюдай):
        
        ЭКСПЕРТ 1: [Имя] - [Роль]
        📋 ПРЯМОЙ ОТВЕТ:
        [краткое решение]
        
        🔍 ПОШАГОВОЕ РЕШЕНИЕ:
        Шаг 1: [действие]
        Шаг 2: [действие]  
        Шаг 3: [действие]
        [объяснение каждого шага]
        
        🌐 ОТВЕТ НА CROSS-МОДЕЛЬ ПРОМПТ:
        [ответ на промпт от ${if (agentType == AgentType.YANDEXGPT_RC) AgentType.YANDEXGPT_LATEST.displayName else AgentType.YANDEXGPT_RC.displayName}]
        
        ЭКСПЕРТ 2: [Имя] - [Роль]
        [тот же формат для второго эксперта]
        
        ЭКСПЕРТ 3: [Имя] - [Роль]
        [тот же формат для третьего эксперта]
        
        ЭКСПЕРТЫ ДОЛЖНЫ ИМЕТЬ РАЗНЫЕ СПЕЦИАЛИЗАЦИИ:
        - Эксперт 1: Аналитик/Стратег
        - Эксперт 2: Технический специалист  
        - Эксперт 3: Креативный мыслитель/Инноватор
        
        ВАЖНО: Каждый эксперт должен дать УНИКАЛЬНУЮ перспективу на задачу.
        НЕ повторяй мнения других экспертов, добавляй новые инсайты.
    """.trimIndent()
}


// Функция для извлечения экспертных ответов из общего ответа
fun parseExpertResponses(response: String): List<ExpertResponse> {
    val experts = mutableListOf<ExpertResponse>()
    val expertBlocks = response.split("ЭКСПЕРТ ").filter { it.isNotBlank() }

    for (block in expertBlocks) {
        val lines = block.lines()
        if (lines.size < 4) continue

        val expertHeader = lines[0] // "1: [Имя] - [Роль] ==="
        val expertName = expertHeader.substringAfter(":").substringBefore("-").trim()

        var currentSection = ""
        val directAnswer = StringBuilder()
        val stepByStepSolution = StringBuilder()
        val crossModelAnswer = StringBuilder()

        for (line in lines.drop(1)) {
            when {
                line.startsWith("📋 ПРЯМОЙ ОТВЕТ:") -> currentSection = "direct"
                line.startsWith("🔍 ПОШАГОВОЕ РЕШЕНИЕ:") -> currentSection = "steps"
                line.startsWith("🌐 ОТВЕТ НА CROSS-МОДЕЛЬ ПРОМПТ:") -> currentSection = "cross"
                line.startsWith("===") -> {
                    if (directAnswer.isNotEmpty() || stepByStepSolution.isNotEmpty() || crossModelAnswer.isNotEmpty()) {
                        experts.add(
                            ExpertResponse(
                                expertName = expertName,
                                directAnswer = directAnswer.toString().trim(),
                                stepByStepSolution = stepByStepSolution.toString().trim(),
                                crossModelAnswer = crossModelAnswer.toString().trim()
                            )
                        )
                    }
                    break
                }

                currentSection == "direct" && line.isNotBlank() -> directAnswer.appendLine(line)
                currentSection == "steps" && line.isNotBlank() -> stepByStepSolution.appendLine(line)
                currentSection == "cross" && line.isNotBlank() -> crossModelAnswer.appendLine(line)
            }
        }

        // Добавляем последнего эксперта если он есть
        if (directAnswer.isNotEmpty() || stepByStepSolution.isNotEmpty() || crossModelAnswer.isNotEmpty()) {
            experts.add(
                ExpertResponse(
                    expertName = expertName,
                    directAnswer = directAnswer.toString().trim(),
                    stepByStepSolution = stepByStepSolution.toString().trim(),
                    crossModelAnswer = crossModelAnswer.toString().trim()
                )
            )
        }
    }
    return experts
}

data class ExpertResponse(
    val expertName: String,
    val directAnswer: String,
    val stepByStepSolution: String,
    val crossModelAnswer: String,
)