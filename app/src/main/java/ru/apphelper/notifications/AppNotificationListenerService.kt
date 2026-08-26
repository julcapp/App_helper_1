package ru.apphelper.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ru.apphelper.journal.EventJournal

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

        val event = NotificationEvent(
            id = status.key,
            packageName = status.packageName,
            appName = appName,
            sender = title.ifBlank { appName },
            text = body,
            receivedAt = status.postTime,
        )

        EventJournal(this).appendNotification(event)
        NotificationEventStore.add(event)
    }
}
