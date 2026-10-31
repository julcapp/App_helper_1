package ru.apphelper.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class AndroidVoiceAssistant(
    private val context: Context,
    private val onRecognized: (String) -> Unit,
    private val onError: (String) -> Unit,
) : VoiceAssistant, TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var interrupted = false
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            interrupted = true
            stopListening()
            stopSpeaking()
        }
        // Do not automatically restart private speech when focus returns.
    }
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
        } else null

    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) interrupted = false
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }
    private val completionCallbacks = mutableMapOf<String, () -> Unit>()
    private val tts = TextToSpeech(context, this)
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onError(utteranceId: String?) {
                utteranceId?.let { id ->
                    mainHandler.post { completionCallbacks.remove(id) }
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (!interrupted) utteranceId?.let { completionCallbacks.remove(it) }?.invoke()
                    else utteranceId?.let { completionCallbacks.remove(it) }
                }
            }
        })

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
                val result = values.firstOrNull { it.isNotBlank() }
                if (result == null) onError("Речь не распознана. Попробуйте ещё раз.")
                else onRecognized(result)
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

    override fun speak(text: String, onDone: (() -> Unit)?) {
        if (!requestFocus()) {
            onError("Звук занят другим приложением. Попробуйте позже.")
            return
        }
        val utteranceId = "app-helper-${UUID.randomUUID()}"
        if (onDone != null) completionCallbacks[utteranceId] = onDone
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun setSpeechRate(rate: Float) {
        tts.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
    }

    override fun startListening() {
        if (interrupted) return
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
        recognizer?.cancel()
    }

    fun stopSpeaking() {
        completionCallbacks.clear()
        tts.stop()
        abandonFocus()
    }

    override fun release() {
        completionCallbacks.clear()
        recognizer?.destroy()
        tts.stop()
        tts.shutdown()
        abandonFocus()
    }
}
