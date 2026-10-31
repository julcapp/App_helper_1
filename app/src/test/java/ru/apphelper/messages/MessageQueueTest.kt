package ru.apphelper.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.apphelper.notifications.NotificationEvent

class MessageQueueTest {
    private fun event(id: String) = NotificationEvent(
        id = id, packageName = "test.app", appName = "Тест",
        sender = "Контакт", text = "Сообщение $id", receivedAt = 1L,
    )

    @Test fun newEventDoesNotInterruptSelectedMessage() {
        val queue = MessageQueue().receive(event("one")).receive(event("two"))
        assertEquals("one", queue.selected?.id)
        assertEquals(2, queue.pending.size)
    }

    @Test fun dismissSelectsNextPendingMessage() {
        val queue = MessageQueue().receive(event("one")).receive(event("two")).dismiss("one")
        assertEquals("two", queue.selected?.id)
        assertEquals(1, queue.pending.size)
    }

    @Test fun duplicatesAreIgnored() {
        val queue = MessageQueue().receive(event("one")).receive(event("one"))
        assertEquals(1, queue.pending.size)
    }

    @Test fun emptyQueueReturnsNoSelection() {
        val queue = MessageQueue().receive(event("one")).dismiss("one")
        assertNull(queue.selected)
    }

    @Test fun restoringNewestFirstSnapshotPreservesOldestPendingOrder() {
        val queue = MessageQueue.fromNewestFirst(listOf(event("new"), event("old")))
        assertEquals("old", queue.selected?.id)
    }
}
