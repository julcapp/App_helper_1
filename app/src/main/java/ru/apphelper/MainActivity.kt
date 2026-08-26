package ru.apphelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import ru.apphelper.domain.AssistanceMode
import ru.apphelper.ui.onboarding.AppHelperTheme
import ru.apphelper.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppHelperTheme {
                var modes by remember {
                    mutableStateOf(setOf(AssistanceMode.VOICE_FIRST))
                }

                Surface(color = Color(0xFF0E1116)) {
                    OnboardingScreen(
                        selectedModes = modes,
                        onModeToggle = { mode ->
                            modes = if (mode in modes) modes - mode else modes + mode
                        },
                        onContinue = {
                            // Следующий этап: настройки речи и сохранение UserProfile.
                        },
                    )
                }
            }
        }
    }
}
