package ru.apphelper.ui.accessibility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Touch target is larger than Android's 48dp minimum to accommodate tremor and low vision.
 * The button remains usable with large font scales and TalkBack.
 */
@Composable
fun AccessibleAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    accessibilityLabel: String = label,
) {
    val shape = RoundedCornerShape(18.dp)
    val base = modifier.fillMaxWidth().heightIn(min = if (primary) 88.dp else 72.dp)
        .semantics { contentDescription = accessibilityLabel }
    if (primary) {
        Button(onClick = onClick, modifier = base, shape = shape) {
            Text(label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = base, shape = shape) {
            Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

/** A semantic heading is navigable through TalkBack's heading controls. */
@Composable
fun AccessibleSection(title: String, description: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { androidx.compose.ui.semantics.heading() })
        if (description != null) Text(description, style = MaterialTheme.typography.bodyLarge)
    }
}
