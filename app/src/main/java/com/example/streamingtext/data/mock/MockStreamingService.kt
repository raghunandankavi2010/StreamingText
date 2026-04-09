package com.example.streamingtext.data.mock

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Simulates a streaming LLM response without a real API key.
 *
 * Splits a pre-written response into sub-word tokens and emits them with variable delays,
 * closely mimicking the burst-and-pause cadence of real LLM inference.
 */
object MockStreamingService {

    // Each entry is a list of trigger keywords paired with a canned response.
    // The first matching entry wins; the last entry is the catch-all fallback.
    private val cannedResponses: List<Pair<List<String>, String>> = listOf(
        listOf("hello", "hi", "hey", "greet") to
            "Hey there! I'm your demo AI assistant. I don't have a real API key, so I'm " +
            "running in mock mode — but the streaming effect you're seeing is the real deal. " +
            "Each word is emitted individually with a small delay, just like tokens arriving " +
            "from a live language model over Server-Sent Events. Ask me anything!",

        listOf("how are you", "how r u", "how do you do") to
            "I'm doing great, thanks for asking! As a demo assistant I'm always in top form — " +
            "no hallucinations, no context window limits, and I never charge per token. " +
            "What can I help you with today?",

        listOf("your name", "who are you", "what are you") to
            "I'm a mock AI chatbot built to demonstrate real-time token streaming in Android " +
            "using Jetpack Compose and Kotlin Coroutines. In production I'd be backed by a " +
            "real LLM like Claude or GPT-4, but right now I'm powered entirely by a Kotlin " +
            "object with a list of canned responses and some well-placed delays.",

        listOf("weather", "forecast", "temperature", "rain", "sunny") to
            "I'd love to help with weather, but as a mock assistant I don't have access to " +
            "live data. In a real app you'd wire up an API like OpenWeatherMap alongside the " +
            "LLM. For now I can tell you it's always a perfect 72°F inside your Android " +
            "emulator — no sunscreen required.",

        listOf("joke", "funny", "laugh", "humor") to
            "Sure! Why do Kotlin developers never get lost? Because they always have a " +
            "companion object to guide them. 😄 Ba-dum-tss. Okay, I'll admit the " +
            "coroutines jokes are better — they run asynchronously so the punchline arrives " +
            "before you expect it.",

        listOf("kotlin", "language", "programming") to
            "Kotlin is a statically typed, multiplatform language from JetBrains that runs " +
            "on the JVM, JavaScript, and native targets. On Android it's the officially " +
            "preferred language. Its killer features include null safety, extension functions, " +
            "data classes, coroutines for async, and a concise syntax that cuts Java " +
            "boilerplate by roughly 40%. Once you go Kotlin, you rarely look back.",

        listOf("compose", "jetpack", "ui", "declarative") to
            "Jetpack Compose is Android's modern declarative UI toolkit. Instead of inflating " +
            "XML layouts and finding views by ID, you describe your UI as composable functions " +
            "and Compose figures out the minimal recomposition needed when state changes. " +
            "Combined with StateFlow and collectAsStateWithLifecycle(), it makes streaming " +
            "token-by-token updates trivially easy — each token append triggers a targeted " +
            "recomposition of only the affected Text composable.",

        listOf("coroutine", "flow", "async", "suspend") to
            "Kotlin Coroutines give you lightweight concurrency without threads. A Flow is a " +
            "cold asynchronous stream — nothing runs until you collect it. For LLM streaming " +
            "we model each token as a Flow emission: the network layer produces tokens on " +
            "Dispatchers.IO via flowOn(), and the ViewModel collects them on Main, appending " +
            "each token to the StateFlow. The UI observes the StateFlow and recomposes " +
            "automatically. No callbacks, no RxJava, no thread management.",

        listOf("streaming", "sse", "server-sent", "token") to
            "Server-Sent Events (SSE) is a dead-simple HTTP streaming protocol: the client " +
            "makes one POST request and the server keeps the connection open, pushing " +
            "newline-delimited 'data:' chunks as they become available. Claude and OpenAI " +
            "both use SSE for streaming responses. On Android, OkHttp reads the response " +
            "body line-by-line — no special library needed. Each 'content_block_delta' event " +
            "carries a JSON payload with the next text token, which we emit into the Flow.",

        listOf("viewmodel", "architecture", "mvvm", "state") to
            "This app follows MVVM: the ViewModel holds a MutableStateFlow<ChatUiState> and " +
            "exposes it as an immutable StateFlow. When the user sends a message, the ViewModel " +
            "pre-allocates an assistant message slot with a stable index, then launches a " +
            "coroutine that collects the token Flow and appends each emission to that slot. " +
            "Using a pre-assigned index prevents race conditions: even if two tokens arrive " +
            "rapidly, they always land in the right message.",

        listOf("help", "what can", "feature", "capability") to
            "As a demo assistant I can answer questions about Android development, Kotlin, " +
            "Jetpack Compose, coroutines, streaming architecture, and general programming. " +
            "I also tell jokes (quality not guaranteed). For anything requiring live data — " +
            "weather, news, calculations — you'd want to replace me with a real LLM backed " +
            "by an API key. Try asking about Kotlin, Compose, coroutines, or SSE streaming!",

        // catch-all
        listOf<String>() to
            "That's an interesting question! As a mock assistant I don't have a trained model " +
            "behind me, so I can't give you a truly intelligent answer here. In production " +
            "this response would come from a large language model streaming tokens over SSE. " +
            "For now, just know that the streaming effect you're watching — each word " +
            "appearing one at a time — is powered by real Kotlin Flow emissions with " +
            "realistic timing delays. Pretty cool, right?",
    )

    /**
     * Emits streamed tokens for [query] with realistic variable timing.
     * Whitespace is emitted faster; word tokens have a slight random spread.
     */
    fun stream(query: String): Flow<String> = flow {
        val response = pickResponse(query)
        // Split on whitespace boundaries while keeping the delimiter characters
        val tokens = response.split(Regex("(?<=\\s)|(?=\\s)"))
        for (token in tokens) {
            emit(token)
            delay(
                when {
                    token.isBlank() -> 12L
                    token.length == 1 -> 30L
                    else -> (22L..50L).random()
                }
            )
        }
    }

    private fun pickResponse(query: String): String {
        val lower = query.lowercase()
        for ((keywords, response) in cannedResponses) {
            if (keywords.isEmpty()) return response          // catch-all
            if (keywords.any { lower.contains(it) }) return response
        }
        return cannedResponses.last().second
    }
}