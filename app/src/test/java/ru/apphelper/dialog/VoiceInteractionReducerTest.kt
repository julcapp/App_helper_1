package ru.apphelper.dialog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInteractionReducerTest {
    @Test fun promptProgressesToListeningAndProcessing() {
        val speaking = VoiceInteractionReducer.reduce(VoicePhase.Idle, VoiceEvent.Prompt("Прочитать?"))
        val listening = VoiceInteractionReducer.reduce(speaking, VoiceEvent.SpeechFinished)
        assertEquals(VoicePhase.AwaitingSpeech("Прочитать?"), listening)
        assertEquals(
            VoicePhase.Processing("Да"),
            VoiceInteractionReducer.reduce(listening, VoiceEvent.Recognized(" Да ")),
        )
    }

    @Test fun recognitionErrorRetainsPromptForRetry() {
        val listening = VoicePhase.AwaitingSpeech("Прочитать?")
        val failure = VoiceInteractionReducer.reduce(listening, VoiceEvent.RecognitionFailed("Нет речи"))
        assertTrue(failure is VoicePhase.RecoverableError)
        assertEquals("Прочитать?", (failure as VoicePhase.RecoverableError).prompt)
        assertEquals(VoicePhase.Speaking("Прочитать?"), VoiceInteractionReducer.reduce(failure, VoiceEvent.Retry))
    }

    @Test fun cancellationReturnsToIdle() {
        assertEquals(VoicePhase.Idle, VoiceInteractionReducer.reduce(VoicePhase.AwaitingSpeech("Да?"), VoiceEvent.Cancel))
    }

    @Test fun strayRecognitionCannotPerformAction() {
        assertEquals(VoicePhase.Idle, VoiceInteractionReducer.reduce(VoicePhase.Idle, VoiceEvent.Recognized("Отправь")))
    }

    @Test fun consecutiveFailuresRemainRecoverable() {
        val first = VoiceInteractionReducer.reduce(
            VoicePhase.AwaitingSpeech("Прочитать?"),
            VoiceEvent.RecognitionFailed("Тишина"),
        ) as VoicePhase.RecoverableError
        val retry = VoiceInteractionReducer.reduce(first, VoiceEvent.Retry)
        val listening = VoiceInteractionReducer.reduce(retry, VoiceEvent.SpeechFinished)
        val second = VoiceInteractionReducer.reduce(
            listening, VoiceEvent.RecognitionFailed("Тишина"),
        ) as VoicePhase.RecoverableError
        assertEquals("Прочитать?", second.prompt)
        assertEquals(1, second.retries)
    }

    @Test fun lateSpeechCallbackAfterCancelDoesNotRestartAction() {
        val cancelled = VoiceInteractionReducer.reduce(
            VoicePhase.AwaitingSpeech("Прочитать?"), VoiceEvent.Cancel,
        )
        assertEquals(VoicePhase.Idle, VoiceInteractionReducer.reduce(
            cancelled, VoiceEvent.Recognized("да"),
        ))
        assertEquals(VoicePhase.Idle, VoiceInteractionReducer.reduce(
            cancelled, VoiceEvent.RecognitionFailed("Поздняя ошибка"),
        ))
    }

    @Test fun secondResultAfterProcessingIsIgnored() {
        val processing = VoiceInteractionReducer.reduce(
            VoicePhase.AwaitingSpeech("Прочитать?"), VoiceEvent.Recognized("да"),
        )
        assertEquals(processing, VoiceInteractionReducer.reduce(
            processing, VoiceEvent.Recognized("отправить"),
        ))
    }
}
