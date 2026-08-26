package ru.apphelper.navigation.transit

/**
 * Не допускает повторного озвучивания одного и того же предупреждения
 * при частых обновлениях позиции/транспортного прогресса.
 */
class TransitRideAlertTracker {
    private var legIdentity: String? = null
    private val announcedStopsRemaining = mutableSetOf<Int>()

    fun onProgress(leg: TransitLeg, stopsRemaining: Int): String? {
        val currentIdentity = buildString {
            append(leg.mode.name)
            append('|')
            append(leg.routeNumber.orEmpty())
            append('|')
            append(leg.from?.name.orEmpty())
            append('|')
            append(leg.to?.name.orEmpty())
        }

        if (legIdentity != currentIdentity) {
            legIdentity = currentIdentity
            announcedStopsRemaining.clear()
        }

        if (stopsRemaining !in setOf(2, 1, 0)) return null
        if (!announcedStopsRemaining.add(stopsRemaining)) return null

        return TransitVoiceGuide.stopsRemainingAlert(leg, stopsRemaining)
    }

    fun reset() {
        legIdentity = null
        announcedStopsRemaining.clear()
    }
}
