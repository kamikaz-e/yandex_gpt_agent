package dev.kamikaze.yandexgpttest.data

import kotlinx.serialization.Serializable

/**
 * Профиль пользователя для персонализации общения с агентом
 */
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val role: String,
    val experience: String,
    val preferences: UserPreferences,
    val description: String
)

/**
 * Предпочтения пользователя для настройки стиля общения
 */
@Serializable
data class UserPreferences(
    val communicationStyle: CommunicationStyle,
    val technicalLevel: TechnicalLevel,
    val codeStyle: CodeStylePreference? = null,
    val focusAreas: List<String> = emptyList()
)

/**
 * Стиль общения агента с пользователем
 */
@Serializable
enum class CommunicationStyle {
    TECHNICAL,      // Технический, лаконичный стиль
    ANALYTICAL,     // Аналитический, много вопросов
    BALANCED        // Сбалансированный стиль
}

/**
 * Уровень технических знаний
 */
@Serializable
enum class TechnicalLevel {
    JUNIOR,
    MIDDLE,
    SENIOR,
    EXPERT
}

/**
 * Предпочтения по стилю кода (для разработчиков)
 */
@Serializable
data class CodeStylePreference(
    val cleanCodeFocus: Boolean = false,
    val patternsFocus: Boolean = false,
    val preferredFrameworks: List<String> = emptyList()
)

/**
 * Предопределенные профили пользователей
 */
object UserProfiles {

    val ANDROID_DEVELOPER = UserProfile(
        id = "android_dev",
        name = "Android Разработчик",
        role = "Senior Android Developer",
        experience = "10 лет опыта в Android разработке",
        preferences = UserPreferences(
            communicationStyle = CommunicationStyle.TECHNICAL,
            technicalLevel = TechnicalLevel.SENIOR,
            codeStyle = CodeStylePreference(
                cleanCodeFocus = true,
                patternsFocus = true,
                preferredFrameworks = listOf("Jetpack Compose", "Kotlin Coroutines", "Hilt", "Room")
            ),
            focusAreas = listOf(
                "Clean Code",
                "Design Patterns",
                "SOLID принципы",
                "Jetpack Compose",
                "Архитектура (MVVM, MVI)",
                "Оптимизация производительности"
            )
        ),
        description = """
            Senior Android разработчик с 10-летним опытом работы.
            Особое внимание уделяет чистоте кода, следованию паттернам проектирования и SOLID принципам.
            Предпочитает современные подходы: Jetpack Compose для UI, Kotlin Coroutines для асинхронности.
            Ожидает технически точных, лаконичных ответов с примерами кода высокого качества.
        """.trimIndent()
    )

    val ANALYST = UserProfile(
        id = "analyst",
        name = "Системный Аналитик",
        role = "Business Analyst / System Analyst",
        experience = "Опыт в системной аналитике и постановке задач",
        preferences = UserPreferences(
            communicationStyle = CommunicationStyle.ANALYTICAL,
            technicalLevel = TechnicalLevel.MIDDLE,
            codeStyle = null,
            focusAreas = listOf(
                "Системный анализ",
                "Постановка задач",
                "Требования к системе",
                "Use Cases",
                "Проектирование решений",
                "Stakeholder Management"
            )
        ),
        description = """
            Системный аналитик, который смотрит на задачи с точки зрения бизнес-процессов и требований.
            Задает много уточняющих вопросов для правильной постановки задачи.
            Важно понимать контекст, цели, ограничения и критерии успеха.
            Ожидает структурированных ответов с анализом вариантов решения и их последствий.
        """.trimIndent()
    )

    /**
     * Получить все доступные профили
     */
    fun getAllProfiles(): List<UserProfile> = listOf(
        ANDROID_DEVELOPER,
        ANALYST
    )

    /**
     * Получить профиль по ID
     */
    fun getProfileById(id: String): UserProfile? {
        return getAllProfiles().find { it.id == id }
    }

    /**
     * Получить дефолтный профиль (на случай если профиль не выбран)
     */
    fun getDefaultProfile(): UserProfile = ANDROID_DEVELOPER
}
