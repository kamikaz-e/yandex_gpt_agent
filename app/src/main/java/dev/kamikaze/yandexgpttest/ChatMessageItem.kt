package dev.kamikaze.yandexgpttest

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kamikaze.yandexgpttest.data.UserMessage

@Composable
fun ChatMessageItem(
    userMessage: UserMessage,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (userMessage.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (userMessage.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (userMessage.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Text(
                text = userMessage.text,
                color = textColor,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}