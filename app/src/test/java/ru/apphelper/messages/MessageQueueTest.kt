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
    @Test fun sameKeyUpdateReplacesPendingPreview() {
        val old = event("one")
        val queue = MessageQueue().receive(old).receive(old.copy(text = "Новое содержимое", receivedAt = 2))
        assertEquals(1, queue.pending.size)
        assertEquals("Новое содержимое", queue.selected?.text)
    }

    @Test fun overflowNeverEvictsSelectedMessage() {
        var queue = MessageQueue().receive(event("active"))
        repeat(60) { queue = queue.receive(event("new-$it")) }
        assertEquals("active", queue.selected?.id)
        assertEquals(MessageQueue.MAX_PENDING, queue.pending.size)
    }
    @Test fun laterPreservesMessageUntilExplicitRestore() {
        val queue = MessageQueue().receive(event("one")).receive(event("two")).defer("one")
        assertEquals("two", queue.selected?.id)
        assertEquals(2, queue.pending.size)
        assertEquals("one", queue.restoreDeferred().selected?.id)
    }
    @Test fun deferringOnlyMessageLeavesNoActiveSelection() {
        val queue = MessageQueue().receive(event("one")).defer("one")
        assertNull(queue.selected)
        assertEquals(1, queue.pending.size)
    }
    @Test fun disablingSourceRemovesAllItsMessages() {
        val queue = MessageQueue().receive(event("one")).withoutPackage("test.app")
        assertNull(queue.selected)
        assertEquals(0, queue.pending.size)
    }
    @Test fun completedReadingIsExcludedFromNewMessages() {
        val first = event("one")
        val queue = MessageQueue().receive(first).receive(event("two")).markRead(first)
        assertEquals(listOf("two"), queue.unread.map { it.id })
    }
    @Test fun newerPreviewRemainsUnreadAfterOldSnapshotCompletes() {
        val first = event("one")
        val queue = MessageQueue().receive(first).receive(first.copy(text = "Новое")).markRead(first)
        assertEquals(1, queue.unread.size)
    }
    @Test fun updatedReadPreviewBecomesUnread() {
        val first = event("one")
        val queue = MessageQueue().receive(first).markRead(first).receive(first.copy(text = "Новое"))
        assertEquals(1, queue.unread.size)
    }
    @Test fun closingOldSnapshotDoesNotDismissNewSameKeyText() {
        val old = event("one")
        val queue = MessageQueue().receive(old).receive(old.copy(text = "Обновление")).dismissSnapshot(old)
        assertEquals("Обновление", queue.selected?.text)
    }
    @Test fun nextAfterDeferringDoesNotReturnToAlreadyReadItem() {
        val first = event("one")
        val queue = MessageQueue().receive(first).receive(event("two")).receive(event("three"))
            .markRead(first).select("two").defer("two")
        assertEquals("three", queue.selected?.id)
    }
}
