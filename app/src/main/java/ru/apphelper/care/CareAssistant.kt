package ru.apphelper.care

import ru.apphelper.domain.CarePreferences

enum class CareSuggestionType {
    ASK_DIRECTION,
    OFFER_HOME_ROUTE,
    SUGGEST_REST,
}

data class CareContext(
    val leftSafePlaceAtMillis: Long? = null,
    val nowMillis: Long = System.currentTimeMillis(),
    val activeMovementMinutes: Int? = null,
    val currentDestinationKnown: Boolean = false,
    val homeKnown: Boolean = false,
    /** Время последней подсказки каждого типа; хранится локально и используется только для cooldown. */
    val lastSuggestionAt: Map<CareSuggestionType, Long> = emptyMap(),
)

data class CareSuggestion(
    val type: CareSuggestionType,
    val message: String,
)

object CareAssistant {
    fun suggest(
        context: CareContext,
        preferences: CarePreferences,
    ): CareSuggestion? {
        if (!preferences.enabled) return null

        fun allowed(type: CareSuggestionType): Boolean {
            val last = context.lastSuggestionAt[type] ?: return true
            val cooldownMillis = preferences.cooldownMinutes.coerceAtLeast(30) * 60_000L
            return context.nowMillis - last >= cooldownMillis
        }

        if (!context.currentDestinationKnown &&
            preferences.askDirectionEnabled &&
            allowed(CareSuggestionType.ASK_DIRECTION)
        ) {
            return CareSuggestion(
                CareSuggestionType.ASK_DIRECTION,
                "В каком направлении вы хотите двигаться?",
            )
        }

        val awayFromHomeHours = context.leftSafePlaceAtMillis?.let {
            ((context.nowMillis - it).coerceAtLeast(0L) / 3_600_000L).toInt()
        }

        if (context.homeKnown &&
            awayFromHomeHours != null &&
            awayFromHomeHours >= 6 &&
            preferences.offerHomeRouteEnabled &&
            allowed(CareSuggestionType.OFFER_HOME_ROUTE)
        ) {
            return CareSuggestion(
                CareSuggestionType.OFFER_HOME_ROUTE,
                "Вы уже давно вне дома. Если собираетесь в сторону дома, я могу помочь построить маршрут.",
            )
        }

        if ((context.activeMovementMinutes ?: 0) >= 60 &&
            preferences.suggestRestEnabled &&
            allowed(CareSuggestionType.SUGGEST_REST)
        ) {
            return CareSuggestion(
                CareSuggestionType.SUGGEST_REST,
                "Вы уже долго в пути. Если хотите, можно немного отдохнуть.",
            )
        }

        return null
    }
}
