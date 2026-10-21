package com.basabali.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.basabali.app.R

/**
 * Five synthesized UI sounds (see tools/make_sounds.py), played through a
 * low-latency SoundPool.  All files are loudness-normalized to the same RMS
 * level.  A sound requested before its sample has finished loading is queued
 * and played as soon as SoundPool reports it ready (the first tap of a
 * session used to be silent because load() is asynchronous).
 */
class SoundFx(context: Context) {

    enum class Sound { CLICK, CORRECT, WRONG, COMPLETE, FAIL }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Sound, Int>()
    private val loaded = HashSet<Int>()
    private val pending = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loaded += sampleId
                if (pending.remove(sampleId)) playId(sampleId)
            }
        }
        ids[Sound.CLICK] = pool.load(context, R.raw.click, 1)
        ids[Sound.CORRECT] = pool.load(context, R.raw.correct, 1)
        ids[Sound.WRONG] = pool.load(context, R.raw.wrong, 1)
        ids[Sound.COMPLETE] = pool.load(context, R.raw.complete, 1)
        ids[Sound.FAIL] = pool.load(context, R.raw.fail, 1)
    }

    fun play(s: Sound) {
        val id = ids[s] ?: return
        if (id in loaded) playId(id)
        else if (id != 0) pending += id
    }

    private fun playId(id: Int) {
        try { pool.play(id, 1f, 1f, 1, 0, 1f) } catch (_: Exception) {}
    }

    fun release() {
        try { pool.release() } catch (_: Exception) {}
    }
}
