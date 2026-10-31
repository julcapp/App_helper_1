package ru.apphelper.messages

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyDraftTest {
    private fun draft() = ReplyDraft("event", "MAX", "Сергей")

    @Test fun blankDraftCannotBeReviewed() {
        assertFalse(draft().markReviewed().reviewed)
    }

    @Test fun editingInvalidatesPriorReview() {
        val reviewed = draft().edit("Здравствуйте").markReviewed()
        assertTrue(reviewed.reviewed)
        val edited = reviewed.edit("Добрый день")
        assertFalse(edited.reviewed)
        assertEquals("Добрый день", edited.text)
    }

    @Test fun lengthIsBounded() {
        val result = draft().edit("а".repeat(ReplyDraft.MAX_TEXT_LENGTH + 50))
        assertEquals(ReplyDraft.MAX_TEXT_LENGTH, result.text.length)
    }

    @Test fun reviewNeverSends() {
        val result = draft().edit("Да").markReviewed()
        assertTrue(result.reviewed)
        assertEquals("event", result.sourceNotificationId)
    }
}
