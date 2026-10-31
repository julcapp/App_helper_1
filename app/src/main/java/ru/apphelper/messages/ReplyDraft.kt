package ru.apphelper.messages

/**
 * A reply is always a draft. Notification titles are NOT verified recipient identities.
 * No transport API is present here: confirmation never implies delivery.
 */
data class ReplyDraft(
    val sourceNotificationId: String,
    val sourceApp: String,
    val displayedSender: String,
    val text: String = "",
    val reviewed: Boolean = false,
) {
    val canReview: Boolean get() = text.isNotBlank() && text.length <= MAX_TEXT_LENGTH
    fun edit(next: String): ReplyDraft = copy(text = next.take(MAX_TEXT_LENGTH), reviewed = false)
    fun markReviewed(): ReplyDraft = if (canReview) copy(reviewed = true) else copy(reviewed = false)
    companion object { const val MAX_TEXT_LENGTH = 4000 }
}
