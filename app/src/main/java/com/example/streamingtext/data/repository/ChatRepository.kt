package com.example.streamingtext.data.repository

import com.example.streamingtext.data.model.ApiMessage
import com.example.streamingtext.data.model.Message
import com.example.streamingtext.data.model.Role
import com.example.streamingtext.data.remote.ClaudeApiService
import com.example.streamingtext.data.remote.SseStreamingService
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun sendMessage(messages: List<Message>): Flow<String>
}

/**
 * Routes requests to the appropriate streaming backend.
 *
 * Priority:
 *  1. [ClaudeApiService] — when an API key is present (real LLM responses)
 *  2. [SseStreamingService] — otherwise (real SSE from stream.wikimedia.org)
 *
 * @param claudeApiService  null when no API key is configured.
 * @param sseStreamingService  public SSE fallback; defaults to a new instance.
 */
class ChatRepositoryImpl(
    private val claudeApiService: ClaudeApiService?,
    private val sseStreamingService: SseStreamingService = SseStreamingService(),
) : ChatRepository {

    override fun sendMessage(messages: List<Message>): Flow<String> {
        val lastUserMessage = messages.lastOrNull { it.role == Role.USER }?.content.orEmpty()

        if (claudeApiService != null) {
            val apiMessages = messages.map { msg ->
                ApiMessage(
                    role = if (msg.role == Role.USER) "user" else "assistant",
                    content = msg.content,
                )
            }
            return claudeApiService.streamMessage(apiMessages)
        }

        return sseStreamingService.stream(lastUserMessage)
    }
}