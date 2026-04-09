package com.example.streamingtext.domain.usecase

import com.example.streamingtext.data.model.Message
import com.example.streamingtext.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

/**
 * Domain use case: stream a chat response for a conversation history.
 *
 * Lives in the domain layer and depends only on the [ChatRepository] interface,
 * keeping the ViewModel unaware of which backend is active at runtime
 * (Claude API, public SSE stream, etc.).
 *
 * Invoked as a callable via [invoke] so call-sites read naturally:
 * ```kotlin
 * streamMessages(messages).collect { token -> ... }
 * ```
 */
class StreamMessagesUseCase(private val repository: ChatRepository) {
    operator fun invoke(messages: List<Message>): Flow<String> =
        repository.sendMessage(messages)
}
