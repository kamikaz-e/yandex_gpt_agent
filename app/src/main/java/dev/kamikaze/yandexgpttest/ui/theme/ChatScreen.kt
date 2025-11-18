package dev.kamikaze.yandexgpttest.ui.theme

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction.Companion.Send
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.kamikaze.yandexgpttest.ChatViewModel
import dev.kamikaze.yandexgpttest.data.StorageInfo
import dev.kamikaze.yandexgpttest.mcp.Tool
import dev.kamikaze.yandexgpttest.ui.UserMessage
import dev.kamikaze.yandexgpttest.ui.utils.ClearMemoryConfirmationDialog
import dev.kamikaze.yandexgpttest.ui.utils.DeleteConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showClearMemoryDialog by viewModel.showClearMemoryDialog.collectAsState() // ← НОВОЕ
    val totalTokenStats by viewModel.totalTokenStats.collectAsState()
    val compactionConfig by viewModel.compactionConfig.collectAsState()
    val compactionStats by viewModel.compactionStats.collectAsState()
    val hasSavedData by viewModel.hasSavedData.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val isLoadingFromMemory by viewModel.isLoadingFromMemory.collectAsState()
    val mcpTools by viewModel.mcpTools.collectAsState()
    val isLoadingMcpTools by viewModel.isLoadingMcpTools.collectAsState()
    val mcpStatus by viewModel.mcpStatus.collectAsState()
    val showMcpToolsDialog by viewModel.showMcpToolsDialog.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yandex AI",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Компактная статистика токенов
                        if (totalTokenStats.totalTokens > 0) {
                            CompactTokenStats(totalTokens = totalTokenStats.totalTokens)
                        }

                        // Кнопка очистки памяти (только если есть данные)
                        if (hasSavedData) {
                            IconButton(
                                onClick = { viewModel.showClearMemoryDialog() },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить память",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.showDeleteConfirmationDialog() },
                            enabled = !isLoading && messages.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить чат",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Компактный блок настроек
                CompactSettingsPanel(
                    compactionConfig = compactionConfig,
                    compactionStats = compactionStats,
                    hasSavedData = hasSavedData,
                    storageInfo = storageInfo,
                    isLoadingFromMemory = isLoadingFromMemory,
                    messagesCount = messages.count { !it.isSummary },
                    onToggleCompaction = { viewModel.toggleCompaction() }
                )
            }
        }

        // Индикатор загрузки из памяти
        AnimatedVisibility(
            visible = isLoadingFromMemory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Загрузка памяти...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
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
            },
            // ← НОВЫЕ параметры для MCP
            mcpStatus = mcpStatus,
            isLoadingMcpTools = isLoadingMcpTools,
            onLoadMcpTools = { viewModel.loadMcpTools() }
        )

        // Диалог с MCP инструментами
        if (showMcpToolsDialog) {
            McpToolsDialog(
                tools = mcpTools,
                onDismiss = { viewModel.hideMcpToolsDialog() }
            )
        }
    }

    // Диалоги
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = { viewModel.confirmDeleteChat() },
            onDismiss = { viewModel.cancelDeleteChat() }
        )
    }

    if (showClearMemoryDialog) {
        ClearMemoryConfirmationDialog(
            storageInfo = storageInfo,
            onConfirm = { viewModel.confirmClearMemory() },
            onDismiss = { viewModel.cancelClearMemory() }
        )
    }
}

// Добавляем в ChatScreen.kt

@Composable
fun McpToolsButton(
    mcpStatus: String,
    isLoadingMcpTools: Boolean,
    onLoadMcpTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔧",
                    style = MaterialTheme.typography.titleMedium
                )
                Column {
                    Text(
                        text = "MCP Инструменты",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = mcpStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isLoadingMcpTools) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                androidx.compose.material3.Button(
                    onClick = onLoadMcpTools,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Загрузить",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun McpToolsDialog(
    tools: List<Tool>,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🔧 MCP Инструменты (${tools.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(tools) { tool ->
                    McpToolItem(tool = tool)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun McpToolItem(
    tool: Tool,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (!tool.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "Тип: ${tool.inputSchema.type}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ← НОВЫЙ КОМПОНЕНТ: Компактная статистика токенов
@Composable
fun CompactTokenStats(
    totalTokens: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Text(
            text = "$totalTokens 🔢",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ← УПРОЩЕННЫЙ КОМПОНЕНТ: Компактная панель настроек
@Composable
fun CompactSettingsPanel(
    compactionConfig: dev.kamikaze.yandexgpttest.data.CompactionConfig,
    compactionStats: dev.kamikaze.yandexgpttest.data.CompactionStats,
    hasSavedData: Boolean,
    storageInfo: StorageInfo,
    isLoadingFromMemory: Boolean,
    messagesCount: Int,
    onToggleCompaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Свернутый вид - компактная строка статуса
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Статус компрессии и памяти
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Статус компрессии
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (compactionConfig.enabled)
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (compactionConfig.enabled) "🗜️ Вкл" else "🗜️ Выкл",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Статус памяти
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hasSavedData)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = if (hasSavedData) "💾 Сохранено" else  "💾 Пусто",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Количество сообщений (если есть)
                if (messagesCount > 0) {
                    Text(
                        text = "$messagesCount сообщений",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Кнопка раскрытия настроек
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Развернутый вид настроек
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                androidx.compose.material3.Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Настройки компрессии
                CompactCompactionSection(
                    compactionConfig = compactionConfig,
                    compactionStats = compactionStats,
                    messagesCount = messagesCount,
                    onToggleCompaction = onToggleCompaction
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Информация о памяти (только просмотр)
                CompactMemoryInfoSection(
                    hasSavedData = hasSavedData,
                    storageInfo = storageInfo,
                    messagesCount = messagesCount
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ← УПРОЩЕННАЯ СЕКЦИЯ: Компрессия (без лишних кнопок)
@Composable
fun CompactCompactionSection(
    compactionConfig: dev.kamikaze.yandexgpttest.data.CompactionConfig,
    compactionStats: dev.kamikaze.yandexgpttest.data.CompactionStats,
    messagesCount: Int,
    onToggleCompaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (compactionConfig.enabled)
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            0.5.dp,
            if (compactionConfig.enabled)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Заголовок с переключателем
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
                        text = "🗜️ Компрессия диалога",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (compactionConfig.enabled) {
                        Text(
                            text = "${compactionConfig.messagesThreshold} сообщений",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Switch(
                    checked = compactionConfig.enabled,
                    onCheckedChange = { onToggleCompaction() },
                    modifier = Modifier.height(32.dp)
                )
            }

            // Описание работы компрессии
            Text(
                text = if (compactionConfig.enabled)
                    "Каждые 10 сообщений создается краткое резюме вместо полной истории"
                else
                    "Все сообщения сохраняются в полном виде",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Статистика (если включена)
            if (compactionConfig.enabled && compactionStats.originalMessages > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactStatItem(
                        label = "Сжато",
                        value = "${compactionStats.originalMessages}",
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatItem(
                        label = "Сводок",
                        value = "${compactionStats.compressedMessages}",
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatItem(
                        label = "Экономия токенов",
                        value = "${compactionStats.tokensSaved}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ← НОВАЯ СЕКЦИЯ: Информация о памяти (только просмотр)
@Composable
fun CompactMemoryInfoSection(
    hasSavedData: Boolean,
    storageInfo: StorageInfo,
    messagesCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (hasSavedData)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            0.5.dp,
            if (hasSavedData)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Заголовок
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💾 Долговременная память",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Информация о состоянии памяти
            if (hasSavedData) {
                // Статус: данные сохранены
                Text(
                    text = "Данные сохранены локально и загрузятся при следующем запуске",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Статистика сохраненных данных
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactStatItem(
                        label = "Размер",
                        value = storageInfo.getFormattedSize(),
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatItem(
                        label = "Сохранено",
                        value = storageInfo.getFormattedDate(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Предупреждение об очистке
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Для очистки памяти нажмите кнопку X в header",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else if (messagesCount > 0) {
                // Статус: данные будут сохранены автоматически
                Text(
                    text = "Диалог будет сохранен автоматически после завершения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Статус: память пуста
                Text(
                    text = "Память пуста. Начните диалог для автоматического сохранения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompactStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Остальные компоненты остаются без изменений...

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
    if (userMessage.isSummary) {
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
    // ← НОВЫЕ параметры для MCP
    mcpStatus: String,
    isLoadingMcpTools: Boolean,
    onLoadMcpTools: () -> Unit
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
        // ← НОВАЯ кнопка MCP над полем ввода
        McpToolsButton(
            mcpStatus = mcpStatus,
            isLoadingMcpTools = isLoadingMcpTools,
            onLoadMcpTools = onLoadMcpTools
        )

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
                    imeAction = Send,
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