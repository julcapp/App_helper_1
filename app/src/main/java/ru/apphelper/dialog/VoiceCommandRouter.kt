package ru.apphelper.dialog

enum class VoiceCommand {
    READ,
    LATER,
    REPEAT,
    UNKNOWN,
}

object VoiceCommandRouter {
    fun parse(raw: String): VoiceCommand {
        val text = raw.trim().lowercase()

        return when {
            listOf("да", "прочитай", "читать", "читай", "озвучь", "расскажи").any { it in text } -> VoiceCommand.READ
            listOf("нет", "позже", "не сейчас", "отложи", "пропусти").any { it in text } -> VoiceCommand.LATER
            listOf("повтори", "ещё раз", "еще раз", "повторить").any { it in text } -> VoiceCommand.REPEAT
            else -> VoiceCommand.UNKNOWN
        }
    }
}
