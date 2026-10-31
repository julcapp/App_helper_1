package ru.apphelper.messages

/**
 * A notification title cannot establish a verified addressable recipient.
 * This state can only support UI review. Sending requires a separate provider adapter.
 */
data class RecipientReview(
    val notificationId: String,
    val appName: String,
    val displayedSender: String,
    val confirmedForDisplay: Boolean = false,
) {
    val isIdentifiable: Boolean get() =
        notificationId.isNotBlank() && appName.isNotBlank() && displayedSender.isNotBlank()

    fun confirmDisplay(): RecipientReview =
        copy(confirmedForDisplay = isIdentifiable)

    fun invalidate(): RecipientReview = copy(confirmedForDisplay = false)
}
