package ru.apphelper.notifications

enum class MessageTextOrigin { NOTIFICATION_PREVIEW, SHARED_TEXT }

data class NotificationEvent(
    val id: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val text: String,
    val receivedAt: Long,
    val quickReplyAvailable: Boolean = false,
    val textOrigin: MessageTextOrigin = MessageTextOrigin.NOTIFICATION_PREVIEW,
)
