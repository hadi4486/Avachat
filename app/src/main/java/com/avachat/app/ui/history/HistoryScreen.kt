package com.avachat.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Delete as DeleteSweepIcon
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star as PushPinIcon
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.avachat.app.R
import com.avachat.app.core.domain.model.Conversation
import com.avachat.app.core.presentation.history.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conversations)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDeleteAll = true }) {
                        Icon(
                            Icons.Filled.DeleteSweepIcon,
                            contentDescription = stringResource(R.string.clear_conversations)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::search,
                placeholder = { Text(stringResource(R.string.search_history)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            if (state.conversations.isEmpty() && !state.loading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.conversations),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.conversations, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = { onOpenConversation(conversation.id) },
                            onRename = { renameTarget = conversation },
                            onDelete = { deleteTarget = conversation },
                            onTogglePin = {
                                viewModel.togglePin(conversation.id, conversation.pinned)
                            },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(conversation.id, conversation.favorite)
                            }
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        var newTitle by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename_title)) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rename(target.id, newTitle)
                    renameTarget = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_conversation_title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.delete_all_conversations_title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    confirmDeleteAll = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conversation.pinned) {
                Icon(
                    Icons.Filled.PushPinIcon,
                    contentDescription = stringResource(R.string.pin),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = conversation.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                        .format(Date(conversation.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (conversation.favorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.favorite),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            IconButton(onClick = onTogglePin, modifier = Modifier.padding(2.dp)) {
                Icon(
                    Icons.Filled.PushPinIcon,
                    contentDescription = stringResource(
                        if (conversation.pinned) R.string.unpin else R.string.pin
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.padding(2.dp)) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(
                        if (conversation.favorite) R.string.unfavorite else R.string.favorite
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRename, modifier = Modifier.padding(2.dp)) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.rename),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.padding(2.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
