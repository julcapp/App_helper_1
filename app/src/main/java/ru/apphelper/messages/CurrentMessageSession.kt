package ru.apphelper.messages

import ru.apphelper.notifications.NotificationEvent

data class CurrentMessageSession(
    val event: NotificationEvent,
    val activatedAtMillis: Long = System.currentTimeMillis(),
) {
    val sender: String
        get() = event.sender.ifBlank { event.appName }

    val source: String
        get() = event.appName

    val originalText: String
        get() = event.text.trim()

    val hasReadableText: Boolean
        get() = originalText.isNotBlank()
}
