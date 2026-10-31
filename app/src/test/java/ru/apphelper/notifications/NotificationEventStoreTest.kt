package ru.apphelper.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationEventStoreTest {
    @Test fun updatesSameKeyButDoesNotAnnounceDuplicate() {
        val id = "store-test-${System.nanoTime()}"
        val original = NotificationEvent(id, "test.app", "Тест", "Контакт", "Первый", 1L)
        val observed = mutableListOf<NotificationEvent>()
        val listener: (NotificationEvent) -> Unit = { if (it.id == id) observed += it }
        NotificationEventStore.addListener(listener)
        try {
            NotificationEventStore.add(original)
            NotificationEventStore.add(original.copy(receivedAt = 2L))
            NotificationEventStore.add(original.copy(text = "Второй", receivedAt = 3L))
            assertEquals(listOf("Первый", "Второй"), observed.map { it.text })
            assertEquals("Второй", NotificationEventStore.snapshot().first { it.id == id }.text)
        } finally { NotificationEventStore.removeListener(listener) }
    }
    @Test fun completedPreviewDoesNotReappearUntilContentChanges() {
        val id = "read-test-${System.nanoTime()}"
        val event = NotificationEvent(id, "test.app", "Тест", "Контакт", "Текст", 1L)
        NotificationEventStore.add(event)
        NotificationEventStore.markRead(event)
        NotificationEventStore.add(event.copy(receivedAt = 2L))
        assertEquals(0, NotificationEventStore.snapshot().count { it.id == id })
        NotificationEventStore.add(event.copy(text = "Новый текст", receivedAt = 3L))
        assertEquals("Новый текст", NotificationEventStore.snapshot().first { it.id == id }.text)
    }
    @Test fun finishingOldSnapshotPreservesNewPreview() {
        val id = "race-test-${System.nanoTime()}"
        val old = NotificationEvent(id, "test.app", "Тест", "Контакт", "Старое", 1L)
        NotificationEventStore.add(old)
        NotificationEventStore.add(old.copy(text = "Новое", receivedAt = 2L))
        NotificationEventStore.markRead(old)
        assertEquals("Новое", NotificationEventStore.snapshot().first { it.id == id }.text)
    }
}
