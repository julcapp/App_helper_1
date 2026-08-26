package ru.apphelper.voice

/**
 * Порт голосового слоя. Реализации Android TextToSpeech и SpeechRecognizer
 * подключаются отдельно, чтобы UI и Agent Core не зависели от конкретного SDK.
 */
interface VoiceAssistant {
    fun speak(text: String)
    fun startListening()
    fun stopListening()
    fun release()
}

data class VoiceState(
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val errorMessage: String? = null,
)
