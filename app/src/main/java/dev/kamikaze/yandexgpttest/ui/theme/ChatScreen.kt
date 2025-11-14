import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.kamikaze.yandexgpttest.ChatViewModel
import dev.kamikaze.yandexgpttest.data.CompactionStats
import dev.kamikaze.yandexgpttest.ui.UserMessage
import dev.kamikaze.yandexgpttest.ui.utils.DeleteConfirmationDialog
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val totalTokenStats by viewModel.totalTokenStats.collectAsState()  // ← Добавляем
    val compactionConfig by viewModel.compactionConfig.collectAsState()
    val compactionStats by viewModel.compactionStats.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header с статистикой токенов
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yandex AI",
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(
                        onClick = { viewModel.showDeleteConfirmationDialog() },
                        enabled = !isLoading && messages.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить чат"
                        )
                    }
                }

                // Статистика токенов
                if (totalTokenStats.totalTokens > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TokenStatisticsCard(
                        inputTokens = totalTokenStats.inputTokens,
                        outputTokens = totalTokenStats.outputTokens,
                        totalTokens = totalTokenStats.totalTokens
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                CompactionToggleCard(
                    compactionEnabled = compactionConfig.enabled,
                    compactionStats = compactionStats,
                    messagesCount = messages.count { !it.isSummary },
                    onToggleCompaction = { viewModel.toggleCompaction() }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = lazyListState,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                if (message.isUser) {
                    RegularChatMessageItem(userMessage = message)
                } else {
                    AIDisplayMessage(userMessage = message)
                }
            }

            if (isLoading) {
                item {
                    LoadingMessageItem()
                }
            }
        }

        MessageInput(
            isLoading = isLoading,
            onSendMessage = { message ->
                viewModel.sendMessage(message)
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = { viewModel.confirmDeleteChat() },
            onDismiss = { viewModel.cancelDeleteChat() }
        )
    }
}

// ← НОВЫЙ КОМПОНЕНТ: Карточка статистики токенов
@Composable
fun TokenStatisticsCard(
    inputTokens: Int,
    outputTokens: Int,
    totalTokens: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TokenStatItem(
                label = "Вход",
                value = inputTokens,
                modifier = Modifier.weight(1f)
            )
            TokenStatItem(
                label = "Выход",
                value = outputTokens,
                modifier = Modifier.weight(1f)
            )
            TokenStatItem(
                label = "Всего",
                value = totalTokens,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TokenStatItem(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoadingMessageItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Yandex AI анализирует...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun RegularChatMessageItem(
    userMessage: UserMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            ),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 2.dp
        ) {
            Text(
                text = userMessage.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
@Composable
fun AIDisplayMessage(
    userMessage: UserMessage,
    modifier: Modifier = Modifier,
) {
    // ← Не показываем summary если компрессия выключена
    if (userMessage.isSummary) {
        // Summary-сообщения видны только в режиме компрессии
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp
        ) {
            Column {
                ChatMessage(
                    text = userMessage.text,
                    modifier = Modifier.padding(16.dp)
                )

                userMessage.tokens?.let { tokens ->
                    if (tokens.totalTokens > 0) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📊 Токены:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "вход: ${tokens.inputTokens}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "выход: ${tokens.outputTokens}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "всего: ${tokens.totalTokens}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
) {
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            messageText = ""
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = {
                    Text(
                        text = "Введите сообщение...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText.trim())
                        }
                    }
                )
            )

            IconButton(
                onClick = {
                    if (messageText.isNotBlank() && !isLoading) {
                        onSendMessage(messageText.trim())
                        messageText = ""
                    }
                },
                enabled = messageText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .background(
                        color = if (messageText.isNotBlank() && !isLoading)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = if (messageText.isNotBlank() && !isLoading)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Добавьте в ChatScreen.kt

@Composable
fun CompactionStatisticsCard(
    compactionStats: CompactionStats,
    compactionEnabled: Boolean,
    onToggleCompaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (compactionEnabled)
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp,
            if (compactionEnabled)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗜️ Компрессия",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (compactionEnabled)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.outline
                    ) {
                        Text(
                            text = if (compactionEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                androidx.compose.material3.Switch(
                    checked = compactionEnabled,
                    onCheckedChange = { onToggleCompaction() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactionStatItem(
                    label = "Сжато",
                    value = "${compactionStats.originalMessages} → ${compactionStats.compressedMessages}",
                    modifier = Modifier.weight(1f)
                )
                CompactionStatItem(
                    label = "Экономия",
                    value = "${compactionStats.tokensSaved} 🪙",
                    modifier = Modifier.weight(1f),
                    highlight = true
                )
            }
        }
    }
}

@Composable
fun CompactionStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompactionToggleCard(
    compactionEnabled: Boolean,
    compactionStats: CompactionStats,
    messagesCount: Int,
    onToggleCompaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (compactionEnabled)
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            width = if (compactionEnabled) 2.dp else 1.dp,
            color = if (compactionEnabled)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок с переключателем
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🗜️",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Компрессия диалога",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (compactionEnabled)
                            "Каждые 10 сообщений → Summary"
                        else
                            "Все сообщения сохраняются",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    androidx.compose.material3.Switch(
                        checked = compactionEnabled,
                        onCheckedChange = { onToggleCompaction() },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                            checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (compactionEnabled)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.outline
                    ) {
                        Text(
                            text = if (compactionEnabled) "ВКЛЮЧЕНО" else "ВЫКЛЮЧЕНО",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Статистика (показываем только если есть данные)
            if (compactionEnabled && (compactionStats.originalMessages > 0 || messagesCount > 0)) {
                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.material3.Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Текущие сообщения
                    CompactionStatItem(
                        label = "Сообщений",
                        value = "$messagesCount",
                        modifier = Modifier.weight(1f),
                        icon = "💬"
                    )

                    // Сжато сообщений
                    CompactionStatItem(
                        label = "Сжато",
                        value = "${compactionStats.originalMessages} → ${compactionStats.compressedMessages}",
                        modifier = Modifier.weight(1f),
                        icon = "📦"
                    )

                    // Экономия токенов
                    CompactionStatItem(
                        label = "Экономия",
                        value = "${compactionStats.tokensSaved}",
                        modifier = Modifier.weight(1f),
                        highlight = true,
                        icon = "💰"
                    )
                }

                // Предупреждение о компрессии
                if (messagesCount >= 8 && messagesCount < 10) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Компрессия через ${10 - messagesCount} сообщений",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Подсказка когда выключено
            if (!compactionEnabled && messagesCount > 10) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Column {
                            Text(
                                text = "Большой контекст: $messagesCount сообщений",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Включите компрессию для экономии токенов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactionStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    icon: String = ""
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon.isNotEmpty()) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}