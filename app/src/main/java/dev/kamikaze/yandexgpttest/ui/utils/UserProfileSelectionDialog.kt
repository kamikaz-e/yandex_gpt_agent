package dev.kamikaze.yandexgpttest.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kamikaze.yandexgpttest.data.UserProfile
import dev.kamikaze.yandexgpttest.data.UserProfiles

@Composable
fun UserProfileSelectionDialog(
    currentProfile: UserProfile?,
    onProfileSelected: (UserProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    val profiles = UserProfiles.getAllProfiles()
    var selectedProfile by remember { mutableStateOf(currentProfile ?: UserProfiles.getDefaultProfile()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Выберите профиль",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Выберите, кто вы, чтобы агент мог персонализировать общение:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                profiles.forEach { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = selectedProfile.id == profile.id,
                        onSelected = { selectedProfile = profile }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onProfileSelected(selectedProfile) },
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(
                    text = "Продолжить",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors()
            ) {
                Text(
                    text = "Отмена",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = profile.role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = profile.description.take(100) + if (profile.description.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
