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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.apphelper.domain.AssistanceMode

private val Background = Color(0xFF0E1116)
private val Surface = Color(0xFF1A1F27)
private val Accent = Color(0xFF4DCC94)
private val MainText = Color(0xFFF8FAFF)
private val SecondaryText = Color(0xFFB7BDC8)

@Composable
fun OnboardingScreen(
    selectedModes: Set<AssistanceMode>,
    onModeToggle: (AssistanceMode) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Настроим помощника",
            color = MainText,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Выберите один или несколько вариантов. Всё можно изменить позже голосом.",
            color = SecondaryText,
            fontSize = 19.sp,
            lineHeight = 27.sp,
        )

        AssistanceCard(
            title = "Мне трудно видеть экран",
            description = "Крупные элементы, подробное озвучивание и управление голосом.",
            selected = AssistanceMode.LOW_VISION in selectedModes,
            accessibilityLabel = "Режим помощи при слабом зрении",
            onClick = { onModeToggle(AssistanceMode.LOW_VISION) },
        )
        AssistanceCard(
            title = "Мне сложно пользоваться телефоном",
            description = "Простой режим: один вопрос — одно действие и больше времени на ответ.",
            selected = AssistanceMode.SIMPLE_PHONE in selectedModes,
            accessibilityLabel = "Простой режим телефона",
            onClick = { onModeToggle(AssistanceMode.SIMPLE_PHONE) },
        )
        AssistanceCard(
            title = "Я предпочитаю голос",
            description = "Обычный интерфейс плюс быстрые голосовые команды и AI-помощник.",
            selected = AssistanceMode.VOICE_FIRST in selectedModes,
            accessibilityLabel = "Режим голосового управления",
            onClick = { onModeToggle(AssistanceMode.VOICE_FIRST) },
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onContinue,
            enabled = selectedModes.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = "Продолжить настройку помощника" },
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color(0xFF08130E),
            ),
        ) {
            Text("Продолжить", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AssistanceCard(
    title: String,
    description: String,
    selected: Boolean,
    accessibilityLabel: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$accessibilityLabel. ${if (selected) "Выбран" else "Не выбран"}"
            },
        colors = CardDefaults.cardColors(containerColor = Surface),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                color = MainText,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                color = SecondaryText,
                fontSize = 18.sp,
                lineHeight = 26.sp,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (selected) "Выбрано" else "Выбрать",
                    color = if (selected) Accent else MainText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun AppHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
