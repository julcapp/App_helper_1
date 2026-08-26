package ru.apphelper.dialog

enum class VoiceCommand {
    READ,
    LATER,
    REPEAT,
    MISSED,
    WHERE_AM_I,
    GO_HOME,
    SAVE_HOME,
    TUNE_TO_ME,
    UNKNOWN,
}

object VoiceCommandRouter {
    fun parse(raw: String): VoiceCommand {
        val text = raw.trim().lowercase()

        return when {
            listOf("настройся под меня", "настрой меня", "помоги настроить помощника", "измени мои приоритеты", "перенастрой помощника").any { it in text } -> VoiceCommand.TUNE_TO_ME
            listOf("что я пропустил", "что пропустил", "что было", "пропущенные события", "пропущенные").any { it in text } -> VoiceCommand.MISSED
            listOf("где я", "где я сейчас", "мое местоположение", "моё местоположение", "где нахожусь").any { it in text } -> VoiceCommand.WHERE_AM_I
            listOf("отведи меня домой", "веди домой", "маршрут домой", "поехали домой", "домой").any { it in text } -> VoiceCommand.GO_HOME
            listOf("запомни дом", "сохрани дом", "это мой дом", "запомни это место как дом").any { it in text } -> VoiceCommand.SAVE_HOME
            listOf("да", "прочитай", "читать", "читай", "озвучь", "расскажи").any { it in text } -> VoiceCommand.READ
            listOf("нет", "позже", "не сейчас", "отложи", "пропусти").any { it in text } -> VoiceCommand.LATER
            listOf("повтори", "ещё раз", "еще раз", "повторить").any { it in text } -> VoiceCommand.REPEAT
            else -> VoiceCommand.UNKNOWN
        }
    }
}
