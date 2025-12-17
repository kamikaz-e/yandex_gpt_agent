package dev.kamikaze.yandexgpttest.data.assistant

import android.content.Context
import android.util.Log
import java.util.regex.Pattern

/**
 * Ассистент команды для управления задачами
 */
class TaskAssistant(private val context: Context) {

    companion object {
        private const val TAG = "TaskAssistant"
    }

    /**
     * Обработать сообщение пользователя
     */
    suspend fun processMessage(message: String): TaskAssistantResponse {
        val lowerMessage = message.lowercase()

        return when {
            // Создание задачи
            lowerMessage.contains("создать задач") ||
            lowerMessage.contains("создай задач") ||
            lowerMessage.contains("добавить задач") ||
            lowerMessage.contains("добавь задач") ||
            lowerMessage.contains("новая задач") ||
            lowerMessage.contains("create task") ||
            lowerMessage.contains("add task") ||
            lowerMessage.contains("new task") -> {
                handleTaskCreation(message)
            }

            // Показать задачи
            lowerMessage.contains("покажи задач") ||
            lowerMessage.contains("список задач") ||
            lowerMessage.contains("show task") ||
            lowerMessage.contains("list task") ||
            lowerMessage.contains("все задачи") -> {
                handleTaskListing(message)
            }

            // Рекомендации
            lowerMessage.contains("что делать") ||
            lowerMessage.contains("с чего начать") ||
            lowerMessage.contains("приоритет") ||
            lowerMessage.contains("важно") ||
            lowerMessage.contains("what to do") ||
            lowerMessage.contains("priority") ||
            lowerMessage.contains("important") -> {
                handleRecommendations()
            }

            else -> TaskAssistantResponse(
                success = false,
                message = "Не понял команду. Попробуйте:\n" +
                        "• Создать задачу: [название]\n" +
                        "• Покажи задачи\n" +
                        "• Что делать в первую очередь?"
            )
        }
    }

    /**
     * Создать задачу из текста
     */
    private suspend fun handleTaskCreation(message: String): TaskAssistantResponse {
        try {
            val taskInfo = parseTaskFromMessage(message)

            val task = Task(
                title = taskInfo.title,
                description = taskInfo.description,
                priority = taskInfo.priority,
                status = TaskStatus.TODO,
                tags = taskInfo.tags
            )

            val createdTask = TaskManager.addTask(context, task)

            return TaskAssistantResponse(
                success = true,
                message = formatTaskCreated(createdTask),
                task = createdTask
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания задачи", e)
            return TaskAssistantResponse(
                success = false,
                message = "Ошибка создания задачи: ${e.message}"
            )
        }
    }

    /**
     * Показать список задач
     */
    private suspend fun handleTaskListing(message: String): TaskAssistantResponse {
        val lowerMessage = message.lowercase()

        val tasks = when {
            lowerMessage.contains("высок") || lowerMessage.contains("high") ||
            lowerMessage.contains("важн") || lowerMessage.contains("priority") -> {
                TaskManager.getHighPriorityTasks(context)
            }
            lowerMessage.contains("блок") || lowerMessage.contains("blocked") -> {
                TaskManager.getTasksByStatus(context, TaskStatus.BLOCKED)
            }
            lowerMessage.contains("в работе") || lowerMessage.contains("in progress") -> {
                TaskManager.getTasksByStatus(context, TaskStatus.IN_PROGRESS)
            }
            else -> {
                TaskManager.loadTasks(context)
            }
        }

        return if (tasks.isEmpty()) {
            TaskAssistantResponse(
                success = true,
                message = "📋 Задач пока нет"
            )
        } else {
            TaskAssistantResponse(
                success = true,
                message = formatTaskList(tasks),
                tasks = tasks
            )
        }
    }

    /**
     * Дать рекомендации по приоритетам
     */
    private suspend fun handleRecommendations(): TaskAssistantResponse {
        val highPriorityTasks = TaskManager.getHighPriorityTasks(context)
        val blockedTasks = TaskManager.getTasksByStatus(context, TaskStatus.BLOCKED)

        val recommendations = buildString {
            appendLine("💡 Рекомендации:")
            appendLine()

            if (highPriorityTasks.isNotEmpty()) {
                appendLine("🔥 Высокоприоритетные задачи (${highPriorityTasks.size}):")
                highPriorityTasks.take(3).forEachIndexed { index, task ->
                    appendLine("${index + 1}. [${task.priority}] ${task.title}")
                }
                appendLine()
            }

            if (blockedTasks.isNotEmpty()) {
                appendLine("⚠️ Заблокированные задачи (${blockedTasks.size}):")
                blockedTasks.take(3).forEach { task ->
                    appendLine("• ${task.title}")
                }
                appendLine()
            }

            if (highPriorityTasks.isEmpty() && blockedTasks.isEmpty()) {
                appendLine("✅ Все в порядке! Критичных задач нет.")
            } else if (highPriorityTasks.isNotEmpty()) {
                appendLine("👉 Рекомендую начать с: ${highPriorityTasks.first().title}")
            }
        }

        return TaskAssistantResponse(
            success = true,
            message = recommendations,
            tasks = highPriorityTasks
        )
    }

    /**
     * Парсинг задачи из сообщения
     */
    private fun parseTaskFromMessage(message: String): TaskInfo {
        val title: String
        val description: String
        val priority: TaskPriority
        val tags = mutableListOf<String>()

        // Убираем команду создания
        var cleanMessage = message
            .replace(Regex("создать задач[у|и]?:?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("создай задач[у|и]?:?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("добавить задач[у|и]?:?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("create task:?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("add task:?", RegexOption.IGNORE_CASE), "")
            .trim()

        // Определяем приоритет
        priority = when {
            cleanMessage.contains(Regex("critical|критич", RegexOption.IGNORE_CASE)) -> {
                cleanMessage = cleanMessage.replace(Regex("critical|критич[еский|ная]?", RegexOption.IGNORE_CASE), "")
                TaskPriority.CRITICAL
            }
            cleanMessage.contains(Regex("high|высок", RegexOption.IGNORE_CASE)) -> {
                cleanMessage = cleanMessage.replace(Regex("high|высок[ий|ая]?", RegexOption.IGNORE_CASE), "")
                TaskPriority.HIGH
            }
            cleanMessage.contains(Regex("low|низк", RegexOption.IGNORE_CASE)) -> {
                cleanMessage = cleanMessage.replace(Regex("low|низк[ий|ая]?", RegexOption.IGNORE_CASE), "")
                TaskPriority.LOW
            }
            else -> TaskPriority.MEDIUM
        }

        // Убираем слово "приоритет"
        cleanMessage = cleanMessage.replace(Regex("приоритет[ом|а]?|priority", RegexOption.IGNORE_CASE), "").trim()

        // Ищем описание после "Описание:" или "Description:"
        val descriptionPattern = Pattern.compile("(?:описание|description)\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val descriptionMatcher = descriptionPattern.matcher(cleanMessage)

        if (descriptionMatcher.find()) {
            description = descriptionMatcher.group(1)?.trim() ?: ""
            title = cleanMessage.substring(0, descriptionMatcher.start()).trim()
        } else {
            // Если нет явного описания, первое предложение - название, остальное - описание
            val sentences = cleanMessage.split(Regex("[.!]"))
            title = sentences.firstOrNull()?.trim() ?: cleanMessage
            description = if (sentences.size > 1) {
                sentences.drop(1).joinToString(". ").trim()
            } else {
                title
            }
        }

        return TaskInfo(
            title = title.ifEmpty { "Новая задача" },
            description = description.ifEmpty { title },
            priority = priority,
            tags = tags
        )
    }

    /**
     * Форматирование созданной задачи
     */
    private fun formatTaskCreated(task: Task): String {
        return buildString {
            appendLine("✅ Задача создана!")
            appendLine()
            appendLine("ID: ${task.id}")
            appendLine("📝 ${task.title}")
            appendLine("📄 ${task.description}")
            appendLine("🎯 Приоритет: ${task.priority}")
            appendLine("📌 Статус: ${task.status}")
        }
    }

    /**
     * Форматирование списка задач
     */
    private fun formatTaskList(tasks: List<Task>): String {
        return buildString {
            appendLine("📋 Задачи (${tasks.size}):")
            appendLine()

            tasks.forEachIndexed { index, task ->
                val priorityEmoji = when (task.priority) {
                    TaskPriority.CRITICAL -> "🔴"
                    TaskPriority.HIGH -> "🟠"
                    TaskPriority.MEDIUM -> "🟡"
                    TaskPriority.LOW -> "🟢"
                }

                val statusEmoji = when (task.status) {
                    TaskStatus.TODO -> "📝"
                    TaskStatus.IN_PROGRESS -> "⚙️"
                    TaskStatus.IN_REVIEW -> "👀"
                    TaskStatus.DONE -> "✅"
                    TaskStatus.BLOCKED -> "🚫"
                }

                appendLine("${index + 1}. $priorityEmoji $statusEmoji ${task.title}")
                appendLine("   ${task.description.take(80)}${if (task.description.length > 80) "..." else ""}")
                if (index < tasks.size - 1) appendLine()
            }
        }
    }

    /**
     * Получить все задачи
     */
    fun getAllTasks(): List<Task> = TaskManager.loadTasks(context)

    /**
     * Получить высокоприоритетные задачи
     */
    fun getHighPriorityTasks(): List<Task> = TaskManager.getHighPriorityTasks(context)
}

/**
 * Информация о задаче
 */
private data class TaskInfo(
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val tags: List<String>
)

/**
 * Ответ ассистента
 */
data class TaskAssistantResponse(
    val success: Boolean,
    val message: String,
    val task: Task? = null,
    val tasks: List<Task>? = null
)
