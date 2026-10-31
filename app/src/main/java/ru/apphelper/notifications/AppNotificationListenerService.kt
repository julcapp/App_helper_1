package ru.apphelper.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ru.apphelper.journal.EventJournal
import ru.apphelper.channels.AndroidReplyCapability
import ru.apphelper.channels.MessageChannelRegistry
import ru.apphelper.channels.MessageChannel

class AppNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val status = sbn ?: return
        if (status.packageName == packageName) return

        val extras = status.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty().trim()
        val body = bigText.ifBlank { text }
        if (title.isBlank() && body.isBlank()) return

        val appName = runCatching {
            val info = packageManager.getApplicationInfo(status.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(status.packageName)

        val channel = MessageChannelRegistry.detect(status.packageName)
        val channelName = if (channel == MessageChannel.OTHER) appName else MessageChannelRegistry.label(channel)

        val quickReply = AndroidReplyCapability.inspect(status.key, status.packageName, status.notification)
        val event = NotificationEvent(
            id = status.key,
            packageName = status.packageName,
            appName = channelName,
            sender = title.ifBlank { appName },
            text = body,
            receivedAt = status.postTime,
            quickReplyAvailable = quickReply.hasTextReply,
        )

        EventJournal(this).appendNotification(event)
        NotificationEventStore.add(event)
    }
}
