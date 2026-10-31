package ru.apphelper.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.security.MessageDigest
import ru.apphelper.journal.EventJournal
import ru.apphelper.channels.AndroidReplyCapability
import ru.apphelper.channels.MessageChannelRegistry
import ru.apphelper.channels.MessageChannel

class AppNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        runCatching { activeNotifications }.getOrNull()
            ?.sortedBy { it.postTime }?.forEach { onNotificationPosted(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val status = sbn ?: return
        if (status.packageName == packageName ||
            !NotificationReadingPreferences(this).isEnabled(status.packageName)) return
        val notification = status.notification
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        // Child notifications carry the actual items; a group summary would duplicate them.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim().take(200)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty().trim()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n").orEmpty()
        val body = bigText.ifBlank { lines.ifBlank { text } }.take(12000)
        val appName = runCatching {
            val info = packageManager.getApplicationInfo(status.packageName, 0)
            packageManager.getApplicationLabel(info).toString().take(200)
        }.getOrDefault(status.packageName)
        val channel = MessageChannelRegistry.detect(status.packageName)
        val channelName = if (channel == MessageChannel.OTHER) appName else MessageChannelRegistry.label(channel)
        NotificationReadingPreferences(this).recordSource(status.packageName, channelName)
        val quickReply = AndroidReplyCapability.inspect(status.key, status.packageName, notification)
        val messages = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)?.messages.orEmpty()
        if (title.isBlank() && body.isBlank() && messages.isEmpty()) return
        val events = if (messages.isEmpty()) listOf(NotificationEvent(
            id = status.key, packageName = status.packageName, appName = channelName,
            sender = title.ifBlank { appName }, text = body, receivedAt = status.postTime,
            quickReplyAvailable = quickReply.hasTextReply,
        )) else messages.takeLast(50).map { message ->
            @Suppress("DEPRECATION")
            val sender = message.sender?.toString().orEmpty().trim().take(200).ifBlank { title.ifBlank { appName } }
            val available = message.text?.toString().orEmpty().take(12000)
            val identity = MessageDigest.getInstance("SHA-256")
                .digest("$sender\u0000$available".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            NotificationEvent(
                id = "${status.key}:${message.timestamp}:$identity",
                packageName = status.packageName, appName = channelName, sender = sender,
                text = available, receivedAt = message.timestamp,
                // A message-specific id must not be used as a live notification reply key.
                quickReplyAvailable = false,
            )
        }
        events.forEach { event ->
            NotificationEventStore.add(event)
            EventJournal(this).appendNotification(event)
        }
    }
}
