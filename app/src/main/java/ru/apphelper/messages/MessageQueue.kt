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
    val deferredIds: Set<String> = emptySet(),
    val readIds: Set<String> = emptySet(),
) {
    val unread: List<NotificationEvent> get() = pending.filterNot { it.id in readIds }
    val selected: NotificationEvent?
        get() = pending.firstOrNull { it.id == selectedId && it.id !in deferredIds }
            ?: pending.firstOrNull { it.id !in deferredIds && it.id !in readIds }

    fun receive(event: NotificationEvent): MessageQueue {
        if (pending.any { it.id == event.id }) {
            val old = pending.first { it.id == event.id }
            return copy(pending = pending.map { if (it.id == event.id) event else it },
                readIds = if (old.copy(receivedAt = event.receivedAt) == event) readIds else readIds - event.id)
        }
        val all = pending + event
        val protectedId = selected?.id
        val updated = if (all.size > MAX_PENDING) {
            val evict = all.indexOfFirst { it.id != protectedId }
            all.filterIndexed { index, _ -> index != evict }
        } else all
        return copy(pending = updated, selectedId = selected?.id
            ?: updated.firstOrNull { it.id !in deferredIds && it.id !in readIds }?.id,
            deferredIds = deferredIds.intersect(updated.map { it.id }.toSet()),
            readIds = readIds.intersect(updated.map { it.id }.toSet()))
    }

    fun dismiss(id: String?): MessageQueue {
        if (id == null) return this
        val updated = pending.filterNot { it.id == id }
        return copy(pending = updated, selectedId = selectedId?.takeIf { it != id }
            ?: updated.firstOrNull { it.id !in deferredIds && it.id !in readIds }?.id, deferredIds = deferredIds - id,
            readIds = readIds - id)
    }

    fun defer(id: String?): MessageQueue {
        if (id == null || pending.none { it.id == id }) return this
        val deferred = deferredIds + id
        return copy(deferredIds = deferred, readIds = readIds - id,
            selectedId = pending.firstOrNull { it.id !in deferred && it.id !in readIds }?.id)
    }

    fun restoreDeferred(): MessageQueue = copy(deferredIds = emptySet(), selectedId = pending.firstOrNull()?.id)

    fun markRead(snapshot: NotificationEvent): MessageQueue =
        if (pending.firstOrNull { it.id == snapshot.id } == snapshot) copy(readIds = readIds + snapshot.id)
        else this

    /** Closing an older active snapshot must never consume a newer same-key preview. */
    fun dismissSnapshot(snapshot: NotificationEvent): MessageQueue =
        if (pending.firstOrNull { it.id == snapshot.id } == snapshot) dismiss(snapshot.id) else this

    fun withoutPackage(packageName: String): MessageQueue {
        val retained = pending.filterNot { it.packageName == packageName }
        return copy(pending = retained, selectedId = selectedId?.takeIf { id -> retained.any { it.id == id } },
            deferredIds = deferredIds.intersect(retained.map { it.id }.toSet()),
            readIds = readIds.intersect(retained.map { it.id }.toSet()))
    }

    fun select(id: String): MessageQueue =
        if (pending.any { it.id == id }) copy(selectedId = id, deferredIds = deferredIds - id) else this

    companion object {
        const val MAX_PENDING = 50
        fun fromNewestFirst(events: List<NotificationEvent>): MessageQueue {
            val distinct = events.distinctBy { it.id }.take(MAX_PENDING).asReversed()
            return MessageQueue(pending = distinct, selectedId = distinct.firstOrNull()?.id)
        }
    }
}
