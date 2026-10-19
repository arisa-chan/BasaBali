package com.basabali.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Speech recognition proxy using the Indonesian (id-ID) recognizer for the
 * read-aloud exercise (E9). Returns up to 5 hypotheses; the runner scores
 * the best one with TextMatch against the target sentence.
 * Requires RECORD_AUDIO permission (requested by the UI) and a recognition
 * service on the device — availability is checked at runtime.
 */
class AsrManager(private val context: Context) {

    var listening by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var recognizer: SpeechRecognizer? = null

    fun available(): Boolean =
        try { SpeechRecognizer.isRecognitionAvailable(context) } catch (_: Exception) { false }

    /** Abandon an in-flight listen (used when the learner skips the exercise). */
    fun stop() {
        val sr = recognizer
        recognizer = null
        listening = false
        try { sr?.stopListening() } catch (_: Exception) {}
        try { sr?.destroy() } catch (_: Exception) {}
    }

    fun listen(onResult: (List<String>) -> Unit) {
        if (!available()) {
            error = "No speech recognizer on this device"
            onResult(emptyList())
            return
        }
        error = null
        val sr = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (_: Exception) {
            error = "Recognizer unavailable"
            onResult(emptyList())
            return
        }
        recognizer = sr
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(err: Int) {
                listening = false
                error = "Mic error ($err)"
                recognizer = null
                try { sr.destroy() } catch (_: Exception) {}
                onResult(emptyList())
            }
            override fun onResults(results: Bundle?) {
                listening = false
                recognizer = null
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: arrayListOf()
                try { sr.destroy() } catch (_: Exception) {}
                onResult(list)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        listening = true
        try {
            sr.startListening(intent)
        } catch (_: Exception) {
            listening = false
            error = "Could not start listening"
            onResult(emptyList())
        }
    }
}
