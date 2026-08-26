package ru.apphelper

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import ru.apphelper.calls.CallEvent
import ru.apphelper.calls.CallEventStore
import ru.apphelper.data.UserProfileStore
import ru.apphelper.dialog.VoiceCommand
import ru.apphelper.dialog.VoiceCommandRouter
import ru.apphelper.domain.UserProfile
import ru.apphelper.notifications.NotificationEvent
import ru.apphelper.notifications.NotificationEventStore
import ru.apphelper.ui.onboarding.AppHelperTheme
import ru.apphelper.ui.onboarding.OnboardingFlow
import ru.apphelper.voice.AndroidVoiceAssistant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = UserProfileStore(this)

        setContent {
            AppHelperTheme {
                var profile by remember { mutableStateOf(store.load()) }
                var microphoneGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED,
                    )
                }
                var contactsGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                            PackageManager.PERMISSION_GRANTED,
                    )
                }
                var voiceError by remember { mutableStateOf<String?>(null) }
                var latestEvent by remember {
                    mutableStateOf<NotificationEvent?>(NotificationEventStore.snapshot().firstOrNull())
                }
                var latestCall by remember { mutableStateOf<CallEvent?>(null) }
                var awaitingNotificationCommand by remember { mutableStateOf(false) }
                var lastPrompt by remember { mutableStateOf<String?>(null) }

                lateinit var voice: AndroidVoiceAssistant

                fun speakAndListen(prompt: String) {
                    lastPrompt = prompt
                    awaitingNotificationCommand = true
                    voice.setSpeechRate(if (profile.speech.slowerSpeech) 0.78f else 1.0f)
                    voice.speak(prompt) {
                        if (microphoneGranted) voice.startListening()
                    }
                }

                fun readLatestNotification() {
                    latestEvent?.let { event ->
                        awaitingNotificationCommand = false
                        val spoken = if (event.text.isBlank()) {
                            "В уведомлении нет доступного текста."
                        } else {
                            val sender = event.sender.ifBlank { event.appName }
                            "$sender. ${event.text}"
                        }
                        voice.speak(spoken)
                        latestEvent = null
                    }
                }

                voice = remember {
                    AndroidVoiceAssistant(
                        context = this,
                        onRecognized = { recognized ->
                            if (awaitingNotificationCommand) {
                                when (VoiceCommandRouter.parse(recognized)) {
                                    VoiceCommand.READ -> readLatestNotification()
                                    VoiceCommand.LATER -> {
                                        awaitingNotificationCommand = false
                                        latestEvent = null
                                        voice.speak("Хорошо. Оставлю на потом.")
                                    }
                                    VoiceCommand.REPEAT -> lastPrompt?.let(::speakAndListen)
                                    VoiceCommand.UNKNOWN -> {
                                        speakAndListen("Не понял ответ. Скажите: прочитай, позже или повтори.")
                                    }
                                }
                            }
                        },
                        onError = { error ->
                            voiceError = error
                            awaitingNotificationCommand = false
                        },
                    )
                }

                DisposableEffect(Unit) {
                    val notificationListener: (NotificationEvent) -> Unit = { event ->
                        latestEvent = event
                        val sender = event.sender.ifBlank { event.appName }
                        val prompt = "Новое сообщение в ${event.appName} от $sender. Прочитать?"
                        if (microphoneGranted) speakAndListen(prompt) else voice.speak(prompt)
                    }
                    val callListener: (CallEvent) -> Unit = { event ->
                        latestCall = event
                        if (event.incoming) {
                            val spokenCaller = event.displayName
                                ?: event.phoneNumber.takeIf { it.isNotBlank() }
                                ?: "неизвестный номер"
                            voice.setSpeechRate(if (profile.speech.slowerSpeech) 0.78f else 1.0f)
                            voice.speak("Входящий звонок. $spokenCaller.")
                        }
                    }
                    NotificationEventStore.addListener(notificationListener)
                    CallEventStore.addListener(callListener)
                    onDispose {
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
                                store.save(completedProfile)
                                profile = completedProfile
                            },
                        )
                    } else {
                        HomeScreen(
                            profile = profile,
                            latestEvent = latestEvent,
                            latestCall = latestCall,
                            contactsGranted = contactsGranted,
                            voiceError = voiceError,
                            onEnableNotificationAccess = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            onRequestContacts = {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            onRequestCallScreeningRole = {
                                val roleManager = getSystemService(RoleManager::class.java)
                                if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true &&
                                    !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                                ) {
                                    roleLauncher.launch(
                                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                                    )
                                }
                            },
                            onReadNotification = { readLatestNotification() },
                            onLater = {
                                awaitingNotificationCommand = false
                                latestEvent = null
                            },
                            onSpeak = {
                                voice.setSpeechRate(if (profile.speech.slowerSpeech) 0.78f else 1.0f)
                                val greeting = if (profile.displayName.isBlank()) {
                                    "Здравствуйте. Я готов помочь."
                                } else {
                                    "Здравствуйте, ${profile.displayName}. Я готов помочь."
                                }
                                voice.speak(greeting)
                            },
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HomeScreen(
    profile: UserProfile,
    latestEvent: NotificationEvent?,
    latestCall: CallEvent?,
    contactsGranted: Boolean,
    voiceError: String?,
    onEnableNotificationAccess: () -> Unit,
    onRequestContacts: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onReadNotification: () -> Unit,
    onLater: () -> Unit,
    onSpeak: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Помощник готов", fontSize = 30.sp)
        Text("Профиль ${profile.displayName.ifBlank { "пользователя" }} активен.", fontSize = 20.sp)

        Button(
            onClick = onEnableNotificationAccess,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { Text("Разрешить доступ к уведомлениям", fontSize = 18.sp) }

        if (!contactsGranted) {
            Button(
                onClick = onRequestContacts,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) { Text("Разрешить доступ к контактам", fontSize = 18.sp) }
        }

        Button(
            onClick = onRequestCallScreeningRole,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { Text("Включить определитель звонков", fontSize = 18.sp) }

        latestCall?.let { call ->
            Text("Последний звонок", fontSize = 24.sp)
            Text(call.displayName ?: call.phoneNumber.ifBlank { "Неизвестный номер" }, fontSize = 20.sp)
        }

        latestEvent?.let { event ->
            Text("Новое уведомление", fontSize = 24.sp)
            Text("${event.appName}: ${event.sender}", fontSize = 20.sp)
            if (event.text.isNotBlank()) Text(event.text, fontSize = 18.sp)
            Button(onClick = onReadNotification, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text("Прочитать", fontSize = 20.sp)
            }
            Button(onClick = onLater, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text("Позже", fontSize = 20.sp)
            }
        }

        Button(onClick = onSpeak, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("Проверить голос", fontSize = 20.sp)
        }
        voiceError?.let { Text(it) }
    }
}
