package com.example.streamingtext.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.streamingtext.BuildConfig
import com.example.streamingtext.data.local.ImeMetricsDataSource
import com.example.streamingtext.data.model.ImeHeights
import com.example.streamingtext.data.model.Message
import com.example.streamingtext.data.model.Role
import com.example.streamingtext.data.remote.ClaudeApiService
import com.example.streamingtext.data.repository.ChatRepositoryImpl
import com.example.streamingtext.data.repository.ImeMetricsRepositoryImpl
import com.example.streamingtext.domain.usecase.ObserveImeHeightsUseCase
import com.example.streamingtext.domain.usecase.RecordImeHeightUseCase
import com.example.streamingtext.domain.usecase.StreamMessagesUseCase
import com.example.streamingtext.ui.voice.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val isListening: Boolean = false,
    val isTtsEnabled: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    application: Application,
    private val streamMessages: StreamMessagesUseCase,
    observeImeHeights: ObserveImeHeightsUseCase,
    private val recordImeHeight: RecordImeHeightUseCase,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /**
     * Persisted per-orientation IME heights, hot-shared as a [StateFlow] so the chat
     * screen can read the seed value synchronously at first composition.
     */
    val imeHeights: StateFlow<ImeHeights> = observeImeHeights()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ImeHeights(),
        )

    fun onImeHeightObserved(orientation: Int, heightPx: Int) {
        viewModelScope.launch { recordImeHeight(orientation, heightPx) }
    }

    private var streamingJob: Job? = null

    // VoiceManager holds Android TTS + STT; needs Application context.
    private val voiceManager = VoiceManager(application).also { it.initTts() }

    // -----------------------------------------------------------------------------------------
    // Text chat
    // -----------------------------------------------------------------------------------------

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isStreaming) return

        val userMessage = Message(role = Role.USER, content = text)
        val assistantPlaceholder = Message(role = Role.ASSISTANT, content = "", isStreaming = true)

        // Pre-assign the assistant slot index BEFORE launching the coroutine so token
        // appends always land in the right message even under rapid emissions.
        var assistantIndex = -1
        _uiState.update { state ->
            val newMessages = state.messages + userMessage + assistantPlaceholder
            assistantIndex = newMessages.lastIndex
            state.copy(
                messages = newMessages,
                inputText = "",
                isStreaming = true,
                error = null,
            )
        }

        // Everything except the empty assistant placeholder goes to the API / mock.
        val messagesToSend = _uiState.value.messages.dropLast(1)

        streamingJob = viewModelScope.launch {
            streamMessages(messagesToSend)
                .catch { e ->
                    voiceManager.stopSpeaking()
                    _uiState.update { state ->
                        state.markStreamingDone(assistantIndex)
                            .copy(error = e.message ?: "An error occurred")
                    }
                }
                .collect { token ->
                    _uiState.update { state -> state.appendToken(assistantIndex, token) }
                    // Feed each token into TTS; it will speak at sentence boundaries.
                    if (_uiState.value.isTtsEnabled) voiceManager.enqueueToken(token)
                }

            // Stream ended cleanly — speak any trailing partial sentence.
            if (_uiState.value.isTtsEnabled) voiceManager.flushTtsBuffer()
            _uiState.update { it.markStreamingDone(assistantIndex) }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        voiceManager.stopSpeaking()
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map {
                    if (it.isStreaming) it.copy(isStreaming = false) else it
                },
                isStreaming = false,
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // -----------------------------------------------------------------------------------------
    // TTS toggle
    // -----------------------------------------------------------------------------------------

    fun toggleTts() {
        val enabling = !_uiState.value.isTtsEnabled
        if (!enabling) voiceManager.stopSpeaking()
        _uiState.update { it.copy(isTtsEnabled = enabling) }
    }

    // -----------------------------------------------------------------------------------------
    // Voice input (STT)
    // -----------------------------------------------------------------------------------------

    fun startVoiceInput() {
        if (!voiceManager.isAvailable) {
            _uiState.update { it.copy(error = "Speech recognition not available on this device") }
            return
        }
        _uiState.update { it.copy(isListening = true, inputText = "") }
        voiceManager.startListening(
            onStarted = { /* mic is open — UI already shows listening state */ },
            onResult = { transcript ->
                _uiState.update { it.copy(inputText = transcript, isListening = false) }
                if (transcript.isNotBlank()) sendMessage()
            },
            onError = { code ->
                val msg = sttErrorMessage(code)
                _uiState.update { it.copy(isListening = false, error = msg) }
            },
        )
    }

    fun cancelVoiceInput() {
        voiceManager.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    // -----------------------------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }

    // -----------------------------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------------------------

    private fun ChatUiState.appendToken(index: Int, token: String): ChatUiState {
        if (index !in messages.indices) return this
        val updated = messages.toMutableList()
        updated[index] = updated[index].copy(content = updated[index].content + token)
        return copy(messages = updated)
    }

    private fun ChatUiState.markStreamingDone(index: Int): ChatUiState {
        val updated = messages.toMutableList()
        if (index in updated.indices) updated[index] = updated[index].copy(isStreaming = false)
        return copy(messages = updated, isStreaming = false)
    }

    private fun sttErrorMessage(code: Int): String = when (code) {
        android.speech.SpeechRecognizer.ERROR_AUDIO -> "Microphone error"
        android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't hear anything — try again"
        android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        android.speech.SpeechRecognizer.ERROR_NETWORK,
        android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during recognition"
        android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        else -> "Speech recognition error ($code)"
    }

    // -----------------------------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------------------------

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val apiKey = BuildConfig.CLAUDE_API_KEY
                val apiService = if (apiKey.isNotBlank()) ClaudeApiService(apiKey) else null
                val chatRepository = ChatRepositoryImpl(claudeApiService = apiService)
                val streamMessagesUseCase = StreamMessagesUseCase(chatRepository)

                val imeMetricsRepository = ImeMetricsRepositoryImpl(
                    dataSource = ImeMetricsDataSource(application),
                )
                val observeImeHeightsUseCase = ObserveImeHeightsUseCase(imeMetricsRepository)
                val recordImeHeightUseCase = RecordImeHeightUseCase(imeMetricsRepository)

                return ChatViewModel(
                    application = application,
                    streamMessages = streamMessagesUseCase,
                    observeImeHeights = observeImeHeightsUseCase,
                    recordImeHeight = recordImeHeightUseCase,
                ) as T
            }
        }
    }
}