package ru.apphelper.data

import android.content.Context
import ru.apphelper.domain.AssistanceMode
import ru.apphelper.domain.SpeechPreferences
import ru.apphelper.domain.TrustedContact
import ru.apphelper.domain.UserPermissions
import ru.apphelper.domain.UserProfile

class UserProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_helper_profile", Context.MODE_PRIVATE)

    fun load(): UserProfile {
        val modes = prefs.getStringSet("modes", setOf(AssistanceMode.VOICE_FIRST.name))
            .orEmpty()
            .mapNotNull { runCatching { AssistanceMode.valueOf(it) }.getOrNull() }
            .toSet()
            .ifEmpty { setOf(AssistanceMode.VOICE_FIRST) }

        val interests = prefs.getStringSet("interests", emptySet()).orEmpty()
        val trustedName = prefs.getString("trusted_name", "").orEmpty()
        val trustedPhone = prefs.getString("trusted_phone", "").orEmpty()

        return UserProfile(
            displayName = prefs.getString("display_name", "").orEmpty(),
            assistanceModes = modes,
            speech = SpeechPreferences(
                slowerSpeech = prefs.getBoolean("slower_speech", false),
                shortPhrases = prefs.getBoolean("short_phrases", true),
                waitLongerForAnswer = prefs.getBoolean("wait_longer", false),
                repeatImportantQuestions = prefs.getBoolean("repeat_questions", true),
            ),
            permissions = UserPermissions(
                microphoneGranted = prefs.getBoolean("microphone_granted", false),
            ),
            interests = interests,
            trustedContact = if (trustedName.isNotBlank() || trustedPhone.isNotBlank()) {
                TrustedContact(trustedName, trustedPhone)
            } else null,
            primaryLanguage = prefs.getString("primary_language", "ru") ?: "ru",
            travelLanguages = prefs.getStringSet("travel_languages", setOf("es")).orEmpty(),
            onboardingCompleted = prefs.getBoolean("onboarding_completed", false),
        )
    }

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString("display_name", profile.displayName)
            .putStringSet("modes", profile.assistanceModes.map { it.name }.toSet())
            .putBoolean("slower_speech", profile.speech.slowerSpeech)
            .putBoolean("short_phrases", profile.speech.shortPhrases)
            .putBoolean("wait_longer", profile.speech.waitLongerForAnswer)
            .putBoolean("repeat_questions", profile.speech.repeatImportantQuestions)
            .putBoolean("microphone_granted", profile.permissions.microphoneGranted)
            .putStringSet("interests", profile.interests)
            .putString("trusted_name", profile.trustedContact?.name.orEmpty())
            .putString("trusted_phone", profile.trustedContact?.phone.orEmpty())
            .putString("primary_language", profile.primaryLanguage)
            .putStringSet("travel_languages", profile.travelLanguages)
            .putBoolean("onboarding_completed", profile.onboardingCompleted)
            .apply()
    }
}
