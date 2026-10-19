package com.basabali.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.basabali.app.R

/**
 * Five synthesized UI sounds (see tools/make_sounds.py), played through a
 * low-latency SoundPool: tile click, correct, wrong, subunit complete,
 * subunit failed (out of hearts). All five are distinct.
 */
class SoundFx(context: Context) {

    enum class Sound { CLICK, CORRECT, WRONG, COMPLETE, FAIL }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Sound, Int>()

    init {
        ids[Sound.CLICK] = pool.load(context, R.raw.click, 1)
        ids[Sound.CORRECT] = pool.load(context, R.raw.correct, 1)
        ids[Sound.WRONG] = pool.load(context, R.raw.wrong, 1)
        ids[Sound.COMPLETE] = pool.load(context, R.raw.complete, 1)
        ids[Sound.FAIL] = pool.load(context, R.raw.fail, 1)
    }

    fun play(s: Sound) {
        try {
            ids[s]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
        } catch (_: Exception) {
        }
    }

    fun release() {
        try { pool.release() } catch (_: Exception) {}
    }
}
