package dev.kamikaze.yandexgpttest.data.assistant

import kotlinx.serialization.Serializable

/**
 * Модель задачи для ассистента команды
 */
@Serializable
data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val assignee: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val tags: List<String> = emptyList()
)

/**
 * Приоритет задачи
 */
@Serializable
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    companion object {
        fun fromString(value: String): TaskPriority {
            return when (value.lowercase()) {
                "low", "низкий" -> LOW
                "medium", "средний" -> MEDIUM
                "high", "высокий" -> HIGH
                "critical", "критический", "критичный" -> CRITICAL
                else -> MEDIUM
            }
        }
    }
}

/**
 * Статус задачи
 */
@Serializable
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    BLOCKED;

    companion object {
        fun fromString(value: String): TaskStatus {
            return when (value.lowercase().replace("_", "").replace(" ", "")) {
                "todo", "новая", "сделать" -> TODO
                "inprogress", "вработе", "процесс" -> IN_PROGRESS
                "inreview", "наревью", "проверка" -> IN_REVIEW
                "done", "готово", "выполнено" -> DONE
                "blocked", "заблокирована", "блок" -> BLOCKED
                else -> TODO
            }
        }
    }
}

/**
 * Запрос на создание задачи
 */
@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assignee: String? = null,
    val dueDate: Long? = null,
    val tags: List<String> = emptyList()
)

/**
 * Фильтр для поиска задач
 */
@Serializable
data class TaskQuery(
    val priority: TaskPriority? = null,
    val status: TaskStatus? = null,
    val assignee: String? = null,
    val tags: List<String>? = null,
    val limit: Int = 10
)
