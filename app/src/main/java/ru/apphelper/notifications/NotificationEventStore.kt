package ru.apphelper.notifications

object NotificationEventStore {
    private const val MAX_EVENTS = 50
    private val events = ArrayDeque<NotificationEvent>()
    private val completed = LinkedHashMap<String, NotificationEvent>()
    private val listeners = mutableSetOf<(NotificationEvent) -> Unit>()

    @Synchronized
    fun add(event: NotificationEvent) {
        val read = completed[event.id]
        if (read != null && read.copy(receivedAt = event.receivedAt) == event) return
        val previous = events.firstOrNull { it.id == event.id }
        if (previous != null && previous.copy(receivedAt = event.receivedAt) == event) return
        events.removeAll { it.id == event.id }
        events.addFirst(event)
        while (events.size > MAX_EVENTS) events.removeLast()
        listeners.toList().forEach { it(event) }
    }

    @Synchronized
    fun snapshot(): List<NotificationEvent> = events.toList()

    /** Local completion only: never changes the originating application's read status. */
    @Synchronized
    fun markRead(event: NotificationEvent) {
        completed[event.id] = event
        while (completed.size > MAX_EVENTS * 2) completed.remove(completed.keys.first())
        // Do not remove a newer preview received while the old snapshot was speaking.
        events.removeAll { it.id == event.id && it.copy(receivedAt = event.receivedAt) == event }
    }

    @Synchronized
    fun removePackage(packageName: String) {
        events.removeAll { it.packageName == packageName }
        completed.entries.removeAll { it.value.packageName == packageName }
    }

    @Synchronized
    fun addListener(listener: (NotificationEvent) -> Unit) {
        listeners += listener
    }

    @Synchronized
    fun removeListener(listener: (NotificationEvent) -> Unit) {
        listeners -= listener
    }
}
