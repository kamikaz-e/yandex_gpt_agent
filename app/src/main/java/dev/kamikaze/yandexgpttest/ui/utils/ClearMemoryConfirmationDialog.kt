package dev.kamikaze.yandexgpttest.ui.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kamikaze.yandexgpttest.data.StorageInfo

@Composable
fun ClearMemoryConfirmationDialog(
    storageInfo: StorageInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Очистить память?",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = buildString {
                    appendLine("Вы уверены, что хотите очистить долговременную память?")
                    appendLine()
                    append("Будут удалены:")
                    append(" ${storageInfo.getFormattedSize()} данных")
                    append(" (сохранено ${storageInfo.getFormattedDate()})")
                    appendLine()
                    appendLine()
                    append("Это действие нельзя отменить.")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "Очистить",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
    )
}