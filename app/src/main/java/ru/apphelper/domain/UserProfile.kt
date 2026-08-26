package ru.apphelper.domain

import ru.apphelper.profile.ProfilePriorities

enum class AssistanceMode {
    LOW_VISION,
    SIMPLE_PHONE,
    VOICE_FIRST
}

data class SpeechPreferences(
    val slowerSpeech: Boolean = false,
    val shortPhrases: Boolean = true,
    val waitLongerForAnswer: Boolean = false,
    val repeatImportantQuestions: Boolean = true,
)

data class UserPermissions(
    val microphoneGranted: Boolean = false,
    val readNotifications: Boolean = false,
    val callContactsWithConfirmation: Boolean = true,
    val sendMessagesWithConfirmation: Boolean = true,
    val shareLocationWithTrustedContactsOnly: Boolean = true,
    val automaticRepliesEnabled: Boolean = false,
)

data class CarePreferences(
    /** Общий выключатель ненавязчивых заботливых подсказок. */
    val enabled: Boolean = true,
    /** Можно ли спрашивать направление, когда цель движения не определена. */
    val askDirectionEnabled: Boolean = true,
    /** Можно ли предлагать помощь с маршрутом домой после длительного отсутствия. */
    val offerHomeRouteEnabled: Boolean = true,
    /** Можно ли предлагать короткий отдых после длительного активного движения. */
    val suggestRestEnabled: Boolean = true,
    /** Минимальный интервал между повторными заботливыми подсказками одного типа. */
    val cooldownMinutes: Int = 180,
)

data class TrustedContact(
    val name: String = "",
    val phone: String = "",
)

data class SafePlace(
    val title: String = "",
    val address: String = "",
)

data class UserProfile(
    val displayName: String = "",
    val assistanceModes: Set<AssistanceMode> = setOf(AssistanceMode.VOICE_FIRST),
    val speech: SpeechPreferences = SpeechPreferences(),
    val permissions: UserPermissions = UserPermissions(),
    val care: CarePreferences = CarePreferences(),
    /** Весовые приоритеты функций 0–100. Не являются диагнозом или возрастной категорией. */
    val priorities: ProfilePriorities = ProfilePriorities(),
    val interests: Set<String> = emptySet(),
    val trustedContact: TrustedContact? = null,
    val safePlaces: List<SafePlace> = emptyList(),
    /** Текущий город, подтверждённый пользователем голосом или через интерфейс. */
    val currentCityName: String = "",
    val primaryLanguage: String = "ru",
    val travelLanguages: Set<String> = setOf("es"),
    val onboardingCompleted: Boolean = false,
)
