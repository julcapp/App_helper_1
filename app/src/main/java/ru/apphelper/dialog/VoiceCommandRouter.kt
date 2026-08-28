package ru.apphelper.dialog

enum class VoiceCommand {
    READ,
    LATER,
    REPEAT,
    EXPLAIN_MESSAGE,
    SIMPLIFY_MESSAGE,
    MESSAGE_MAIN_POINT,
    MESSAGE_REQUEST,
    MESSAGE_DATE,
    MESSAGE_TIME,
    MESSAGE_PHONE,
    MESSAGE_AMOUNT,
    MESSAGE_LINK,
    CLOSE_MESSAGE,
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
            listOf("объясни сообщение", "объясни", "что это значит", "что значит сообщение").any { it in text } -> VoiceCommand.EXPLAIN_MESSAGE
            listOf("скажи проще", "проще", "объясни проще", "простыми словами").any { it in text } -> VoiceCommand.SIMPLIFY_MESSAGE
            listOf("что здесь главное", "что главное", "главное в сообщении", "скажи главное").any { it in text } -> VoiceCommand.MESSAGE_MAIN_POINT
            listOf("что от меня хотят", "что от меня хочет", "что он хочет", "что она хочет", "что нужно сделать", "что просят").any { it in text } -> VoiceCommand.MESSAGE_REQUEST
            listOf("есть ли дата", "какая дата", "назови дату", "дата в сообщении").any { it in text } -> VoiceCommand.MESSAGE_DATE
            listOf("есть ли время", "какое время", "назови время", "время в сообщении").any { it in text } -> VoiceCommand.MESSAGE_TIME
            listOf("есть ли телефон", "какой телефон", "номер телефона", "телефон в сообщении").any { it in text } -> VoiceCommand.MESSAGE_PHONE
            listOf("есть ли сумма", "какая сумма", "сумма в сообщении", "сколько денег").any { it in text } -> VoiceCommand.MESSAGE_AMOUNT
            listOf("есть ли ссылка", "какая ссылка", "ссылка в сообщении").any { it in text } -> VoiceCommand.MESSAGE_LINK
            listOf("закрой сообщение", "закончить сообщение", "больше не надо", "хватит читать").any { it in text } -> VoiceCommand.CLOSE_MESSAGE
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
