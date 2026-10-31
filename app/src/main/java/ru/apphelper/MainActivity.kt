package ru.apphelper

import android.Manifest
import android.app.role.RoleManager
import android.app.KeyguardManager
import ru.apphelper.notifications.MessageTextOrigin
import ru.apphelper.notifications.NotificationReadingPreferences
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale
import kotlin.math.roundToInt
import ru.apphelper.calls.CallEvent
import ru.apphelper.calls.CallEventStore
import ru.apphelper.calls.PendingUnknownCaller
import ru.apphelper.calls.UnknownCallerStore
import ru.apphelper.contacts.ContactInsertHelper
import ru.apphelper.data.UserProfileStore
import ru.apphelper.dialog.VoiceCommand
import ru.apphelper.dialog.VoiceCommandRouter
import ru.apphelper.dialog.VoiceEvent
import ru.apphelper.dialog.VoiceInteractionReducer
import ru.apphelper.dialog.VoicePhase
import ru.apphelper.domain.UserProfile
import ru.apphelper.journal.EventJournal
import ru.apphelper.journal.JournalEventType
import ru.apphelper.location.DeviceLocation
import ru.apphelper.location.LocationAssistant
import ru.apphelper.location.NavigationHelper
import ru.apphelper.location.SafeLocation
import ru.apphelper.location.SafePlaceStore
import ru.apphelper.messages.AnalysisUncertainty
import ru.apphelper.messages.CurrentMessageSession
import ru.apphelper.messages.HttpMessageAnalysisProvider
import ru.apphelper.messages.MessageAnalysis
import ru.apphelper.messages.MessageFactCommand
import ru.apphelper.messages.MessageFactsExtractor
import ru.apphelper.messages.MessageQueue
import ru.apphelper.messages.ReplyDraft
import ru.apphelper.messages.RecipientReview
import ru.apphelper.notifications.NotificationEvent
import ru.apphelper.notifications.NotificationEventStore
import ru.apphelper.profile.ProfileTuningSession
import ru.apphelper.ui.accessibility.AccessibleAction
import ru.apphelper.ui.accessibility.AccessibleSection
import ru.apphelper.ui.onboarding.AppHelperTheme
import ru.apphelper.ui.onboarding.OnboardingFlow
import ru.apphelper.voice.AndroidVoiceAssistant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) receiveSharedText(intent)
        val readingPreferences = NotificationReadingPreferences(this)
        val profileStore = UserProfileStore(this)
        val journal = EventJournal(this)
        val unknownCallerStore = UnknownCallerStore(this)
        val safePlaceStore = SafePlaceStore(this)
        val locationAssistant = LocationAssistant(this)
        val messageAnalysisProvider = HttpMessageAnalysisProvider(BuildConfig.AI_GATEWAY_URL)

        setContent {
            AppHelperTheme {
                var profile by remember { mutableStateOf(profileStore.load()) }
                var microphoneGranted by remember {
                    mutableStateOf(hasPermission(Manifest.permission.RECORD_AUDIO))
                }
                var contactsGranted by remember {
                    mutableStateOf(hasPermission(Manifest.permission.READ_CONTACTS))
                }
                var locationGranted by remember {
                    mutableStateOf(
                        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                }
                var voiceError by remember { mutableStateOf<String?>(null) }
                var voicePhase by remember { mutableStateOf<VoicePhase>(VoicePhase.Idle) }
                var messageQueue by remember { mutableStateOf(MessageQueue.fromNewestFirst(NotificationEventStore.snapshot())) }
                var latestEvent by remember { mutableStateOf<NotificationEvent?>(messageQueue.selected) }
                var currentMessageSession by remember { mutableStateOf<CurrentMessageSession?>(null) }
                var replyDraft by remember { mutableStateOf<ReplyDraft?>(null) }
                var awaitingDraftDictation by remember { mutableStateOf(false) }
                var recipientReview by remember { mutableStateOf<RecipientReview?>(null) }
                var latestCall by remember { mutableStateOf<CallEvent?>(null) }
                var pendingUnknownCaller by remember {
                    mutableStateOf<PendingUnknownCaller?>(unknownCallerStore.load())
                }
                var safeHome by remember { mutableStateOf<SafeLocation?>(safePlaceStore.loadHome()) }
                var lastKnownLocation by remember { mutableStateOf<DeviceLocation?>(null) }

                var awaitingNotificationCommand by remember { mutableStateOf(false) }
                var awaitingMessageFollowUp by remember { mutableStateOf(false) }
                var awaitingUnknownCallerConfirmation by remember { mutableStateOf(false) }
                var awaitingContactName by remember { mutableStateOf(false) }
                var listeningForGeneralCommand by remember { mutableStateOf(false) }
                var tuningSession by remember { mutableStateOf<ProfileTuningSession?>(null) }
                var lastPrompt by remember { mutableStateOf<String?>(null) }

                var readingBatch by remember { mutableStateOf<List<String>>(emptyList()) }
                var readingGeneration by remember { mutableStateOf(0L) }
                var slowerReading by remember { mutableStateOf(false) }
                var knownSources by remember { mutableStateOf(readingPreferences.sources()) }
                var sourceRevision by remember { mutableStateOf(0) }
                lateinit var voice: AndroidVoiceAssistant

                fun speak(text: String) {
                    voice.setSpeechRate(if (slowerReading || profile.speech.slowerSpeech) 0.78f else 1.0f)
                    voice.speak(text)
                }

                fun speakAndListen(prompt: String) {
                    lastPrompt = prompt
                    voicePhase = VoiceInteractionReducer.reduce(voicePhase, VoiceEvent.Prompt(prompt))
                    voiceError = null
                    voice.setSpeechRate(if (slowerReading || profile.speech.slowerSpeech) 0.78f else 1.0f)
                    voice.speak(prompt) {
                        if (microphoneGranted) {
                            voicePhase = VoiceInteractionReducer.reduce(voicePhase, VoiceEvent.SpeechFinished)
                            voice.startListening()
                        }
                    }
                }

                fun speakAndListenNotification(prompt: String) {
                    awaitingNotificationCommand = true
                    speakAndListen(prompt)
                }

                fun askTuningQuestion(session: ProfileTuningSession) {
                    val prompt = session.currentPrompt()
                    if (prompt == null) return
                    speakAndListen(prompt)
                }

                fun startProfileTuning() {
                    if (!microphoneGranted) {
                        speak("Для голосовой настройки нужен доступ к микрофону.")
                        return
                    }
                    awaitingNotificationCommand = false
                    awaitingMessageFollowUp = false
                    awaitingUnknownCallerConfirmation = false
                    awaitingContactName = false
                    listeningForGeneralCommand = false
                    val session = ProfileTuningSession(priorities = profile.priorities)
                    tuningSession = session
                    speakAndListen(
                        "Хорошо. Настроюсь под вас. Отвечайте да или нет. ${session.currentPrompt()}",
                    )
                }

                fun finishProfileTuning(session: ProfileTuningSession) {
                    val updatedProfile = profile.copy(priorities = session.priorities)
                    profileStore.save(updatedProfile)
                    profile = updatedProfile
                    tuningSession = null
                    speak("Готово. Я сохранил ваши приоритеты. Вы сможете снова сказать: настройся под меня.")
                }

                fun askAboutUnknownCaller() {
                    val caller = pendingUnknownCaller ?: return
                    awaitingUnknownCallerConfirmation = true
                    listeningForGeneralCommand = false
                    speakAndListen("Последний неизвестный номер ${caller.phoneNumber}. Сохранить в контакты?")
                }

                fun askContactName() {
                    awaitingContactName = true
                    speakAndListen("Как назвать этот контакт? Назовите имя.")
                }

                fun openContactForm(displayName: String) {
                    val caller = pendingUnknownCaller ?: return
                    ContactInsertHelper.launch(this, caller.phoneNumber, displayName)
                    unknownCallerStore.clear()
                    pendingUnknownCaller = null
                    awaitingUnknownCallerConfirmation = false
                    awaitingContactName = false
                    speak("Открыта форма нового контакта. Проверьте имя и номер перед сохранением.")
                }

                fun pauseReading() {
                    readingGeneration++
                    readingBatch = emptyList()
                    voice.stopListening()
                    voice.stopSpeaking()
                    voicePhase = VoicePhase.Idle
                    awaitingNotificationCommand = false
                    awaitingMessageFollowUp = currentMessageSession != null
                }

                fun readLatestNotification() {
                    val event = latestEvent ?: run { speak("Нет выбранного сообщения."); return }
                    if (getSystemService(KeyguardManager::class.java)?.isDeviceLocked == true) {
                        pauseReading()
                        return
                    }
                    readingGeneration++
                    val generation = readingGeneration
                    awaitingNotificationCommand = false
                    awaitingMessageFollowUp = true
                    currentMessageSession = CurrentMessageSession(event)
                    replyDraft = null
                    recipientReview = null
                    awaitingDraftDictation = false
                    voice.stopListening()
                    voicePhase = VoicePhase.Idle
                    val spoken = if (event.text.isBlank()) {
                        "В уведомлении нет доступного текста. Откройте исходное приложение."
                    } else if (event.textOrigin == MessageTextOrigin.SHARED_TEXT) {
                        "Переданный вами текст. ${event.text}"
                    } else {
                        "${event.appName}. Отправитель в уведомлении: ${event.sender}. " +
                            "Доступный текст уведомления; он может быть неполным. ${event.text}"
                    }
                    voice.setSpeechRate(if (slowerReading || profile.speech.slowerSpeech) 0.78f else 1.0f)
                    voice.speak(spoken) {
                        if (generation == readingGeneration) {
                            NotificationEventStore.markRead(event)
                            messageQueue = messageQueue.markRead(event)
                            val batch = readingBatch
                            if (batch.isNotEmpty()) {
                                readingBatch = batch.drop(1)
                                // Newer same-key text stays pending rather than being lost on completion.
                                if (messageQueue.pending.firstOrNull { it.id == event.id } == event) {
                                    messageQueue = messageQueue.dismiss(event.id)
                                }
                                val nextId = readingBatch.firstOrNull { id -> messageQueue.pending.any { it.id == id } }
                                if (nextId != null) {
                                    readingBatch = readingBatch.dropWhile { it != nextId }
                                    messageQueue = messageQueue.select(nextId)
                                    latestEvent = messageQueue.selected
                                    readLatestNotification()
                                } else {
                                    readingBatch = emptyList()
                                    speakAndListen("Чтение выбранной очереди завершено. Слушаю следующую команду.")
                                }
                            } else {
                                speakAndListen("Текст озвучен. Можно сказать: повтори, кто написал, следующее, позже или объясни.")
                            }
                        }
                    }
                }

                fun readInbox(all: Boolean) {
                    pauseReading()
                    if (all) messageQueue = messageQueue.restoreDeferred()
                    val items = messageQueue.unread.filter { all || it.id !in messageQueue.deferredIds }
                    if (items.isEmpty()) { speak("Доступных входящих сообщений нет."); return }
                    readingBatch = if (all) items.map { it.id } else emptyList()
                    messageQueue = messageQueue.select(items.first().id)
                    latestEvent = messageQueue.selected
                    readLatestNotification()
                }

                fun deferCurrentMessage() {
                    pauseReading()
                    val postponed = currentMessageSession?.event ?: latestEvent
                    if (postponed != null && messageQueue.pending.none { it.id == postponed.id }) {
                        messageQueue = messageQueue.receive(postponed)
                    }
                    messageQueue = messageQueue.defer(postponed?.id)
                    currentMessageSession = null
                    replyDraft = null
                    recipientReview = null
                    awaitingDraftDictation = false
                    awaitingMessageFollowUp = false
                    latestEvent = messageQueue.selected
                    speak("Сообщение отложено. Оно сохранено в очереди этого сеанса.")
                }

                fun closeCurrentMessage() {
                    pauseReading()
                    val completed = currentMessageSession?.event ?: latestEvent
                    if (completed != null) {
                        NotificationEventStore.markRead(completed)
                        messageQueue = messageQueue.dismissSnapshot(completed)
                    }
                    awaitingMessageFollowUp = false
                    currentMessageSession = null
                    replyDraft = null
                    recipientReview = null
                    awaitingDraftDictation = false
                    latestEvent = messageQueue.selected
                    val next = latestEvent
                    if (next != null) {
                        speak("Следующее сообщение выбрано. Нажмите «Прочитать» или скажите «прочитай».")
                    } else speak("Сообщение закрыто.")
                }

                fun answerMessageFact(command: MessageFactCommand) {
                    val session = currentMessageSession
                    if (session == null || !session.hasReadableText) {
                        speak("Сейчас нет сообщения с доступным текстом.")
                        return
                    }
                    awaitingMessageFollowUp = true
                    speakAndListen(MessageFactsExtractor.answer(command, session.originalText))
                }

                fun formatAnalysis(command: VoiceCommand, analysis: MessageAnalysis): String {
                    val uncertaintyPrefix = when (analysis.uncertainty) {
                        AnalysisUncertainty.LOW -> ""
                        AnalysisUncertainty.MEDIUM -> "Смысл сообщения может быть немного неоднозначным. "
                        AnalysisUncertainty.HIGH -> "Сообщение неоднозначно, поэтому это только осторожная интерпретация. "
                    }
                    val body = when (command) {
                        VoiceCommand.MESSAGE_MAIN_POINT -> analysis.summary.ifBlank { "Не удалось выделить главное без домыслов." }
                        VoiceCommand.MESSAGE_REQUEST -> analysis.explicitRequest
                            ?: "В сообщении нет однозначно сформулированной просьбы или ожидаемого действия."
                        VoiceCommand.EXPLAIN_MESSAGE,
                        VoiceCommand.SIMPLIFY_MESSAGE -> analysis.plainExplanation.ifBlank {
                            "Не удалось объяснить сообщение без добавления новых предположений."
                        }
                        else -> analysis.summary
                    }
                    return uncertaintyPrefix + body
                }

                fun analyzeCurrentMessage(command: VoiceCommand) {
                    val session = currentMessageSession
                    if (session == null || !session.hasReadableText) {
                        speak("Сейчас нет сообщения с доступным текстом.")
                        return
                    }
                    pauseReading()
                    val generation = readingGeneration
                    awaitingMessageFollowUp = false
                    speak("Анализирую сообщение.")
                    messageAnalysisProvider.analyze(
                        originalText = session.originalText,
                        onSuccess = { analysis ->
                            if (generation == readingGeneration && currentMessageSession?.event?.id == session.event.id) {
                                awaitingMessageFollowUp = true
                                speakAndListen(formatAnalysis(command, analysis))
                            }
                        },
                        onError = { error ->
                            if (generation == readingGeneration && currentMessageSession?.event?.id == session.event.id) {
                                awaitingMessageFollowUp = true
                                speakAndListen(error)
                            }
                        },
                    )
                }

                fun repeatCurrentMessage() {
                    val session = currentMessageSession
                    if (session == null || !session.hasReadableText) {
                        speak("Сейчас нет сообщения с доступным текстом.")
                        return
                    }
                    awaitingMessageFollowUp = true
                    speakAndListen("Дословно: ${session.originalText}")
                }

                fun speakMissedEvents() {
                    pauseReading()
                    val groups = messageQueue.unread.groupingBy { it.appName }.eachCount()
                    if (groups.isEmpty()) speak("Доступных входящих сообщений нет.")
                    else speak("В очереди этого сеанса ${messageQueue.unread.size} неозвученных сообщений. " +
                        groups.entries.joinToString(". ") { "${it.key}: ${it.value}" } +
                        ". Можно сказать: прочитай новые сообщения или читай всё по очереди.")
                }

                fun tellWhereAmI() {
                    if (!locationGranted) {
                        speak("Нужно разрешить доступ к местоположению.")
                        return
                    }
                    locationAssistant.getCurrentLocation(
                        onSuccess = { location ->
                            lastKnownLocation = location
                            val lat = String.format(Locale.US, "%.5f", location.latitude)
                            val lon = String.format(Locale.US, "%.5f", location.longitude)
                            val accuracy = location.accuracyMeters?.roundToInt()
                            val accuracyText = accuracy?.let { " Точность около $it метров." }.orEmpty()
                            speak("Ваши координаты: широта $lat, долгота $lon.$accuracyText")
                        },
                        onError = { speak(it) },
                    )
                }

                fun saveCurrentAsHome() {
                    if (!locationGranted) {
                        speak("Нужно разрешить доступ к местоположению.")
                        return
                    }
                    locationAssistant.getCurrentLocation(
                        onSuccess = { location ->
                            lastKnownLocation = location
                            safePlaceStore.saveHome(location)
                            safeHome = safePlaceStore.loadHome()
                            speak("Текущее место сохранено как дом.")
                        },
                        onError = { speak(it) },
                    )
                }

                fun navigateHome() {
                    val home = safeHome
                    if (home == null) {
                        speak("Дом ещё не сохранён. Скажите: запомни это место как дом.")
                        return
                    }
                    val opened = NavigationHelper.openRoute(this, home)
                    if (opened) speak("Открываю маршрут домой.")
                    else speak("На телефоне не найдено приложение, которое может открыть маршрут.")
                }

                fun handleRecognized(recognized: String) {
                    // Ignore callbacks after cancellation or once an utterance has already been processed.
                    if (voicePhase !is VoicePhase.AwaitingSpeech) return
                    voicePhase = VoiceInteractionReducer.reduce(voicePhase, VoiceEvent.Recognized(recognized))
                    voiceError = null
                    if (awaitingDraftDictation) {
                        awaitingDraftDictation = false
                        val draft = replyDraft
                        if (draft != null && currentMessageSession?.event?.id == draft.sourceNotificationId) {
                            replyDraft = draft.edit(recognized.trim())
                            speak("Текст записан в черновик. Проверьте его перед дальнейшими действиями.")
                        }
                        return
                    }
                    if (awaitingContactName) {
                        val name = recognized.trim()
                        if (name.isBlank()) askContactName() else openContactForm(name)
                        return
                    }

                    val command = VoiceCommandRouter.parse(recognized)
                    // Dictation is handled above. Reading controls never execute message contents.
                    if (tuningSession == null && !awaitingUnknownCallerConfirmation) {
                        when (command) {
                            VoiceCommand.READ_ALL -> { readInbox(true); return }
                            VoiceCommand.READ_NEW -> { readInbox(false); return }
                            VoiceCommand.PAUSE -> { pauseReading(); return }
                            VoiceCommand.NEXT -> {
                                deferCurrentMessage()
                                if (latestEvent != null) readLatestNotification()
                                return
                            }
                            VoiceCommand.SENDER -> {
                                val event = currentMessageSession?.event ?: latestEvent
                                pauseReading()
                                speak(event?.let { "Источник: ${it.appName}. Отправитель в уведомлении: ${it.sender}." }
                                    ?: "Нет выбранного сообщения.")
                                return
                            }
                            VoiceCommand.SLOWER -> {
                                pauseReading()
                                slowerReading = true
                                if (currentMessageSession != null) {
                                    latestEvent = currentMessageSession?.event
                                    readLatestNotification()
                                } else speak("Буду читать медленнее.")
                                return
                            }
                            VoiceCommand.MISSED -> { speakMissedEvents(); return }
                            else -> Unit
                        }
                    }


                    tuningSession?.let { current ->
                        when (command) {
                            VoiceCommand.READ,
                            VoiceCommand.LATER -> {
                                val next = current.answer(command == VoiceCommand.READ)
                                if (next.isComplete) {
                                    finishProfileTuning(next)
                                } else {
                                    tuningSession = next
                                    askTuningQuestion(next)
                                }
                            }
                            VoiceCommand.REPEAT -> askTuningQuestion(current)
                            else -> speakAndListen("Не понял. Ответьте да, нет или скажите повтори. ${current.currentPrompt()}")
                        }
                        return
                    }

                    if (awaitingUnknownCallerConfirmation) {
                        when (command) {
                            VoiceCommand.READ -> {
                                awaitingUnknownCallerConfirmation = false
                                askContactName()
                            }
                            VoiceCommand.LATER -> {
                                awaitingUnknownCallerConfirmation = false
                                speak("Хорошо. Номер останется в списке, можно сохранить его позже.")
                            }
                            VoiceCommand.REPEAT -> askAboutUnknownCaller()
                            else -> speakAndListen("Не понял. Скажите да, нет или повтори.")
                        }
                        return
                    }

                    if (awaitingNotificationCommand) {
                        when (command) {
                            VoiceCommand.READ -> readLatestNotification()
                            VoiceCommand.LATER -> {
                                deferCurrentMessage()
                            }
                            VoiceCommand.REPEAT -> lastPrompt?.let(::speakAndListenNotification)
                            else -> speakAndListenNotification("Не понял ответ. Скажите: прочитай, позже или повтори.")
                        }
                        return
                    }

                    if (awaitingMessageFollowUp && currentMessageSession != null) {
                        when (command) {
                            VoiceCommand.READ,
                            VoiceCommand.REPEAT -> repeatCurrentMessage()
                            VoiceCommand.MESSAGE_DATE -> answerMessageFact(MessageFactCommand.DATE)
                            VoiceCommand.MESSAGE_TIME -> answerMessageFact(MessageFactCommand.TIME)
                            VoiceCommand.MESSAGE_PHONE -> answerMessageFact(MessageFactCommand.PHONE)
                            VoiceCommand.MESSAGE_AMOUNT -> answerMessageFact(MessageFactCommand.AMOUNT)
                            VoiceCommand.MESSAGE_LINK -> answerMessageFact(MessageFactCommand.LINK)
                            VoiceCommand.EXPLAIN_MESSAGE,
                            VoiceCommand.SIMPLIFY_MESSAGE,
                            VoiceCommand.MESSAGE_MAIN_POINT,
                            VoiceCommand.MESSAGE_REQUEST -> analyzeCurrentMessage(command)
                            VoiceCommand.CLOSE_MESSAGE -> closeCurrentMessage()
                            VoiceCommand.LATER -> deferCurrentMessage()
                            else -> speakAndListen(
                                "Я работаю с текущим сообщением. Скажите: повтори, объясни, скажи проще, что главное, что от меня хотят, есть ли дата, время, телефон, сумма, ссылка или закрой сообщение.",
                            )
                        }
                        return
                    }

                    if (listeningForGeneralCommand) {
                        listeningForGeneralCommand = false
                        when (command) {
                            VoiceCommand.MISSED -> speakMissedEvents()
                            VoiceCommand.WHERE_AM_I -> tellWhereAmI()
                            VoiceCommand.GO_HOME -> navigateHome()
                            VoiceCommand.SAVE_HOME -> saveCurrentAsHome()
                            VoiceCommand.TUNE_TO_ME -> startProfileTuning()
                            VoiceCommand.READ -> readLatestNotification()
                            VoiceCommand.LATER -> deferCurrentMessage()
                            VoiceCommand.REPEAT -> lastPrompt?.let(::speak)
                            else -> speak(
                                "Я понимаю команды: что я пропустил, где я, запомни дом, отведи меня домой и настройся под меня.",
                            )
                        }
                    }
                }

                voice = remember {
                    AndroidVoiceAssistant(
                        context = this,
                        onRecognized = ::handleRecognized,
                        onError = { error ->
                            voiceError = error
                            voicePhase = VoiceInteractionReducer.reduce(
                                voicePhase, VoiceEvent.RecognitionFailed(error),
                            )
                            // Keep message/session state for retry rather than discarding context.
                        },
                    )
                }

                DisposableEffect(Unit) {
                    val lifecycleObserver = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) pauseReading()
                    }
                    lifecycle.addObserver(lifecycleObserver)
                    val notificationListener: (NotificationEvent) -> Unit = { event ->
                        messageQueue = messageQueue.receive(event)
                        knownSources = readingPreferences.sources()
                        if (currentMessageSession == null) latestEvent = messageQueue.selected
                        // Incoming content does not start TTS or interrupt the user's current action.
                    }
                    val callListener: (CallEvent) -> Unit = { event ->
                        latestCall = event
                        if (event.incoming) {
                            readingGeneration++
                            readingBatch = emptyList()
                            awaitingDraftDictation = false
                            voice.stopListening()
                            voice.stopSpeaking()
                            voicePhase = VoiceInteractionReducer.reduce(voicePhase, VoiceEvent.Cancel)
                            if (event.displayName == null && event.phoneNumber.isNotBlank()) {
                                pendingUnknownCaller = PendingUnknownCaller(
                                    phoneNumber = event.phoneNumber,
                                    detectedAt = event.timestampMillis,
                                )
                            }
                            val spokenCaller = event.displayName
                                ?: event.phoneNumber.takeIf { it.isNotBlank() }
                                ?: "неизвестный номер"
                            lastPrompt = "Входящий звонок. $spokenCaller."
                        }
                    }
                    NotificationEventStore.addListener(notificationListener)
                    CallEventStore.addListener(callListener)
                    onDispose {
                        lifecycle.removeObserver(lifecycleObserver)
                        NotificationEventStore.removeListener(notificationListener)
                        CallEventStore.removeListener(callListener)
                        voice.release()
                    }
                }

                val microphoneLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> microphoneGranted = granted }

                val contactsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> contactsGranted = granted }

                val locationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                }

                val roleLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { }

                Surface(color = Color(0xFF0E1116)) {
                    if (!profile.onboardingCompleted) {
                        OnboardingFlow(
                            initialProfile = profile,
                            onRequestMicrophone = {
                                microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            microphoneGranted = microphoneGranted,
                            onTestVoice = { text, slowerSpeech ->
                                voice.setSpeechRate(if (slowerSpeech) 0.78f else 1.0f)
                                voice.speak(text)
                            },
                            onFinish = { completed ->
                                val completedProfile = completed.copy(
                                    permissions = completed.permissions.copy(
                                        microphoneGranted = microphoneGranted,
                                    ),
                                    onboardingCompleted = true,
                                )
                                profileStore.save(completedProfile)
                                profile = completedProfile
                            },
                        )
                    } else {
                        HomeScreen(
                            profile = profile,
                            latestEvent = latestEvent,
                            latestCall = latestCall,
                            pendingUnknownCaller = pendingUnknownCaller,
                            safeHome = safeHome,
                            lastKnownLocation = lastKnownLocation,
                            contactsGranted = contactsGranted,
                            locationGranted = locationGranted,
                            voiceError = voiceError,
                            tuningActive = tuningSession != null,
                            onEnableNotificationAccess = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            onRequestContacts = {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            onRequestLocation = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                            onRequestCallScreeningRole = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = getSystemService(RoleManager::class.java)
                                    if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true &&
                                        !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                                    ) {
                                        roleLauncher.launch(
                                            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                                        )
                                    }
                                } else {
                                    speak("Определитель звонков требует Android 10 или новее.")
                                }
                            },
                            inbox = messageQueue,
                            knownSources = knownSources,
                            sourceRevision = sourceRevision,
                            isSourceEnabled = { readingPreferences.isEnabled(it) },
                            onToggleSource = { source ->
                                val enabled = !readingPreferences.isEnabled(source)
                                readingPreferences.setEnabled(source, enabled)
                                sourceRevision++
                                if (!enabled) {
                                    pauseReading()
                                    NotificationEventStore.removePackage(source)
                                    messageQueue = messageQueue.withoutPackage(source)
                                    if (currentMessageSession?.event?.packageName == source) {
                                        currentMessageSession = null
                                        replyDraft = null
                                        recipientReview = null
                                    }
                                    latestEvent = messageQueue.selected
                                }
                            },
                            onReadAll = { readInbox(true) },
                            onReadNew = { readInbox(false) },
                            onPauseReading = { pauseReading() },
                            onNextMessage = { deferCurrentMessage(); if (latestEvent != null) readLatestNotification() },
                            onReadScreenSettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            currentMessage = currentMessageSession,
                            replyDraft = replyDraft,
                            recipientReview = recipientReview,
                            onConfirmRecipientDisplay = { recipientReview = recipientReview?.confirmDisplay() },
                            onStartDraft = {
                                currentMessageSession?.let { session ->
                                    replyDraft = ReplyDraft(session.event.id, session.source, session.sender)
                                    recipientReview = RecipientReview(session.event.id, session.source, session.sender)
                                }
                            },
                            onDraftTextChange = { value -> replyDraft = replyDraft?.edit(value) },
                            onDictateDraft = {
                                if (!microphoneGranted) microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                else if (replyDraft != null) {
                                    awaitingDraftDictation = true
                                    speakAndListen("Продиктуйте полный текст ответа. Предыдущий текст будет заменён.")
                                }
                            },
                            onReviewDraft = {
                                replyDraft = replyDraft?.markReviewed()
                                replyDraft?.takeIf { it.reviewed }?.let {
                                    speak("Черновик для ${it.displayedSender}. Текст: ${it.text}. Это только черновик, отправка недоступна.")
                                }
                            },
                            onDiscardDraft = { replyDraft = null; recipientReview = null; awaitingDraftDictation = false },
                            onReadNotification = { readLatestNotification() },
                            onRepeatMessage = { repeatCurrentMessage() },
                            onExplainMessage = { analyzeCurrentMessage(VoiceCommand.EXPLAIN_MESSAGE) },
                            onMainPoint = { analyzeCurrentMessage(VoiceCommand.MESSAGE_MAIN_POINT) },
                            onCloseMessage = { closeCurrentMessage() },
                            onLater = { deferCurrentMessage() },
                            onMissed = { speakMissedEvents() },
                            onWhereAmI = { tellWhereAmI() },
                            onSaveHome = { saveCurrentAsHome() },
                            onNavigateHome = { navigateHome() },
                            onTuneToMe = {
                                if (!microphoneGranted) {
                                    microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    startProfileTuning()
                                }
                            },
                            onSaveUnknown = {
                                if (microphoneGranted) askAboutUnknownCaller()
                                else microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onListen = {
                                readingGeneration++
                                readingBatch = emptyList()
                                voice.stopSpeaking()
                                val recoverable = voicePhase as? VoicePhase.RecoverableError
                                if (recoverable != null && microphoneGranted) {
                                    voicePhase = VoiceInteractionReducer.reduce(voicePhase, VoiceEvent.Retry)
                                    speakAndListen(recoverable.prompt)
                                } else if (!microphoneGranted) {
                                    microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else if (pendingUnknownCaller != null) {
                                    askAboutUnknownCaller()
                                } else if (awaitingMessageFollowUp && currentMessageSession != null) {
                                    speakAndListen("Слушаю команду по сообщению.")
                                } else {
                                    listeningForGeneralCommand = true
                                    speakAndListen("Слушаю.")
                                }
                            },
                            onRepeat = {
                                if (microphoneGranted && !lastPrompt.isNullOrBlank()) {
                                    voice.stopListening()
                                    speakAndListen(lastPrompt!!)
                                } else if (!microphoneGranted) {
                                    microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    speak("Пока нечего повторять.")
                                }
                            },
                            onCancelVoice = {
                                readingGeneration++
                                readingBatch = emptyList()
                                voice.stopListening()
                                voice.stopSpeaking()
                                voicePhase = VoiceInteractionReducer.reduce(voicePhase, VoiceEvent.Cancel)
                                voiceError = null
                                awaitingNotificationCommand = false
                                awaitingMessageFollowUp = false
                                awaitingUnknownCallerConfirmation = false
                                awaitingContactName = false
                                listeningForGeneralCommand = false
                                tuningSession = null
                                awaitingDraftDictation = false
                                speak("Действие отменено.")
                            },
                            onSpeak = {
                                val greeting = if (profile.displayName.isBlank()) {
                                    "Здравствуйте. Я готов помочь."
                                } else {
                                    "Здравствуйте, ${profile.displayName}. Я готов помочь."
                                }
                                speak(greeting)
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receiveSharedText(intent)
    }

    private fun receiveSharedText(input: Intent?) {
        val text = when (input?.action) {
            Intent.ACTION_SEND -> input.getCharSequenceExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> input.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            else -> null
        }?.toString()?.trim()?.take(12000).orEmpty()
        if (text.isEmpty()) return
        NotificationEventStore.add(NotificationEvent(
            id = "shared:${java.util.UUID.randomUUID()}", packageName = packageName,
            appName = "Переданный текст", sender = "Выбранный вами фрагмент",
            text = text, receivedAt = System.currentTimeMillis(), textOrigin = MessageTextOrigin.SHARED_TEXT,
        ))
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

@androidx.compose.runtime.Composable
private fun HomeScreen(
    profile: UserProfile,
    inbox: MessageQueue,
    knownSources: Map<String, String>,
    sourceRevision: Int,
    isSourceEnabled: (String) -> Boolean,
    onToggleSource: (String) -> Unit,
    onReadAll: () -> Unit,
    onReadNew: () -> Unit,
    onPauseReading: () -> Unit,
    onNextMessage: () -> Unit,
    onReadScreenSettings: () -> Unit,
    latestEvent: NotificationEvent?,
    currentMessage: CurrentMessageSession?,
    replyDraft: ReplyDraft?,
    recipientReview: RecipientReview?,
    onConfirmRecipientDisplay: () -> Unit,
    onStartDraft: () -> Unit,
    onDraftTextChange: (String) -> Unit,
    onDictateDraft: () -> Unit,
    onReviewDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
    latestCall: CallEvent?,
    pendingUnknownCaller: PendingUnknownCaller?,
    safeHome: SafeLocation?,
    lastKnownLocation: DeviceLocation?,
    contactsGranted: Boolean,
    locationGranted: Boolean,
    voiceError: String?,
    tuningActive: Boolean,
    onEnableNotificationAccess: () -> Unit,
    onRequestContacts: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onReadNotification: () -> Unit,
    onRepeatMessage: () -> Unit,
    onExplainMessage: () -> Unit,
    onMainPoint: () -> Unit,
    onCloseMessage: () -> Unit,
    onLater: () -> Unit,
    onMissed: () -> Unit,
    onWhereAmI: () -> Unit,
    onSaveHome: () -> Unit,
    onNavigateHome: () -> Unit,
    onTuneToMe: () -> Unit,
    onSaveUnknown: () -> Unit,
    onListen: () -> Unit,
    onRepeat: () -> Unit,
    onCancelVoice: () -> Unit,
    onSpeak: () -> Unit,
) {
    var showTools by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AccessibleSection(
            title = "Мой помощник",
            description = "Нажмите «Говорить» или выберите действие ниже.",
        )

        // Primary action is always visible before technical or optional functionality.
        AccessibleAction(
            label = "Говорить",
            accessibilityLabel = "Говорить с помощником. Двойное нажатие для начала.",
            onClick = onListen,
            primary = true,
        )

        AccessibleAction(label = "Повторить вопрос", onClick = onRepeat)
        AccessibleAction(label = "Отмена", onClick = onCancelVoice)

        AccessibleSection(
            title = "Входящие и чтение",
            description = "Не озвучено: ${inbox.unread.size}. Отложено: ${inbox.deferredIds.size}. " +
                "Уведомления могут содержать только часть сообщения. Прочтение здесь не меняет статус в мессенджере.",
        )
        AccessibleAction(label = "Прочитать входящие", onClick = onReadNew)
        AccessibleAction(label = "Читать всё по очереди", onClick = onReadAll)
        AccessibleAction(label = "Пауза чтения", onClick = onPauseReading)
        if (inbox.pending.isNotEmpty()) AccessibleAction(label = "Следующее сообщение", onClick = onNextMessage)

        if (currentMessage != null) {
            AccessibleSection(
                title = "Работа с сообщением",
                description = "От: ${currentMessage.sender}. Источник: ${currentMessage.source}.",
            )
            AccessibleSection(
                title = "Возможность быстрого ответа",
                description = if (currentMessage.event.quickReplyAvailable) {
                    "В уведомлении обнаружено действие ответа. Отправка пока отключена."
                } else {
                    "В этом уведомлении быстрый ответ не обнаружен."
                },
            )
            Text("Доступный исходный текст: ${currentMessage.originalText.ifBlank { "Текст не передан источником" }}")
            AccessibleAction(label = "Прочитать ещё раз", onClick = onRepeatMessage)
            AccessibleAction(label = "Объяснить простыми словами", onClick = onExplainMessage)
            AccessibleAction(label = "Что главное?", onClick = onMainPoint)
            if (replyDraft == null) {
                AccessibleAction(label = "Подготовить ответ", onClick = onStartDraft)
            } else {
                AccessibleSection(
                    title = "Черновик ответа",
                    description = "Получатель не подтверждён: ${replyDraft.displayedSender}. Отправка пока недоступна.",
                )
                if (recipientReview != null && !recipientReview.confirmedForDisplay) {
                    AccessibleAction(label = "Отображаемое имя проверено, адрес ещё не установлен", onClick = onConfirmRecipientDisplay)
                }
                OutlinedTextField(
                    value = replyDraft.text,
                    onValueChange = onDraftTextChange,
                    label = { Text("Текст ответа") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                AccessibleAction(label = "Продиктовать текст заново", onClick = onDictateDraft)
                if (replyDraft.canReview) {
                    AccessibleAction(label = "Прочитать черновик", onClick = onReviewDraft)
                }
                AccessibleAction(label = "Отменить черновик", onClick = onDiscardDraft)
            }
            AccessibleAction(label = "Закрыть сообщение", onClick = onCloseMessage)
        } else if (latestEvent != null) {
            AccessibleSection(
                title = "Новое сообщение",
                description = "Источник: ${latestEvent.appName}. Отправитель: ${latestEvent.sender}.",
            )
            AccessibleAction(
                label = "Прочитать вслух",
                accessibilityLabel = "Прочитать доступный текст нового уведомления вслух",
                onClick = onReadNotification,
            )
            AccessibleAction(label = "Позже", onClick = onLater)
        }

        AccessibleAction(label = "Что я пропустил?", onClick = onMissed)

        if (voiceError != null) {
            AccessibleSection(title = "Голосовая функция недоступна", description = voiceError)
            AccessibleAction(label = "Попробовать снова", onClick = onListen)
        }

        if (pendingUnknownCaller != null) {
            AccessibleSection(
                title = "Неизвестный номер",
                description = pendingUnknownCaller.phoneNumber,
            )
            AccessibleAction(label = "Сохранить контакт", onClick = onSaveUnknown)
        }

        if (latestCall != null) {
            AccessibleSection(
                title = "Последний звонок",
                description = latestCall.displayName
                    ?: latestCall.phoneNumber.ifBlank { "Неизвестный номер" },
            )
        }

        AccessibleAction(
            label = if (showTools) "Скрыть дополнительные функции" else "Помощь и дополнительные функции",
            onClick = { showTools = !showTools },
        )

        if (showTools) {
            AccessibleSection(
                title = "Дополнительные функции",
                description = "Выберите только то, что вам нужно. Разрешения можно настроить позже.",
            )
            AccessibleAction(
                label = if (tuningActive) "Идёт настройка" else "Настроить помощника под меня",
                onClick = onTuneToMe,
            )
            if (!locationGranted) {
                AccessibleAction(label = "Включить определение местоположения", onClick = onRequestLocation)
            } else {
                AccessibleAction(label = "Где я?", onClick = onWhereAmI)
                AccessibleAction(
                    label = if (safeHome == null) "Запомнить место как дом" else "Обновить место «Дом»",
                    onClick = onSaveHome,
                )
            }
            if (safeHome != null) AccessibleAction(label = "Показать дорогу домой", onClick = onNavigateHome)
            AccessibleAction(label = "Настроить чтение уведомлений", onClick = onEnableNotificationAccess)
            AccessibleSection(title = "Источники входящих",
                description = "Список пополняется при получении уведомлений. Отключение удаляет сообщения этого приложения из очереди.")
            androidx.compose.runtime.key(sourceRevision) {
                knownSources.forEach { (source, label) ->
                    val enabled = isSourceEnabled(source)
                    AccessibleAction(label = "$label: ${if (enabled) "включено — отключить" else "выключено — включить"}",
                        onClick = { onToggleSource(source) })
                }
            }
            AccessibleSection(title = "Чтение страницы или экрана",
                description = "Выделите текст в браузере и выберите «Голосовой помощник» или используйте «Поделиться». " +
                    "Для чтения открытого окна включите службу «Чтение открытого экрана» и назначьте ей кнопку специальных возможностей. " +
                    "Читается только доступный текст, без скрытой истории и распознавания изображений.")
            AccessibleAction(label = "Настроить чтение открытого экрана", onClick = onReadScreenSettings)
            if (!contactsGranted) AccessibleAction(label = "Разрешить доступ к контактам", onClick = onRequestContacts)
            AccessibleAction(label = "Настроить определитель звонков", onClick = onRequestCallScreeningRole)
            AccessibleAction(label = "Проверить голос помощника", onClick = onSpeak)
            // Coordinates are diagnostic data, not the primary answer to "Where am I?".
            if (lastKnownLocation != null) {
                Text(
                    "Последние координаты: " +
                        String.format(Locale.US, "%.5f", lastKnownLocation.latitude) + ", " +
                        String.format(Locale.US, "%.5f", lastKnownLocation.longitude),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
