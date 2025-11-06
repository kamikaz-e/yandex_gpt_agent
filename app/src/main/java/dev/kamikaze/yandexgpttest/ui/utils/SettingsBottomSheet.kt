package dev.kamikaze.yandexgpttest.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kamikaze.yandexgpttest.data.prompt.ResponseFormat
import dev.kamikaze.yandexgpttest.data.prompt.ResponseStyle
import dev.kamikaze.yandexgpttest.ui.AISettings

@Composable
fun SettingsBottomSheet(
    currentSettings: AISettings,
    onSettingsChanged: (AISettings) -> Unit,
    onClose: () -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(currentSettings.responseFormat) }
    var selectedStyle by remember { mutableStateOf(currentSettings.responseStyle) }
    var maxLength by remember { mutableFloatStateOf(currentSettings.maxLength) }

    fun saveSettings() {
        val newSettings = AISettings(
            responseFormat = selectedFormat,
            responseStyle = selectedStyle,
            maxLength = maxLength
        )
        onSettingsChanged(newSettings)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f) // Еще меньше
    ) {
        // Минималистичный заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(18.dp))
            }
        }

        HorizontalDivider()

        // Минимальные отступы
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ультра компактные селекторы
            UltraCompactDropdown(
                "Формат", selectedFormat.displayName,
                ResponseFormat.entries
            ) { format ->
                selectedFormat = format
                saveSettings()
            }

            UltraCompactDropdown(
                "Стиль", selectedStyle.displayName,
                ResponseStyle.entries
            ) { style ->
                selectedStyle = style
                saveSettings()
            }

            UltraCompactSlider("Длина: $maxLength", maxLength, 100f..2000f, 18) { length ->
                maxLength = length
                saveSettings()
            }
        }
    }
}

@Composable
fun <T> UltraCompactDropdown(
    label: String,
    value: String,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    if (expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp)
                )
                .padding(8.dp)
        ) {
            options.forEach { option ->
                Text(
                    text = if (option is ResponseFormat) option.displayName
                    else if (option is ResponseStyle) option.displayName
                    else option.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOptionSelected(option)
                            expanded = false
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun UltraCompactSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChanged: (Float) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChanged,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors()
        )
    }
}