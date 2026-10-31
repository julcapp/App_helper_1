package ru.apphelper.dialog

enum class VoiceCommand {
    READ_NEW, READ_ALL, NEXT, PAUSE, SENDER, SLOWER,
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
        val text = raw.trim().lowercase(java.util.Locale.ROOT)
            .replace(Regex("[!?.,]"), "").replace(Regex("\\s+"), " ")

        return when {
            text in listOf("не читай", "не надо читать", "не сейчас", "позже", "нет", "отложи", "пропусти") -> VoiceCommand.LATER
            text in listOf("прочитай новые сообщения", "читай новые сообщения", "прочитай входящие", "новые сообщения") -> VoiceCommand.READ_NEW
            text in listOf("читай всё по очереди", "читай все по очереди", "прочитай всё", "прочитай все", "читай всё", "читай все") -> VoiceCommand.READ_ALL
            text in listOf("следующее", "дальше", "следующее сообщение") -> VoiceCommand.NEXT
            text in listOf("пауза", "стоп", "остановись", "останови чтение") -> VoiceCommand.PAUSE
            text in listOf("кто написал", "от кого сообщение", "кто отправитель") -> VoiceCommand.SENDER
            text in listOf("читай медленнее", "медленнее", "говори медленнее") -> VoiceCommand.SLOWER
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
            text in listOf("да", "прочитай", "читать", "читай", "озвучь", "расскажи", "прочитай полностью", "продолжи", "продолжить") -> VoiceCommand.READ
            listOf("повтори", "ещё раз", "еще раз", "повторить").any { it in text } -> VoiceCommand.REPEAT
            else -> VoiceCommand.UNKNOWN
        }
    }
}
