package com.example.streamingtext.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.streamingtext.ui.chat.components.ChatInputBar
import com.example.streamingtext.ui.chat.components.EmojiPanel
import com.example.streamingtext.ui.chat.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val textFieldFocusRequester = remember { FocusRequester() }

    // Request RECORD_AUDIO at runtime when the user taps the mic button.
    // Declare this FIRST to ensure it's available for capture in lambdas below.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startVoiceInput()
        else viewModel.dismissError()
    }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Persisted per-orientation IME heights.
    val imeHeights by viewModel.imeHeights.collectAsStateWithLifecycle()
    val orientation = LocalConfiguration.current.orientation
    val storedImeHeightPx = when (orientation) {
        android.content.res.Configuration.ORIENTATION_LANDSCAPE -> imeHeights.landscapePx
        else -> imeHeights.portraitPx
    }

    // Tracks the peak observed IME height.
    val defaultEmojiHeightPx = with(density) { 300.dp.roundToPx() }
    var savedImeHeightPx by remember(storedImeHeightPx) { mutableIntStateOf(storedImeHeightPx) }
    var emojiPanelVisible by remember { mutableStateOf(false) }
    var pendingKeyboardRestore by remember { mutableStateOf(false) }
    var expectingImeClose by remember { mutableStateOf(false) }

    val imeInsets = WindowInsets.ime
    val currentIsImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)

    // Sync IME height and handle transitions.
    LaunchedEffect(orientation, density) {
        var lastPx = 0
        snapshotFlow { imeInsets.getBottom(density) }
            .collect { px ->
                if (px > savedImeHeightPx) {
                    savedImeHeightPx = px
                    if (currentIsImeVisible) {
                        viewModel.onImeHeightObserved(orientation, px)
                    }
                }

                if (pendingKeyboardRestore && savedImeHeightPx > 0 && px >= savedImeHeightPx) {
                    emojiPanelVisible = false
                    pendingKeyboardRestore = false
                }

                if (px < lastPx) {
                    if (expectingImeClose) {
                        if (px == 0) expectingImeClose = false
                    } else if (emojiPanelVisible) {
                        emojiPanelVisible = false
                        pendingKeyboardRestore = false
                    }
                }
                lastPx = px
            }
    }

    BackHandler(enabled = emojiPanelVisible) {
        emojiPanelVisible = false
        pendingKeyboardRestore = false
    }

    // Stable callbacks to prevent unnecessary recompositions.
    val onEmojiToggle: () -> Unit = remember(emojiPanelVisible, savedImeHeightPx, density) {
        {
            if (emojiPanelVisible) {
                pendingKeyboardRestore = true
                textFieldFocusRequester.requestFocus()
                keyboardController?.show()
            } else {
                if (savedImeHeightPx == 0) savedImeHeightPx = defaultEmojiHeightPx
                if (imeInsets.getBottom(density) > 0) expectingImeClose = true
                emojiPanelVisible = true
                pendingKeyboardRestore = false
                keyboardController?.hide()
            }
        }
    }

    val currentInputText by rememberUpdatedState(uiState.inputText)
    val onEmojiInsert: (String) -> Unit = remember {
        { emoji ->
            viewModel.onInputTextChange(currentInputText + emoji)
        }
    }

    val onBackspace: () -> Unit = remember {
        {
            val current = currentInputText
            if (current.isNotEmpty()) {
                val cps = current.codePointCount(0, current.length)
                val cutoff = current.offsetByCodePoints(0, cps - 1)
                viewModel.onInputTextChange(current.substring(0, cutoff))
            }
        }
    }

    val onMicClicked: () -> Unit = remember(context, permissionLauncher) {
        {
            val permission = Manifest.permission.RECORD_AUDIO
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startVoiceInput()
            } else {
                permissionLauncher.launch(permission)
            }
        }
    }

    // Auto-scroll whenever new tokens arrive or a new message is added.
    val lastLength = uiState.messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(uiState.messages.size, lastLength) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // Surface errors via Snackbar.
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it, actionLabel = "OK")
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("AI Chat") },
                actions = {
                    IconButton(onClick = viewModel::toggleTts) {
                        Icon(
                            imageVector = if (uiState.isTtsEnabled)
                                Icons.AutoMirrored.Filled.VolumeUp
                            else
                                Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (uiState.isTtsEnabled)
                                "Disable voice output"
                            else
                                "Enable voice output",
                            tint = if (uiState.isTtsEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            val navBottomPx = WindowInsets.navigationBars.getBottom(density)
            val imeBottomPx = imeInsets.getBottom(density)
            val lowerRegionPx = if (emojiPanelVisible) {
                kotlin.math.max(imeBottomPx, savedImeHeightPx)
            } else {
                kotlin.math.max(imeBottomPx, navBottomPx)
            }
            val lowerRegionDp = with(density) { lowerRegionPx.toDp() }
            val savedImeHeightDp = with(density) { savedImeHeightPx.toDp() }

            Column(modifier = Modifier.fillMaxWidth()) {
                ChatInputBar(
                    inputText = uiState.inputText,
                    isStreaming = uiState.isStreaming,
                    isListening = uiState.isListening,
                    isEmojiOpen = emojiPanelVisible,
                    textFieldFocusRequester = textFieldFocusRequester,
                    onTextChange = viewModel::onInputTextChange,
                    onSend = viewModel::sendMessage,
                    onStop = viewModel::stopStreaming,
                    onMicClick = onMicClicked,
                    onCancelListening = viewModel::cancelVoiceInput,
                    onEmojiToggle = onEmojiToggle,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lowerRegionDp),
                ) {
                    if (emojiPanelVisible) {
                        EmojiPanel(
                            onEmojiClick = onEmojiInsert,
                            onBackspace = onBackspace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(savedImeHeightDp)
                                .align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
    ) { innerPadding ->
        if (uiState.messages.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding).fillMaxSize())
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id },
                ) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Start a conversation",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Type or tap the mic to speak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
