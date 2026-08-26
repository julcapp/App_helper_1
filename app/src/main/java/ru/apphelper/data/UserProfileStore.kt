package ru.apphelper.data

import android.content.Context
import ru.apphelper.domain.AssistanceMode
import ru.apphelper.domain.CarePreferences
import ru.apphelper.domain.SpeechPreferences
import ru.apphelper.domain.TrustedContact
import ru.apphelper.domain.UserPermissions
import ru.apphelper.domain.UserProfile
import ru.apphelper.profile.ProfilePriorities
import ru.apphelper.profile.ProfilePriority

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
        val defaultPriorities = ProfilePriorities()
        val priorities = ProfilePriorities(
            weights = ProfilePriority.entries.associateWith { priority ->
                prefs.getInt(
                    "priority_${priority.name.lowercase()}",
                    defaultPriorities.weight(priority),
                ).coerceIn(0, 100)
            },
        )

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
            care = CarePreferences(
                enabled = prefs.getBoolean("care_enabled", true),
                askDirectionEnabled = prefs.getBoolean("care_ask_direction", true),
                offerHomeRouteEnabled = prefs.getBoolean("care_offer_home_route", true),
                suggestRestEnabled = prefs.getBoolean("care_suggest_rest", true),
                cooldownMinutes = prefs.getInt("care_cooldown_minutes", 180).coerceAtLeast(30),
            ),
            priorities = priorities,
            interests = interests,
            trustedContact = if (trustedName.isNotBlank() || trustedPhone.isNotBlank()) {
                TrustedContact(trustedName, trustedPhone)
            } else null,
            currentCityName = prefs.getString("current_city_name", "").orEmpty(),
            primaryLanguage = prefs.getString("primary_language", "ru") ?: "ru",
            travelLanguages = prefs.getStringSet("travel_languages", setOf("es")).orEmpty(),
            onboardingCompleted = prefs.getBoolean("onboarding_completed", false),
        )
    }

    fun save(profile: UserProfile) {
        val editor = prefs.edit()
            .putString("display_name", profile.displayName)
            .putStringSet("modes", profile.assistanceModes.map { it.name }.toSet())
            .putBoolean("slower_speech", profile.speech.slowerSpeech)
            .putBoolean("short_phrases", profile.speech.shortPhrases)
            .putBoolean("wait_longer", profile.speech.waitLongerForAnswer)
            .putBoolean("repeat_questions", profile.speech.repeatImportantQuestions)
            .putBoolean("microphone_granted", profile.permissions.microphoneGranted)
            .putBoolean("care_enabled", profile.care.enabled)
            .putBoolean("care_ask_direction", profile.care.askDirectionEnabled)
            .putBoolean("care_offer_home_route", profile.care.offerHomeRouteEnabled)
            .putBoolean("care_suggest_rest", profile.care.suggestRestEnabled)
            .putInt("care_cooldown_minutes", profile.care.cooldownMinutes)
            .putStringSet("interests", profile.interests)
            .putString("trusted_name", profile.trustedContact?.name.orEmpty())
            .putString("trusted_phone", profile.trustedContact?.phone.orEmpty())
            .putString("current_city_name", profile.currentCityName)
            .putString("primary_language", profile.primaryLanguage)
            .putStringSet("travel_languages", profile.travelLanguages)
            .putBoolean("onboarding_completed", profile.onboardingCompleted)

        ProfilePriority.entries.forEach { priority ->
            editor.putInt(
                "priority_${priority.name.lowercase()}",
                profile.priorities.weight(priority),
            )
        }
        editor.apply()
    }
}
