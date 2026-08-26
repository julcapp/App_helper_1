package ru.apphelper.profile

data class ProfileTuningQuestion(
    val prompt: String,
    val priority: ProfilePriority,
    val yesWeight: Int,
    val noWeight: Int,
)

data class ProfileTuningSession(
    val stepIndex: Int = 0,
    val priorities: ProfilePriorities = ProfilePriorities(),
) {
    fun currentQuestion(): ProfileTuningQuestion? = questions.getOrNull(stepIndex)

    fun currentPrompt(): String? = currentQuestion()?.prompt

    fun answer(yes: Boolean): ProfileTuningSession {
        val question = currentQuestion() ?: return this
        val nextPriorities = priorities.withWeight(
            question.priority,
            if (yes) question.yesWeight else question.noWeight,
        )
        return copy(stepIndex = stepIndex + 1, priorities = nextPriorities)
    }

    val isComplete: Boolean
        get() = stepIndex >= questions.size

    companion object {
        val questions = listOf(
            ProfileTuningQuestion(
                prompt = "Хотите, чтобы я в первую очередь помогал читать и озвучивать сообщения?",
                priority = ProfilePriority.MESSAGES,
                yesWeight = 100,
                noWeight = 60,
            ),
            ProfileTuningQuestion(
                prompt = "Хотите в основном управлять помощником голосом?",
                priority = ProfilePriority.VOICE,
                yesWeight = 100,
                noWeight = 60,
            ),
            ProfileTuningQuestion(
                prompt = "Нужна ли вам повышенная помощь с маршрутами и поездками?",
                priority = ProfilePriority.NAVIGATION,
                yesWeight = 90,
                noWeight = 40,
            ),
            ProfileTuningQuestion(
                prompt = "Хотите, чтобы я иногда ненавязчиво предлагал помощь и отдых?",
                priority = ProfilePriority.CARE,
                yesWeight = 80,
                noWeight = 10,
            ),
            ProfileTuningQuestion(
                prompt = "Нужна ли повышенная помощь со звонками и контактами?",
                priority = ProfilePriority.CALLS,
                yesWeight = 85,
                noWeight = 50,
            ),
        )
    }
}
