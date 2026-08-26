package ru.apphelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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

                val voice = remember {
                    AndroidVoiceAssistant(
                        context = this,
                        onRecognized = { },
                        onError = { voiceError = it },
                    )
                }

                DisposableEffect(Unit) {
                    onDispose { voice.release() }
                }

                val microphoneLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    microphoneGranted = granted
                }

                Surface(color = Color(0xFF0E1116)) {
                    if (!profile.onboardingCompleted) {
                        OnboardingFlow(
                            initialProfile = profile,
                            onRequestMicrophone = {
                                microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            microphoneGranted = microphoneGranted,
                            onTestVoice = { text ->
                                voice.setSpeechRate(if (profile.speech.slowerSpeech) 0.78f else 1.0f)
                                voice.speak(text)
                            },
                            onFinish = { completed ->
                                profile = completed.copy(
                                    permissions = completed.permissions.copy(
                                        microphoneGranted = microphoneGranted,
                                    ),
                                )
                                store.save(profile)
                            },
                        )
                    } else {
                        HomePlaceholder(
                            profile = profile,
                            voiceError = voiceError,
                            onSpeak = {
                                voice.setSpeechRate(if (profile.speech.slowerSpeech) 0.78f else 1.0f)
                                voice.speak(
                                    "Здравствуйте, ${profile.displayName.ifBlank { "я готов помочь" }}. Скажите, что нужно сделать.",
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HomePlaceholder(
    profile: UserProfile,
    voiceError: String?,
    onSpeak: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Помощник готов", fontSize = 30.sp)
        Text(
            "Профиль ${profile.displayName.ifBlank { "пользователя" }} сохранён. Следующий этап — сообщения, звонки и контакты.",
            fontSize = 20.sp,
        )
        Button(onClick = onSpeak) {
            Text("Проверить голос", fontSize = 20.sp)
        }
        voiceError?.let { Text(it) }
    }
}
