package ru.apphelper.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.apphelper.domain.AssistanceMode
import ru.apphelper.domain.SpeechPreferences
import ru.apphelper.domain.TrustedContact
import ru.apphelper.domain.UserPermissions
import ru.apphelper.domain.UserProfile

private val interestOptions = listOf("История", "Путешествия", "Испания", "Музыка", "Спорт", "Технологии")

@Composable
fun OnboardingFlow(
    initialProfile: UserProfile,
    onRequestMicrophone: () -> Unit,
    microphoneGranted: Boolean,
    onTestVoice: (String) -> Unit,
    onFinish: (UserProfile) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var modes by remember { mutableStateOf(initialProfile.assistanceModes) }
    var displayName by remember { mutableStateOf(initialProfile.displayName) }
    var slowerSpeech by remember { mutableStateOf(initialProfile.speech.slowerSpeech) }
    var waitLonger by remember { mutableStateOf(initialProfile.speech.waitLongerForAnswer) }
    var interests by remember { mutableStateOf(initialProfile.interests) }
    var trustedName by remember { mutableStateOf(initialProfile.trustedContact?.name.orEmpty()) }
    var trustedPhone by remember { mutableStateOf(initialProfile.trustedContact?.phone.orEmpty()) }

    val profile = UserProfile(
        displayName = displayName.trim(),
        assistanceModes = modes.ifEmpty { setOf(AssistanceMode.VOICE_FIRST) },
        speech = SpeechPreferences(
            slowerSpeech = slowerSpeech,
            shortPhrases = true,
            waitLongerForAnswer = waitLonger,
            repeatImportantQuestions = true,
        ),
        permissions = UserPermissions(microphoneGranted = microphoneGranted),
        interests = interests,
        trustedContact = if (trustedName.isNotBlank() || trustedPhone.isNotBlank()) {
            TrustedContact(trustedName.trim(), trustedPhone.trim())
        } else null,
        primaryLanguage = "ru",
        travelLanguages = setOf("es"),
        onboardingCompleted = step >= 6,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Настройка помощника", fontSize = 30.sp)
        Text("Шаг ${step + 1} из 7", fontSize = 18.sp)

        when (step) {
            0 -> {
                Text("Как вам удобнее пользоваться телефоном?", fontSize = 24.sp)
                ModeRow("Мне трудно видеть экран", AssistanceMode.LOW_VISION, modes) { modes = toggle(modes, it) }
                ModeRow("Мне сложно пользоваться телефоном", AssistanceMode.SIMPLE_PHONE, modes) { modes = toggle(modes, it) }
                ModeRow("Я предпочитаю голосовое управление", AssistanceMode.VOICE_FIRST, modes) { modes = toggle(modes, it) }
            }
            1 -> {
                Text("Как помощник должен говорить?", fontSize = 24.sp)
                ToggleRow("Говорить медленнее", slowerSpeech) { slowerSpeech = it }
                ToggleRow("Давать больше времени на ответ", waitLonger) { waitLonger = it }
                Text("Важные действия помощник будет подтверждать отдельно.")
            }
            2 -> {
                Text("Как к вам обращаться?", fontSize = 24.sp)
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            3 -> {
                Text("Какие темы вам интересны?", fontSize = 24.sp)
                interestOptions.forEach { item ->
                    ToggleRow(item, item in interests) { checked ->
                        interests = if (checked) interests + item else interests - item
                    }
                }
            }
            4 -> {
                Text("Доверенный контакт", fontSize = 24.sp)
                Text("Этот человек пригодится для будущих сценариев помощи и передачи геопозиции только после разрешения пользователя.")
                OutlinedTextField(trustedName, { trustedName = it }, label = { Text("Имя контакта") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(trustedPhone, { trustedPhone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
            }
            5 -> {
                Text("Разрешение на микрофон", fontSize = 24.sp)
                Text(if (microphoneGranted) "Микрофон разрешён." else "Микрофон нужен только тогда, когда помощник слушает вашу голосовую команду.")
                if (!microphoneGranted) {
                    Button(onClick = onRequestMicrophone, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        Text("Разрешить микрофон", fontSize = 20.sp)
                    }
                }
            }
            6 -> {
                Text("Проверка голоса", fontSize = 24.sp)
                Text("Нажмите кнопку. Помощник произнесёт тестовую фразу с выбранным темпом речи.")
                Button(
                    onClick = {
                        val name = displayName.ifBlank { "пользователь" }
                        onTestVoice("Здравствуйте, $name. Голосовой помощник готов к работе.")
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Text("Проверить голос", fontSize = 20.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("Основной язык: русский. Для поездок подготовлен испанский язык.")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) {
                Button(onClick = { step-- }, modifier = Modifier.weight(1f).height(60.dp)) { Text("Назад") }
            }
            if (step < 6) {
                Button(onClick = { step++ }, modifier = Modifier.weight(1f).height(60.dp)) { Text("Дальше") }
            } else {
                Button(
                    onClick = { onFinish(profile.copy(onboardingCompleted = true)) },
                    modifier = Modifier.weight(1f).height(60.dp),
                ) { Text("Завершить") }
            }
        }
    }
}

@Composable
private fun ModeRow(label: String, mode: AssistanceMode, selected: Set<AssistanceMode>, onToggle: (AssistanceMode) -> Unit) {
    val checked = mode in selected
    Row(modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }) {
        Checkbox(checked = checked, onCheckedChange = { onToggle(mode) })
        Text(label, fontSize = 20.sp, modifier = Modifier.padding(top = 11.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, fontSize = 20.sp, modifier = Modifier.padding(top = 11.dp))
    }
}

private fun toggle(current: Set<AssistanceMode>, mode: AssistanceMode): Set<AssistanceMode> =
    if (mode in current) current - mode else current + mode
