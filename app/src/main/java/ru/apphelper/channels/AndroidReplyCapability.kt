package ru.apphelper.channels

import android.app.Notification
import android.app.RemoteInput
import android.os.Build

/**
 * Observation only: never invokes PendingIntent or sends a message.
 * Capabilities must be rechecked against the live notification immediately before any future send.
 */
data class ReplyCapability(
    val notificationKey: String,
    val packageName: String,
    val channel: MessageChannel,
    val hasTextReply: Boolean,
    val replyActionCount: Int,
)

object AndroidReplyCapability {
    fun inspect(notificationKey: String, packageName: String, notification: Notification): ReplyCapability {
        val actions = notification.actions.orEmpty()
        val replyCount = actions.count { action ->
            action.actionIntent != null &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !action.actionIntent.isImmutable) &&
                action.remoteInputs.orEmpty().any { input: RemoteInput ->
                    input.allowFreeFormInput && input.resultKey.isNotBlank()
                }
        }
        return ReplyCapability(
            notificationKey = notificationKey,
            packageName = packageName,
            channel = MessageChannelRegistry.detect(packageName),
            hasTextReply = replyCount > 0,
            replyActionCount = replyCount,
        )
    }
}
