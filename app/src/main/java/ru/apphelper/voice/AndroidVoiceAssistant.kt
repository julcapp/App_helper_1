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
    private var speechGeneration = 0L
    private var listening = false
    private var localVoiceReady = false
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
    private var recognizer: SpeechRecognizer? = null
    private var recognitionGeneration = 0L
    private fun createRecognizer(): SpeechRecognizer? =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) ->
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            SpeechRecognizer.isRecognitionAvailable(context) -> SpeechRecognizer.createSpeechRecognizer(context)
            else -> null
        }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { id ->
                    mainHandler.post {
                        if (id.startsWith("$speechGeneration:")) {
                            stopSpeaking()
                            this@AndroidVoiceAssistant.onError("Озвучивание прервано. Сообщение сохранено для повтора.")
                        }
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (!interrupted) utteranceId?.let { completionCallbacks.remove(it) }?.invoke()
                    else utteranceId?.let { completionCallbacks.remove(it) }
                }
            }
        })

    }

    private fun recognitionListener(generation: Long) = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onError(error: Int) {
                if (!listening || generation != recognitionGeneration) return
                listening = false
                this@AndroidVoiceAssistant.onError("Ошибка распознавания речи: $error")
            }

            override fun onResults(results: Bundle?) {
                if (!listening || generation != recognitionGeneration) return
                listening = false
                val values = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                val result = values.firstOrNull { it.isNotBlank() }
                if (result == null) this@AndroidVoiceAssistant.onError("Речь не распознана. Попробуйте ещё раз.")
                else onRecognized(result)
            }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localVoice = tts.voices?.firstOrNull {
                it.locale.language == "ru" && !it.isNetworkConnectionRequired
            }
            localVoiceReady = localVoice != null && tts.setVoice(localVoice) == TextToSpeech.SUCCESS
            if (!localVoiceReady) this@AndroidVoiceAssistant.onError("Установите русский офлайн-голос в настройках синтеза речи Android. Частный текст не передаётся сетевому голосу.")
        } else {
            this@AndroidVoiceAssistant.onError("Не удалось запустить синтез речи")
        }
    }

    override fun speak(text: String, onDone: (() -> Unit)?) {
        stopListening()
        speechGeneration++
        val generation = speechGeneration
        completionCallbacks.clear()
        if (!localVoiceReady) {
            this@AndroidVoiceAssistant.onError("Офлайн-голос ещё не готов или не установлен. Текст сохранён; проверьте настройки синтеза речи Android.")
            return
        }
        if (!requestFocus()) {
            this@AndroidVoiceAssistant.onError("Звук занят другим приложением. Попробуйте позже.")
            return
        }
        // Android TTS rejects an input longer than its limit; queue bounded chunks.
        val chunks = text.chunked(TextToSpeech.getMaxSpeechInputLength())
        chunks.forEachIndexed { index, chunk ->
            val utteranceId = "$generation:${UUID.randomUUID()}"
            if (index == chunks.lastIndex && onDone != null) {
                completionCallbacks[utteranceId] = { if (generation == speechGeneration) onDone() }
            }
            val result = tts.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                stopSpeaking()
                this@AndroidVoiceAssistant.onError("Не удалось озвучить текст. Сообщение сохранено для повтора.")
                return
            }
        }
    }

    override fun setSpeechRate(rate: Float) {
        tts.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
    }

    override fun startListening() {
        if (interrupted) return
        stopListening()
        val speechRecognizer = runCatching { createRecognizer() }.getOrNull() ?: run {
            this@AndroidVoiceAssistant.onError("Распознавание речи недоступно на этом устройстве")
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
        recognizer = speechRecognizer
        val generation = recognitionGeneration
        speechRecognizer.setRecognitionListener(recognitionListener(generation))
        listening = true
        runCatching { speechRecognizer.startListening(intent) }.onFailure {
            stopListening()
            this@AndroidVoiceAssistant.onError("Не удалось запустить распознавание. Проверьте микрофон и установленный язык.")
        }
    }

    override fun stopListening() {
        listening = false
        recognitionGeneration++
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun stopSpeaking() {
        speechGeneration++
        completionCallbacks.clear()
        tts.stop()
        abandonFocus()
    }

    override fun release() {
        speechGeneration++
        stopListening()
        completionCallbacks.clear()
        tts.stop()
        tts.shutdown()
        abandonFocus()
    }
}
