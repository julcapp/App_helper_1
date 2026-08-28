package ru.apphelper.messages

data class MessageFacts(
    val dates: List<String> = emptyList(),
    val times: List<String> = emptyList(),
    val phones: List<String> = emptyList(),
    val amounts: List<String> = emptyList(),
    val links: List<String> = emptyList(),
)

object MessageFactsExtractor {
    private val dateRegex = Regex("\\b(?:0?[1-9]|[12]\\d|3[01])[./-](?:0?[1-9]|1[0-2])(?:[./-](?:\\d{2}|\\d{4}))?\\b")
    private val timeRegex = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
    private val phoneRegex = Regex("(?<!\\d)(?:\\+7|8)[\\s()\\-]*\\d{3}[\\s()\\-]*\\d{3}[\\s\\-]*\\d{2}[\\s\\-]*\\d{2}(?!\\d)")
    private val amountRegex = Regex("(?<!\\d)\\d[\\d \\u00A0]*(?:[.,]\\d{1,2})?\\s*(?:₽|руб\\.?|рублей|рубля|р\\.)(?!\\p{L})", RegexOption.IGNORE_CASE)
    private val linkRegex = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)

    fun extract(text: String): MessageFacts = MessageFacts(
        dates = dateRegex.findAll(text).map { it.value }.distinct().toList(),
        times = timeRegex.findAll(text).map { it.value }.distinct().toList(),
        phones = phoneRegex.findAll(text).map { it.value.trim() }.distinct().toList(),
        amounts = amountRegex.findAll(text).map { it.value.trim() }.distinct().toList(),
        links = linkRegex.findAll(text).map { it.value.trimEnd('.', ',', ';', ')') }.distinct().toList(),
    )

    fun answer(command: MessageFactCommand, text: String): String {
        val facts = extract(text)
        return when (command) {
            MessageFactCommand.DATE -> listAnswer("Дата", "дат", facts.dates)
            MessageFactCommand.TIME -> listAnswer("Время", "времени", facts.times)
            MessageFactCommand.PHONE -> listAnswer("Телефон", "номера телефона", facts.phones)
            MessageFactCommand.AMOUNT -> listAnswer("Сумма", "суммы в рублях", facts.amounts)
            MessageFactCommand.LINK -> listAnswer("Ссылка", "ссылки", facts.links)
        }
    }

    private fun listAnswer(label: String, missingLabel: String, values: List<String>): String =
        if (values.isEmpty()) {
            "В тексте сообщения я не нашёл $missingLabel."
        } else {
            "$label: ${values.joinToString(", ")}."
        }
}

enum class MessageFactCommand {
    DATE,
    TIME,
    PHONE,
    AMOUNT,
    LINK,
}
