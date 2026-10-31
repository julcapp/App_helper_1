package ru.apphelper.screen

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.util.Locale

/** Reads only on the user's accessibility-button action. Never caches or uploads screens. */
class ScreenReaderService : AccessibilityService() {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var speaking = false
    private val handler = Handler(Looper.getMainLooper())
    private val buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
            if (speaking) { onInterrupt(); return }
            readCurrentWindow()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityButtonController.registerAccessibilityButtonCallback(buttonCallback, handler)
        tts = TextToSpeech(this) { status ->
            handler.post {
                val engine = tts ?: return@post
                val localVoice = engine.voices?.firstOrNull {
                    it.locale.language == "ru" && !it.isNetworkConnectionRequired
                }
                ready = status == TextToSpeech.SUCCESS && localVoice != null &&
                    engine.setVoice(localVoice) == TextToSpeech.SUCCESS
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "screen-last") handler.post { speaking = false }
            }
            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) { handler.post { speaking = false } }
        })
    }

    private fun readCurrentWindow() {
        if (getSystemService(KeyguardManager::class.java)?.isDeviceLocked == true) return
        if (!ready) { Toast.makeText(this, "Для чтения установите русский офлайн-голос в настройках синтеза речи Android.", Toast.LENGTH_LONG).show(); return }
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() == packageName) {
            say("Откройте страницу или приложение, затем нажмите кнопку чтения экрана.")
            return
        }
        val parts = sequence {
            val stack = ArrayDeque<AccessibilityNodeInfo>()
            stack.add(root)
            var visited = 0
            while (stack.isNotEmpty() && visited <= ScreenTextCollector.MAX_NODES) {
                val node = stack.removeLast()
                visited++
                val visible = node.isVisibleToUser
                val password = node.isPassword
                if (visible && !password) {
                    for (index in minOf(node.childCount, ScreenTextCollector.MAX_NODES) - 1 downTo 0) {
                        if (stack.size < ScreenTextCollector.MAX_NODES) node.getChild(index)?.let(stack::add)
                    }
                }
                yield(ScreenTextPart(
                    text = node.text?.toString().orEmpty().ifBlank { node.contentDescription?.toString().orEmpty() },
                    visible = visible, password = password,
                ))
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
        val result = ScreenTextCollector.collect(parts)
        val spoken = if (result.text.isBlank()) "На экране нет доступного текста. Изображения и скрытое содержимое не прочитаны."
            else "Доступный текст открытого экрана. ${result.text}" +
                if (result.truncated) ". Прочитан ограниченный фрагмент. Для продолжения выберите нужный текст и передайте его помощнику." else ""
        say(spoken)
    }

    private fun say(text: String) {
        val engine = tts ?: return
        engine.stop()
        val chunks = text.chunked(TextToSpeech.getMaxSpeechInputLength())
        speaking = true
        chunks.forEachIndexed { index, chunk ->
            val result = engine.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null, if (index == chunks.lastIndex) "screen-last" else "screen-$index")
            if (result == TextToSpeech.ERROR) { onInterrupt(); return }
        }
    }

    // Window events are used only to stop speech on lock; no screen text is collected here.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (speaking && getSystemService(KeyguardManager::class.java)?.isDeviceLocked == true) onInterrupt()
    }
    override fun onInterrupt() { speaking = false; tts?.stop() }
    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(buttonCallback)
        onInterrupt()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
