package ru.apphelper.notifications

data class NotificationEvent(
    val id: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val text: String,
    val receivedAt: Long,
)
