package com.example.streamingtext.data.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val isStreaming: Boolean = false,
)

enum class Role { USER, ASSISTANT }

// --- Claude API wire types ---

data class ApiMessage(
    val role: String,   // "user" | "assistant"
    val content: String,
)