package ru.apphelper.domain

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
    val interests: Set<String> = emptySet(),
    val trustedContact: TrustedContact? = null,
    val safePlaces: List<SafePlace> = emptyList(),
    val primaryLanguage: String = "ru",
    val travelLanguages: Set<String> = setOf("es"),
    val onboardingCompleted: Boolean = false,
)
