package com.basabali.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Text-to-speech proxy for Balinese using the Indonesian voice (id-ID).
 * Balinese shares most phonology and a large Mider/Indonesian loan layer,
 * so id-ID is a usable stand-in until the sentence bank is recorded by
 * native speakers (see README roadmap). `available` flips true when the
 * engine reports success; the UI listens to it via Compose state.
 */
class TtsManager(context: Context) {

    var available by mutableStateOf(false)
        private set

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val t = tts ?: return@TextToSpeech
                var res = t.setLanguage(Locale("id", "ID"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    res = t.setLanguage(Locale.ENGLISH)   // last-resort fallback
                }
                t.setSpeechRate(0.95f)
                available = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        }
    }

    fun speak(text: String, slow: Boolean = false) {
        val t = tts ?: return
        t.setSpeechRate(if (slow) 0.55f else 0.95f)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "basabali-tts")
    }

    /** Cut playback short (e.g. before opening the mic, so ASR can't hear the TTS). */
    fun stop() {
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun shutdown() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        tts = null
        available = false
    }
}
