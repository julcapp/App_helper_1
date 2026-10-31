package ru.apphelper.messages

import ru.apphelper.notifications.NotificationEvent

/**
 * Immutable reducer for pending messages. The selected item stays stable while
 * new events arrive. A bounded snapshot protects the UI from unbounded memory use.
 * Persistence and cross-process delivery must be implemented separately.
 */
data class MessageQueue(
    val pending: List<NotificationEvent> = emptyList(),
    val selectedId: String? = null,
) {
    val selected: NotificationEvent?
        get() = pending.firstOrNull { it.id == selectedId } ?: pending.firstOrNull()

    fun receive(event: NotificationEvent): MessageQueue {
        if (pending.any { it.id == event.id }) return this
        val updated = (pending + event).takeLast(MAX_PENDING)
        return copy(pending = updated, selectedId = selectedId?.takeIf { id -> updated.any { it.id == id } }
            ?: updated.firstOrNull()?.id)
    }

    fun dismiss(id: String?): MessageQueue {
        if (id == null) return this
        val updated = pending.filterNot { it.id == id }
        return copy(pending = updated, selectedId = updated.firstOrNull()?.id)
    }

    fun select(id: String): MessageQueue =
        if (pending.any { it.id == id }) copy(selectedId = id) else this

    companion object {
        const val MAX_PENDING = 50
        fun fromNewestFirst(events: List<NotificationEvent>): MessageQueue {
            val distinct = events.distinctBy { it.id }.take(MAX_PENDING).asReversed()
            return MessageQueue(pending = distinct, selectedId = distinct.firstOrNull()?.id)
        }
    }
}
