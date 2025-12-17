package dev.kamikaze.yandexgpttest.ui.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.kamikaze.yandexgpttest.data.ApiEnvironment
import dev.kamikaze.yandexgpttest.data.ApiSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsDialog(
    currentSettings: ApiSettings,
    onDismiss: () -> Unit,
    onConfirm: (ApiSettings) -> Unit
) {
    var selectedEnvironment by remember { mutableStateOf(currentSettings.environment) }
    var localLlmUrl by remember { mutableStateOf(currentSettings.localLlmUrl) }
    var localLlmModel by remember { mutableStateOf(currentSettings.localLlmModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "API Settings"
            )
        },
        title = {
            Text("Настройки API")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Выберите окружение API:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Выбор окружения
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ApiEnvironment.values().forEach { environment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            RadioButton(
                                selected = selectedEnvironment == environment,
                                onClick = { selectedEnvironment = environment }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = environment.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                            )
                        }
                    }
                }

                // Настройки локальной LLM (показываются только если выбран LOCAL_LLM)
                if (selectedEnvironment == ApiEnvironment.LOCAL_LLM) {
                    HorizontalDivider()

                    Text(
                        "Настройки локальной LLM:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = localLlmUrl,
                        onValueChange = { localLlmUrl = it },
                        label = { Text("URL сервера") },
                        placeholder = { Text("http://10.0.2.2:11434") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        )
                    )

                    OutlinedTextField(
                        value = localLlmModel,
                        onValueChange = { localLlmModel = it },
                        label = { Text("Модель") },
                        placeholder = { Text("qwen2.5:14b") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        "Примеры моделей: llama3.2:3b, qwen2.5:14b, mistral:7b",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ApiSettings(
                            environment = selectedEnvironment,
                            localLlmUrl = localLlmUrl.trim(),
                            localLlmModel = localLlmModel.trim()
                        )
                    )
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
