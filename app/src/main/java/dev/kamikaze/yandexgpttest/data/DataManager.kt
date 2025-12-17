package dev.kamikaze.yandexgpttest.data

import android.content.Context
import dev.kamikaze.yandexgpttest.ui.UserMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object DataManager {

    private const val CHAT_DATA_FILE = "chat_memory.json"
    private const val BACKUP_SUFFIX = "_backup"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Сохраняет все данные чата в JSON файл
     */
    fun saveChatData(
        context: Context,
        messages: List<UserMessage>,
        totalTokenStats: TokenStats,
        compactionConfig: CompactionConfig,
        compactionStats: CompactionStats,
        currentUserProfile: UserProfile? = null,
        apiSettings: ApiSettings = ApiSettings()
    ) {
        try {
            val chatData = ChatMemoryData(
                messages = messages,
                totalTokenStats = totalTokenStats,
                compactionConfig = compactionConfig,
                compactionStats = compactionStats,
                currentUserProfile = currentUserProfile,
                apiSettings = apiSettings,
                savedAt = System.currentTimeMillis(),
                version = "1.0"
            )

            val jsonString = json.encodeToString(chatData)
            val file = File(context.filesDir, CHAT_DATA_FILE)
            file.writeText(jsonString)

            // Создаем backup
            createBackup(context, jsonString)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Загружает данные чата из JSON файла
     */
    fun loadChatData(context: Context): ChatMemoryData? {
        return try {
            val file = File(context.filesDir, CHAT_DATA_FILE)
            if (file.exists()) {
                val jsonString = file.readText()
                json.decodeFromString<ChatMemoryData>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Попробуем загрузить backup
            loadBackup(context)
        }
    }

    /**
     * Проверяет, есть ли сохраненные данные
     */
    fun hasSavedData(context: Context): Boolean {
        val file = File(context.filesDir, CHAT_DATA_FILE)
        return file.exists() && file.length() > 0
    }

    /**
     * Удаляет сохраненные данные
     */
    fun clearChatData(context: Context) {
        try {
            val file = File(context.filesDir, CHAT_DATA_FILE)
            file.delete()

            // Также удаляем backup
            val backupFile = File(context.filesDir, "$CHAT_DATA_FILE$BACKUP_SUFFIX")
            backupFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Создает резервную копию данных
     */
    private fun createBackup(context: Context, jsonString: String) {
        try {
            val backupFile = File(context.filesDir, "$CHAT_DATA_FILE$BACKUP_SUFFIX")
            backupFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Загружает данные из резервной копии
     */
    private fun loadBackup(context: Context): ChatMemoryData? {
        return try {
            val backupFile = File(context.filesDir, "$CHAT_DATA_FILE$BACKUP_SUFFIX")
            if (backupFile.exists()) {
                val jsonString = backupFile.readText()
                json.decodeFromString<ChatMemoryData>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает информацию о сохраненных данных
     */
    fun getStorageInfo(context: Context): StorageInfo {
        return try {
            val file = File(context.filesDir, CHAT_DATA_FILE)
            val backupFile = File(context.filesDir, "$CHAT_DATA_FILE$BACKUP_SUFFIX")

            StorageInfo(
                hasData = file.exists(),
                mainFileSize = if (file.exists()) file.length() else 0,
                backupFileSize = if (backupFile.exists()) backupFile.length() else 0,
                lastSaved = if (file.exists()) file.lastModified() else 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            StorageInfo()
        }
    }
}

/**
 * Модель данных для сохранения в JSON
 */
@Serializable
data class ChatMemoryData(
    val messages: List<UserMessage>,
    val totalTokenStats: TokenStats,
    val compactionConfig: CompactionConfig,
    val compactionStats: CompactionStats,
    val currentUserProfile: UserProfile? = null,
    val apiSettings: ApiSettings = ApiSettings(),
    val savedAt: Long,
    val version: String
)

/**
 * Информация о хранилище
 */
data class StorageInfo(
    val hasData: Boolean = false,
    val mainFileSize: Long = 0,
    val backupFileSize: Long = 0,
    val lastSaved: Long = 0
) {
    fun getFormattedSize(): String {
        val totalSize = mainFileSize + backupFileSize
        return when {
            totalSize < 1024 -> "$totalSize B"
            totalSize < 1024 * 1024 -> "${totalSize / 1024} KB"
            else -> "${totalSize / (1024 * 1024)} MB"
        }
    }

    fun getFormattedDate(): String {
        if (lastSaved == 0L) return "Никогда"
        return SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        ).format(java.util.Date(lastSaved))
    }
}