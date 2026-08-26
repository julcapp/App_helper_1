package ru.apphelper.notifications

object NotificationEventStore {
    private const val MAX_EVENTS = 50
    private val events = ArrayDeque<NotificationEvent>()
    private val listeners = mutableSetOf<(NotificationEvent) -> Unit>()

    @Synchronized
    fun add(event: NotificationEvent) {
        if (events.any { it.id == event.id }) return
        events.addFirst(event)
        while (events.size > MAX_EVENTS) events.removeLast()
        listeners.toList().forEach { it(event) }
    }

    @Synchronized
    fun snapshot(): List<NotificationEvent> = events.toList()

    @Synchronized
    fun addListener(listener: (NotificationEvent) -> Unit) {
        listeners += listener
    }

    @Synchronized
    fun removeListener(listener: (NotificationEvent) -> Unit) {
        listeners -= listener
    }
}
