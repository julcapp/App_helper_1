package ru.apphelper.dialog

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandRouterTest {
    @Test fun explicitReadingControls() {
        mapOf("читай всё по очереди" to VoiceCommand.READ_ALL,
            "следующее" to VoiceCommand.NEXT,
            "пауза" to VoiceCommand.PAUSE,
            "кто написал" to VoiceCommand.SENDER,
            "читай медленнее" to VoiceCommand.SLOWER,
            "прочитай новые сообщения" to VoiceCommand.READ_NEW).forEach { (text, expected) ->
            assertEquals(text, expected, VoiceCommandRouter.parse(text))
        }
    }
    @Test fun negationNeverStartsReading() {
        listOf("не читай", "не надо читать", "не сейчас").forEach {
            assertEquals(it, VoiceCommand.LATER, VoiceCommandRouter.parse(it))
        }
    }
    @Test fun wordFragmentsAreNotCommands() {
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommandRouter.parse("погода"))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommandRouter.parse("рассказ о читателях"))
    }
}
