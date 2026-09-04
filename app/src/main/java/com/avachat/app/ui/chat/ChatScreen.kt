package com.avachat.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Create as ContentCopyIcon
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle as HistoryIcon
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check as StopIcon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.avachat.app.R
import com.avachat.app.core.domain.model.Message
import com.avachat.app.core.domain.model.Role
import com.avachat.app.core.presentation.chat.ChatUiState
import com.avachat.app.core.presentation.chat.ChatViewModel
import com.avachat.app.ui.components.TypingIndicator
import com.avachat.app.ui.markdown.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedText = stringResource(R.string.copied)

    // Keep composer text here so it survives process-level re-creates of the
    // composable; the pending text in the state stays authoritative offline.
    var composerText by rememberSaveable(state.pendingUserText) { mutableStateOf(state.pendingUserText) }
    LaunchedEffect(state.pendingUserText) {
        if (state.pendingUserText != composerText && state.pendingUserText.isNotBlank()) {
            composerText = state.pendingUserText
        }
    }

    fun copyText(text: String) {
        clipboard.setText(AnnotatedString(text))
        scope.launch { snackbar.showSnackbar(copiedText) }
    }

    fun shareText(text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        context.startActivity(android.content.Intent.createChooser(intent, null))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ChatTopBar(
                title = state.conversationTitle,
                model = state.model,
                offline = state.offline,
                generating = state.generating,
                onOpenHistory = onOpenHistory,
                onOpenSettings = onOpenSettings,
                onNewChat = viewModel::newChat,
                onClearChat = viewModel::clearCurrentChat
            )
        },
        bottomBar = {
            Composer(
                initialText = composerText,
                generating = state.generating,
                offline = state.offline,
                configured = state.model.isNotBlank(),
                onTextChange = { composerText = it },
                onSend = { text ->
                    viewModel.sendMessage(text)
                    composerText = ""
                },
                onStop = viewModel::stopGeneration
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = state.offline, enter = fadeIn(), exit = fadeOut()) {
                    OfflineBanner()
                }
                if (state.messages.isEmpty() && !state.generating) {
                    EmptyChatWelcome { suggestion ->
                        viewModel.sendMessage(suggestion)
                        composerText = ""
                    }
                } else {
                    MessageList(
                        state = state,
                        onCopy = ::copyText,
                        onShare = ::shareText,
                        onDelete = viewModel::deleteMessage,
                        onRegenerate = viewModel::regenerate,
                        onRetry = viewModel::retry
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    title: String,
    model: String,
    offline: Boolean,
    generating: Boolean,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewChat: () -> Unit,
    onClearChat: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(online = !offline, active = generating)
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = model.ifBlank { stringResource(R.string.not_configured) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.Filled.HistoryIcon, contentDescription = stringResource(R.string.history))
            }
        },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_chat))
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.settings))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = { menuOpen = false; onOpenSettings() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_chat)) },
                    onClick = { menuOpen = false; onClearChat() }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun StatusDot(online: Boolean, active: Boolean) {
    val color = when {
        active -> MaterialTheme.colorScheme.primary
        online -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        Modifier
            .size(8.dp)
            .background(color, RoundedCornerShape(50))
    )
}

@Composable
private fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MessageList(
    state: ChatUiState,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit
) {
    val listState = rememberLazyListState()
    val atBottom = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount == 0 || last >= info.totalItemsCount - 2
        }
    }

    LaunchedEffect(state.messages.size, state.generating) {
        if (atBottom.value) {
            val target = state.messages.size + (if (state.generating) 1 else 0)
            if (target > 0) listState.animateScrollToItem((target - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(state.streamingDraft.length) {
        if (state.streamingDraft.isNotEmpty() && atBottom.value) {
            val target = state.messages.size + 1
            if (target > 0) listState.scrollToItem((target - 1).coerceAtLeast(0))
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                state.messages,
                onCopy = onCopy,
                onShare = onShare,
                onDelete = onDelete,
                onRegenerate = onRegenerate,
                onRetry = onRetry
            )
            if (state.generating) {
                item(key = "generating") { GeneratingBubble(draft = state.streamingDraft) }
            }
        }
        AnimatedVisibility(
            visible = !atBottom.value,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            ScrollToBottomButton(listState)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(
    messages: List<Message>,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit
) {
    messages.forEachIndexed { index, message ->
        item(key = message.id) {
            MessageBubble(
                message = message,
                isLast = index == messages.lastIndex,
                onCopy = onCopy,
                onShare = onShare,
                onDelete = onDelete,
                onRegenerate = if (message.isError) onRetry else onRegenerate
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isLast: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == Role.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = when {
                message.isError -> MaterialTheme.colorScheme.errorContainer
                isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = shape,
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            if (message.isError) {
                ErrorBubbleBody(
                    message = message,
                    onRetry = onRegenerate,
                    onDelete = { onDelete(message.id) }
                )
            } else {
                SelectionContainer {
                    if (isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    } else {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                            MarkdownText(markdown = message.content)
                        }
                    }
                }
            }
        }
        BubbleActions(
            message = message,
            isUser = isUser,
            isLast = isLast,
            onCopy = onCopy,
            onShare = onShare,
            onDelete = onDelete,
            onRegenerate = onRegenerate
        )
    }
}

@Composable
private fun ErrorBubbleBody(message: Message, onRetry: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
            text = stringResource(R.string.error_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun BubbleActions(
    message: Message,
    isUser: Boolean,
    isLast: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRegenerate: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = rememberTime(message.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
        BubbleActionIcon(Icons.Filled.ContentCopyIcon, R.string.copy) { onCopy(message.content) }
        BubbleActionIcon(Icons.Filled.Share, R.string.share) { onShare(message.content) }
        BubbleActionIcon(Icons.Filled.Delete, R.string.delete) { onDelete(message.id) }
        if (!isUser && isLast && !message.isError) {
            BubbleActionIcon(Icons.Filled.Refresh, R.string.regenerate) { onRegenerate() }
        }
    }
}

@Composable
private fun BubbleActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(
            icon,
            contentDescription = stringResource(labelRes),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun rememberTime(epochMillis: Long): String = remember(epochMillis) {
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))
}

@Composable
private fun GeneratingBubble(draft: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
        modifier = Modifier.widthIn(max = 340.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (draft.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypingIndicator()
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(R.string.typing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                MarkdownText(markdown = draft)
                BlinkingCursor()
            }
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Box(
        Modifier
            .size(width = 3.dp, height = 16.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                RoundedCornerShape(2.dp)
            )
    )
}

@Composable
private fun EmptyChatWelcome(onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(24.dp))
        val suggestions = listOf(
            stringResource(R.string.suggestion_explain),
            stringResource(R.string.suggestion_write),
            stringResource(R.string.suggestion_brainstorm),
            stringResource(R.string.suggestion_summarize)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            suggestions.forEach { suggestion ->
                Card(
                    onClick = { onSuggestion(suggestion) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollToBottomButton(listState: LazyListState) {
    val scope = rememberCoroutineScope()
    FilledIconButton(
        onClick = {
            scope.launch {
                if (listState.layoutInfo.totalItemsCount > 0) {
                    listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                }
            }
        },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.scroll_to_bottom)
        )
    }
}

@Composable
private fun Composer(
    initialText: String,
    generating: Boolean,
    offline: Boolean,
    configured: Boolean,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var text by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.autoSaver()) {
        mutableStateOf(initialText)
    }
    val canSend = text.isNotBlank() && !generating && !offline && configured

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.attachment),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onTextChange(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .imePadding(),
                placeholder = { Text(stringResource(R.string.hint_message)) },
                maxLines = 5,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            if (generating) {
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Filled.StopIcon,
                        contentDescription = stringResource(R.string.stop),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            } else {
                FilledIconButton(
                    onClick = {
                        onSend(text)
                        text = ""
                    },
                    enabled = canSend,
                    modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send)
                    )
                }
            }
        }
    }
}
