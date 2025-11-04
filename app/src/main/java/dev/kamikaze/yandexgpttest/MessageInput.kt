package dev.kamikaze.yandexgpttest

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            placeholder = { Text("Введите сообщение...") },
            modifier = Modifier
                .weight(1f),
            enabled = !isLoading,
            singleLine = false,
            maxLines = 3
        )
        
        Button(
            onClick = {
                if (messageText.isNotBlank()) {
                    onSendMessage(messageText)
                    messageText = ""
                }
            },
            enabled = messageText.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Отправить")
            }
        }
    }
}