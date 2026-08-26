package ru.apphelper.profile

enum class ProfilePriority {
    MESSAGES,
    VOICE,
    NAVIGATION,
    CARE,
    CALLS,
    CAMERA,
    TRANSLATION,
}

data class ProfilePriorities(
    val weights: Map<ProfilePriority, Int> = defaultWeights(),
) {
    fun weight(priority: ProfilePriority): Int = weights[priority]?.coerceIn(0, 100) ?: 0

    fun withWeight(priority: ProfilePriority, value: Int): ProfilePriorities =
        copy(weights = weights + (priority to value.coerceIn(0, 100)))

    companion object {
        fun defaultWeights(): Map<ProfilePriority, Int> = mapOf(
            ProfilePriority.MESSAGES to 100,
            ProfilePriority.VOICE to 100,
            ProfilePriority.NAVIGATION to 50,
            ProfilePriority.CARE to 40,
            ProfilePriority.CALLS to 70,
            ProfilePriority.CAMERA to 40,
            ProfilePriority.TRANSLATION to 30,
        )
    }
}

enum class ProfileDeterminationLevel {
    /** Первый запуск: пользователь явно отвечает, какая помощь ему нужна. */
    EXPLICIT_ONBOARDING,
    /** Использование: приложение может заметить повторяющийся сценарий и только предложить изменить приоритет. */
    USAGE_SUGGESTION,
    /** Пользователь сам голосом или в настройках меняет приоритеты. */
    USER_OVERRIDE,
}
