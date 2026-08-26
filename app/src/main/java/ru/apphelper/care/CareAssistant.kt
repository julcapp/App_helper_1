package ru.apphelper.care

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
)

data class CareSuggestion(
    val type: CareSuggestionType,
    val message: String,
)

object CareAssistant {
    fun suggest(context: CareContext): CareSuggestion? {
        if (!context.currentDestinationKnown) {
            return CareSuggestion(
                CareSuggestionType.ASK_DIRECTION,
                "В каком направлении вы хотите двигаться?",
            )
        }

        val awayFromHomeHours = context.leftSafePlaceAtMillis?.let {
            ((context.nowMillis - it).coerceAtLeast(0L) / 3_600_000L).toInt()
        }

        if (context.homeKnown && awayFromHomeHours != null && awayFromHomeHours >= 6) {
            return CareSuggestion(
                CareSuggestionType.OFFER_HOME_ROUTE,
                "Вы уже давно вне дома. Если собираетесь в сторону дома, я могу помочь построить маршрут.",
            )
        }

        if ((context.activeMovementMinutes ?: 0) >= 60) {
            return CareSuggestion(
                CareSuggestionType.SUGGEST_REST,
                "Вы уже долго в пути. Если хотите, можно немного отдохнуть.",
            )
        }

        return null
    }
}
