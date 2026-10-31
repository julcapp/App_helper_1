package ru.apphelper.channels

/**
 * Notification access does not confer permission to read complete chat histories
 * or send messages. Channel detection is based on Android package identity only.
 */
enum class MessageChannel { MAX, TELEGRAM, WHATSAPP, OTHER }

interface MessageChannelAdapter {
    val channel: MessageChannel
    fun matches(packageName: String): Boolean
    val supportsNotificationReading: Boolean get() = true
    val supportsVerifiedSending: Boolean get() = false
}

private class PackageAdapter(
    override val channel: MessageChannel,
    private val packageNames: Set<String>,
) : MessageChannelAdapter {
    override fun matches(packageName: String): Boolean = packageName in packageNames
}

object MessageChannelRegistry {
    private val adapters: List<MessageChannelAdapter> = listOf(
        // MAX Android package from the published app store listing.
        PackageAdapter(MessageChannel.MAX, setOf("ru.oneme.app")),
        PackageAdapter(MessageChannel.TELEGRAM, setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
        )),
        PackageAdapter(MessageChannel.WHATSAPP, setOf(
            "com.whatsapp", "com.whatsapp.w4b",
        )),
    )

    fun detect(packageName: String): MessageChannel =
        adapters.firstOrNull { it.matches(packageName) }?.channel ?: MessageChannel.OTHER

    fun label(channel: MessageChannel): String = when (channel) {
        MessageChannel.MAX -> "MAX"
        MessageChannel.TELEGRAM -> "Telegram"
        MessageChannel.WHATSAPP -> "WhatsApp"
        MessageChannel.OTHER -> "Другое приложение"
    }
}
