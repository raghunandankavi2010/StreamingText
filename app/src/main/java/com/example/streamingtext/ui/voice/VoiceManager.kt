package com.example.streamingtext.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Wraps Android's built-in TextToSpeech and SpeechRecognizer engines.
 *
 * No API key or network dependency — both engines run entirely on-device.
 *
 * TTS streaming design:
 *   Tokens arrive one at a time from the LLM stream. Speaking a single word sounds robotic,
 *   and buffering the entire response delays audio. The solution: flush the buffer when we
 *   hit a sentence boundary (. ! ? newline). This gives natural sentence-by-sentence speech
 *   that closely tracks the streaming text.
 *
 * STT design:
 *   A single-shot recognition session. The caller is responsible for requesting the
 *   RECORD_AUDIO permission before calling [startListening].
 */
class VoiceManager(private val context: Context) {

    // -----------------------------------------------------------------------------------------
    // TTS
    // -----------------------------------------------------------------------------------------

    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    private val tokenBuffer = StringBuilder()
    private val utteranceId = AtomicInteger(0)

    /** Initialise TTS. Safe to call multiple times; re-init is a no-op if already ready. */
    fun initTts(onReady: () -> Unit = {}) {
        if (ttsReady) { onReady(); return }
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = Unit
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = Unit
                })
                ttsReady = true
                onReady()
            }
        }
    }

    /**
     * Append a streaming token to the internal buffer.
     * Speaks accumulated text whenever a sentence-ending character is encountered.
     * Call [flushTtsBuffer] when the stream ends to speak any remaining partial sentence.
     */
    fun enqueueToken(token: String) {
        if (!ttsReady) return
        tokenBuffer.append(token)

        // Scan for the first sentence boundary and speak everything up to (and including) it.
        val text = tokenBuffer.toString()
        val boundaryIdx = text.indexOfFirst { it == '.' || it == '!' || it == '?' || it == '\n' }
        if (boundaryIdx != -1) {
            val sentence = text.substring(0, boundaryIdx + 1).trim()
            if (sentence.isNotBlank()) speakNow(sentence)
            tokenBuffer.clear()
            val remainder = text.substring(boundaryIdx + 1)
            if (remainder.isNotBlank()) tokenBuffer.append(remainder)
        }
    }

    /** Speak whatever is left in the buffer after the stream has ended. */
    fun flushTtsBuffer() {
        val remaining = tokenBuffer.toString().trim()
        if (remaining.isNotBlank()) speakNow(remaining)
        tokenBuffer.clear()
    }

    fun stopSpeaking() {
        tts?.stop()
        tokenBuffer.clear()
    }

    val isSpeaking: Boolean get() = tts?.isSpeaking == true

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utt_${utteranceId.incrementAndGet()}")
    }

    // -----------------------------------------------------------------------------------------
    // STT
    // -----------------------------------------------------------------------------------------

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Start a single-shot speech recognition session.
     *
     * [onResult]  — called with the top hypothesis; empty string if nothing was heard.
     * [onError]   — called only for genuine, non-recoverable errors.
     * [onStarted] — called when the mic is open and actively recording.
     *
     * Must be called on the main thread.
     *
     * Error handling:
     *  - ERROR_RECOGNIZER_BUSY  → destroy, wait 300 ms, retry once automatically.
     *  - ERROR_NO_MATCH /
     *    ERROR_SPEECH_TIMEOUT   → treated as "nothing heard"; calls onResult("") so the
     *                             caller can silently reset to idle without a snackbar.
     *  - Everything else        → forwarded to onError.
     */
    fun startListening(
        onResult: (String) -> Unit,
        onError: (Int) -> Unit,
        onStarted: () -> Unit = {},
    ) = startListeningInternal(onResult, onError, onStarted, retryCount = 0)

    private fun startListeningInternal(
        onResult: (String) -> Unit,
        onError: (Int) -> Unit,
        onStarted: () -> Unit,
        retryCount: Int,
    ) {
        destroyRecognizer()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        // Guard so that only the first of onResults / onError is ever delivered.
        // (Both can fire in edge cases when the recognizer is torn down mid-session.)
        var delivered = false
        fun deliver(block: () -> Unit) { if (!delivered) { delivered = true; block() } }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onStarted()
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onResults(results: Bundle?) = deliver {
                val match = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                onResult(match)
            }

            override fun onError(error: Int) = deliver {
                when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        // Previous session hasn't fully torn down yet — retry once after a pause.
                        destroyRecognizer()
                        if (retryCount < 1) {
                            mainHandler.postDelayed({
                                startListeningInternal(onResult, onError, onStarted, retryCount + 1)
                            }, 350)
                        } else {
                            onError(error)
                        }
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // User didn't speak or speech was unclear — not a real error.
                        // Deliver empty string so the caller silently resets to idle.
                        onResult("")
                    }
                    else -> onError(error)
                }
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    // -----------------------------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------------------------

    /** Call from ViewModel.onCleared() to free native resources. */
    fun release() {
        mainHandler.removeCallbacksAndMessages(null) // cancel any pending busy-retry
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        destroyRecognizer()
    }
}