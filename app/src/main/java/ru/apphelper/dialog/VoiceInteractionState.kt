package ru.apphelper.dialog

/**
 * Deterministic interaction model to replace fragile combinations of boolean dialogue flags.
 * This is a staged core model; wiring into MainActivity and its platform speech adapter
 * is deliberately separate to preserve existing behaviour until device testing.
 */
sealed interface VoicePhase {
    data object Idle : VoicePhase
    data class Speaking(val prompt: String) : VoicePhase
    data class AwaitingSpeech(val prompt: String, val retries: Int = 0) : VoicePhase
    data class Processing(val recognizedText: String) : VoicePhase
    data class RecoverableError(val prompt: String, val reason: String, val retries: Int) : VoicePhase
}

sealed interface VoiceEvent {
    data class Prompt(val text: String) : VoiceEvent
    data object SpeechFinished : VoiceEvent
    data class Recognized(val text: String) : VoiceEvent
    data class RecognitionFailed(val reason: String) : VoiceEvent
    data object Retry : VoiceEvent
    data object Cancel : VoiceEvent
}

object VoiceInteractionReducer {
    fun reduce(state: VoicePhase, event: VoiceEvent): VoicePhase = when (event) {
        is VoiceEvent.Prompt -> VoicePhase.Speaking(event.text)
        VoiceEvent.SpeechFinished -> when (state) {
            is VoicePhase.Speaking -> VoicePhase.AwaitingSpeech(state.prompt)
            else -> state
        }
        is VoiceEvent.Recognized -> when (state) {
            is VoicePhase.AwaitingSpeech -> {
                if (event.text.isBlank()) VoicePhase.RecoverableError(
                    state.prompt, "Речь не распознана", state.retries + 1,
                ) else VoicePhase.Processing(event.text.trim())
            }
            else -> state
        }
        is VoiceEvent.RecognitionFailed -> when (state) {
            is VoicePhase.AwaitingSpeech -> VoicePhase.RecoverableError(
                state.prompt, event.reason, state.retries + 1,
            )
            else -> state
        }
        VoiceEvent.Retry -> when (state) {
            is VoicePhase.RecoverableError -> VoicePhase.Speaking(state.prompt)
            else -> state
        }
        VoiceEvent.Cancel -> VoicePhase.Idle
    }
}
