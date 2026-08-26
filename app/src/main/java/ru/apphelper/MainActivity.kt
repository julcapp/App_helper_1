package ru.apphelper

import android.Manifest
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
import ru.apphelper.data.UserProfileStore
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
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED,
                    )
                }
                var voiceError by remember { mutableStateOf<String?>(null) }
                var latestEvent by remember { mutableStateOf<NotificationEvent?>(NotificationEventStore.snapshot().firstOrNull()) }

                val voice = remember {
                    AndroidVoiceAssistant(
                        context = this,
                        onRecognized = { },
                        onError = { voiceError = it },
                    )
                }

                DisposableEffect(Unit) {
                    val listener: (NotificationEvent) -> Unit = { event ->
                        latestEvent = event
                        val sender = event.sender.ifBlank { event.appName }
                        voice.setSpeechRate(if (profile.speech.slowerSpeech) 0.78f else 1.0f)
                        voice.speak("Новое сообщение в ${event.appName} от $sender. Прочитать?")
                    }
                    NotificationEventStore.addListener(listener)
                    onDispose {
                        NotificationEventStore.removeListener(listener)
                        voice.release()
                    }
                }

                val microphoneLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> microphoneGranted = granted }

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
                            voiceError = voiceError,
                            onEnableNotificationAccess = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            onReadNotification = {
                                latestEvent?.let { event ->
                                    val spoken = if (event.text.isBlank()) {
                                        "В уведомлении нет доступного текста."
                                    } else {
                                        "${event.sender}. ${event.text}"
                                    }
                                    voice.speak(spoken)
                                }
                            },
                            onLater = { latestEvent = null },
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
    voiceError: String?,
    onEnableNotificationAccess: () -> Unit,
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
        ) {
            Text("Разрешить доступ к уведомлениям", fontSize = 18.sp)
        }

        latestEvent?.let { event ->
            Text("Новое уведомление", fontSize = 24.sp)
            Text("${event.appName}: ${event.sender}", fontSize = 20.sp)
            if (event.text.isNotBlank()) Text(event.text, fontSize = 18.sp)
            Button(
                onClick = onReadNotification,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) { Text("Прочитать", fontSize = 20.sp) }
            Button(
                onClick = onLater,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) { Text("Позже", fontSize = 20.sp) }
        }

        Button(onClick = onSpeak, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("Проверить голос", fontSize = 20.sp)
        }
        voiceError?.let { Text(it) }
    }
}
