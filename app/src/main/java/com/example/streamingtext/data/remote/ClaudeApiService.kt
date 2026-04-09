package com.example.streamingtext.data.remote

import com.example.streamingtext.data.model.ApiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Streams tokens from the Claude API over Server-Sent Events (SSE).
 *
 * The Claude API returns newline-delimited SSE events. Each `content_block_delta` event
 * carries a JSON payload with the next text token. We read the response body line-by-line,
 * parse only the delta events, and emit each token into the returned Flow.
 *
 * Key design decisions:
 *  - `flow { ... }.flowOn(Dispatchers.IO)` — upstream network work runs on IO, downstream
 *    collection (ViewModel) stays on Main. Never use `withContext` inside a `flow {}` builder.
 *  - Read timeout is 60 s to keep the connection alive while waiting for the first token.
 *  - Body / reader are always closed in a `finally` block to prevent connection leaks.
 */
class ClaudeApiService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun streamMessage(messages: List<ApiMessage>): Flow<String> = flow {
        val jsonMessages = messages.joinToString(",", "[", "]") { msg ->
            """{"role":"${msg.role}","content":${JSONObject.quote(msg.content)}}"""
        }

        val body = """
            {
              "model": "claude-3-5-sonnet-20241022",
              "max_tokens": 1024,
              "stream": true,
              "messages": $jsonMessages
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .addHeader("accept", "text/event-stream")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val error = response.body?.string() ?: "Unknown error"
            throw ClaudeApiException(response.code, error)
        }

        val reader = response.body?.charStream()?.buffered()
            ?: throw IllegalStateException("Empty response body from Claude API")

        try {
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") continue

                runCatching {
                    val json = JSONObject(data)
                    if (json.optString("type") == "content_block_delta") {
                        val text = json.optJSONObject("delta")?.optString("text")
                        if (!text.isNullOrEmpty()) {
                            emit(text)
                        }
                    }
                }
                // malformed lines are silently skipped via runCatching
            }
        } finally {
            reader.close()
            response.body?.close()
        }
    }.flowOn(Dispatchers.IO)
}

class ClaudeApiException(val code: Int, message: String) :
    Exception("Claude API error $code: $message")
