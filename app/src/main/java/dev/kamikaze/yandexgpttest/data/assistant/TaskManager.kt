package dev.kamikaze.yandexgpttest.data.assistant

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Менеджер задач с JSON хранилищем
 */
object TaskManager {

    private const val TASKS_FILE = "tasks.json"
    private const val BACKUP_SUFFIX = "_backup"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Загрузить все задачи
     */
    fun loadTasks(context: Context): List<Task> {
        return try {
            val file = File(context.filesDir, TASKS_FILE)
            if (file.exists()) {
                val jsonString = file.readText()
                val tasksData = json.decodeFromString<TasksData>(jsonString)
                tasksData.tasks
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Попробуем загрузить backup
            loadBackup(context)
        }
    }

    /**
     * Сохранить все задачи
     */
    fun saveTasks(context: Context, tasks: List<Task>) {
        try {
            val tasksData = TasksData(
                tasks = tasks,
                savedAt = System.currentTimeMillis(),
                version = "1.0"
            )

            val jsonString = json.encodeToString(TasksData.serializer(), tasksData)
            val file = File(context.filesDir, TASKS_FILE)
            file.writeText(jsonString)

            // Создаем backup
            createBackup(context, jsonString)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Добавить задачу
     */
    fun addTask(context: Context, task: Task): Task {
        val tasks = loadTasks(context).toMutableList()
        val newTask = task.copy(
            id = System.currentTimeMillis() + tasks.size,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        tasks.add(newTask)
        saveTasks(context, tasks)
        return newTask
    }

    /**
     * Обновить задачу
     */
    fun updateTask(context: Context, updatedTask: Task) {
        val tasks = loadTasks(context).toMutableList()
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask.copy(updatedAt = System.currentTimeMillis())
            saveTasks(context, tasks)
        }
    }

    /**
     * Удалить задачу
     */
    fun deleteTask(context: Context, taskId: Long) {
        val tasks = loadTasks(context).toMutableList()
        tasks.removeAll { it.id == taskId }
        saveTasks(context, tasks)
    }

    /**
     * Получить задачу по ID
     */
    fun getTaskById(context: Context, taskId: Long): Task? {
        return loadTasks(context).firstOrNull { it.id == taskId }
    }

    /**
     * Получить задачи по приоритету
     */
    fun getTasksByPriority(context: Context, priority: TaskPriority): List<Task> {
        return loadTasks(context).filter { it.priority == priority }
    }

    /**
     * Получить задачи по статусу
     */
    fun getTasksByStatus(context: Context, status: TaskStatus): List<Task> {
        return loadTasks(context).filter { it.status == status }
    }

    /**
     * Получить высокоприоритетные незавершенные задачи
     */
    fun getHighPriorityTasks(context: Context): List<Task> {
        return loadTasks(context).filter {
            (it.priority == TaskPriority.HIGH || it.priority == TaskPriority.CRITICAL) &&
                    it.status != TaskStatus.DONE
        }.sortedWith(
            compareByDescending<Task> { it.priority.ordinal }
                .thenByDescending { it.createdAt }
        )
    }

    /**
     * Поиск задач по тексту
     */
    fun searchTasks(context: Context, query: String): List<Task> {
        val lowerQuery = query.lowercase()
        return loadTasks(context).filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.description.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Удалить все задачи
     */
    fun clearAllTasks(context: Context) {
        try {
            val file = File(context.filesDir, TASKS_FILE)
            file.delete()

            val backupFile = File(context.filesDir, "$TASKS_FILE$BACKUP_SUFFIX")
            backupFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Создать резервную копию
     */
    private fun createBackup(context: Context, jsonString: String) {
        try {
            val backupFile = File(context.filesDir, "$TASKS_FILE$BACKUP_SUFFIX")
            backupFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Загрузить резервную копию
     */
    private fun loadBackup(context: Context): List<Task> {
        return try {
            val backupFile = File(context.filesDir, "$TASKS_FILE$BACKUP_SUFFIX")
            if (backupFile.exists()) {
                val jsonString = backupFile.readText()
                val tasksData = json.decodeFromString<TasksData>(jsonString)
                tasksData.tasks
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

/**
 * Модель данных для сохранения задач в JSON
 */
@Serializable
data class TasksData(
    val tasks: List<Task>,
    val savedAt: Long,
    val version: String
)
