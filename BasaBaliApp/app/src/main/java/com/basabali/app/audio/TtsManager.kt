package com.basabali.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Text-to-speech proxy for Balinese using the Indonesian voice (id-ID) when the
 * device has it, otherwise Indonesian generic, then the engine default / English.
 *
 * The engine is usable as soon as Android reports TextToSpeech.SUCCESS, even if
 * the requested language pack is absent: a missing language must not grey out
 * every speaker button in the app.  Calls made while the engine is still
 * initialising are queued and spoken once it is ready.
 */
class TtsManager(context: Context) {

    var available by mutableStateOf(false)
        private set
    var languageLabel by mutableStateOf("starting…")
        private set

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, Boolean>? = null

    private var initStatus: Int? = null

    init {
        // Assign the field only after construction; some engines invoke the
        // callback before the constructor returns, so setup() is idempotent and
        // can be retried once the field is in place.
        val engine = TextToSpeech(context.applicationContext) { status ->
            initStatus = status
            setup()
        }
        tts = engine
        setup()
    }

    private fun setup() {
        if (ready) return
        val status = initStatus ?: return
        val t = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            languageLabel = "unavailable"
            return
        }
        val candidates = listOf(
            Locale("id", "ID"),
            Locale("id"),
            Locale.getDefault(),
            Locale.ENGLISH
        )
        var chosen: Locale? = null
        for (loc in candidates) {
            val res = try { t.setLanguage(loc) } catch (_: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                chosen = loc
                break
            }
        }
        if (chosen == null) {
            chosen = try { t.defaultLanguage } catch (_: Exception) { null }
            if (chosen != null) {
                try { t.setLanguage(chosen) } catch (_: Exception) {}
            }
        }
        try {
            t.setSpeechRate(0.95f)
            t.setPitch(1.0f)
        } catch (_: Exception) {}
        languageLabel = when {
            chosen == null -> "default voice"
            chosen.language == "id" -> "Indonesian proxy"
            else -> chosen.displayName
        }
        ready = true
        available = true
        pending?.let { (text, slow) ->
            pending = null
            speak(text, slow)
        }
    }

    fun speak(text: String, slow: Boolean = false) {
        if (text.isBlank()) return
        val t = tts
        if (!ready || t == null) {
            pending = text to slow
            return
        }
        try {
            t.setSpeechRate(if (slow) 0.55f else 0.95f)
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "basabali-tts")
        } catch (_: Exception) {
            // keep the UI alive if the engine disappears mid-session
        }
    }

    /** Cut playback short (e.g. before opening the mic, so ASR can't hear the TTS). */
    fun stop() {
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun shutdown() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        tts = null
        ready = false
        available = false
    }
}
