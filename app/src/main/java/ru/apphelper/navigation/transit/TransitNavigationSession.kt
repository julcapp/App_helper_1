package ru.apphelper.navigation.transit

class TransitNavigationSession(
    val route: TransitRoute,
) {
    private var currentLegIndex: Int = 0

    val currentLeg: TransitLeg?
        get() = route.legs.getOrNull(currentLegIndex)

    val isFinished: Boolean
        get() = currentLegIndex >= route.legs.size

    fun currentInstruction(): String? = currentLeg?.let(TransitVoiceGuide::describeLeg)

    fun advance(): String? {
        if (!isFinished) currentLegIndex++
        return currentInstruction()
    }

    fun reset() {
        currentLegIndex = 0
    }

    fun progress(): Pair<Int, Int> =
        (currentLegIndex.coerceAtMost(route.legs.size)) to route.legs.size
}
