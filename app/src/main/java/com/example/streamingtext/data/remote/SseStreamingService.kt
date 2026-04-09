package com.example.streamingtext.data.remote

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
 * Streams real token-by-token AI responses from Pollinations.AI.
 *
 * **Why Pollinations.AI:**
 *  - Completely free — no account, no API key, no credit card
 *  - HTTPS endpoint with a valid certificate (no SSL issues)
 *  - Streams responses in the standard OpenAI SSE format
 *
 * **Endpoint:** POST https://text.pollinations.ai/openai
 *
 * **Request body (OpenAI-compatible):**
 * ```json
 * {
 *   "model": "openai",
 *   "messages": [{"role": "user", "content": "..."}],
 *   "stream": true
 * }
 * ```
 *
 * **SSE response format:**
 * ```
 * data: {"choices":[{"delta":{"content":"Hello"}}]}
 * data: {"choices":[{"delta":{"content":" world"}}]}
 * data: \[DONE\]
 * ```
 *
 * Parsing mirrors [ClaudeApiService]: line-by-line reading, `data:` prefix filter,
 * `[DONE]` sentinel, JSON extraction via [runCatching] to silently skip malformed lines.
 */
class SseStreamingService {

    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val SSE_URL = "https://text.pollinations.ai/openai"
    }

    fun stream(userQuery: String): Flow<String> = flow {
        val requestBody = """
            {
              "model": "openai",
              "messages": [{"role": "user", "content": ${JSONObject.quote(userQuery)}}],
              "stream": true,
              "max_tokens": 400
            }
        """.trimIndent()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(SSE_URL)
            .addHeader("Accept", "text/event-stream")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw SseConnectionException(response.code, "HTTP ${response.code}")
        }

        val reader = response.body?.charStream()?.buffered()
            ?: throw IllegalStateException("Empty response body from SSE endpoint")

        try {
            while (true) {
                val line = reader.readLine() ?: break

                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break

                runCatching {
                    val json = JSONObject(data)
                    val content = json
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                    if (!content.isNullOrEmpty()) emit(content)
                }
                // malformed lines are silently skipped via runCatching
            }
        } finally {
            reader.close()
            response.body?.close()
        }
    }.flowOn(Dispatchers.IO)
}

class SseConnectionException(code: Int, message: String) :
    Exception("SSE connection error $code: $message")