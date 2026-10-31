package ru.apphelper.screen

import org.junit.Assert.*
import org.junit.Test

class ScreenTextCollectorTest {
    @Test fun passwordsAndInvisibleTextAreExcluded() {
        val result = ScreenTextCollector.collect(sequenceOf(
            ScreenTextPart("Пароль 123", password = true),
            ScreenTextPart("Скрыто", visible = false), ScreenTextPart("Открытая статья")))
        assertEquals("Открытая статья", result.text)
    }
    @Test fun adjacentDuplicateLabelsAreReadOnce() {
        assertEquals("Текст\nДругой\nТекст", ScreenTextCollector.collect(sequenceOf(
            ScreenTextPart("Текст"), ScreenTextPart("Текст"),
            ScreenTextPart("Другой"), ScreenTextPart("Текст"))).text)
    }
    @Test fun textIsBoundedAndTruncationIsReported() {
        val result = ScreenTextCollector.collect(sequenceOf(ScreenTextPart("a".repeat(20000))))
        assertEquals(12000, result.text.length)
        assertTrue(result.truncated)
    }
    @Test fun emptyContentIsNotFabricated() {
        assertEquals("", ScreenTextCollector.collect(emptySequence()).text)
    }
}
