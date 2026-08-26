package ru.apphelper.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidVoiceAssistant(
    private val context: Context,
    private val onRecognized: (String) -> Unit,
    private val onError: (String) -> Unit,
) : VoiceAssistant, TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }

    init {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onError(error: Int) {
                onError("Ошибка распознавания речи: $error")
            }

            override fun onResults(results: Bundle?) {
                val values = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                values.firstOrNull()?.let(onRecognized)
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("ru", "RU")
        } else {
            onError("Не удалось запустить синтез речи")
        }
    }

    override fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "app-helper")
    }

    override fun startListening() {
        val speechRecognizer = recognizer ?: run {
            onError("Распознавание речи недоступно на этом устройстве")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите")
        }
        speechRecognizer.startListening(intent)
    }

    override fun stopListening() {
        recognizer?.stopListening()
    }

    override fun release() {
        recognizer?.destroy()
        tts.stop()
        tts.shutdown()
    }
}
