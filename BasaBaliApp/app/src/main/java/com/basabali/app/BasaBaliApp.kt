package com.basabali.app

import android.app.Application
import android.content.Context
import com.basabali.app.audio.AsrManager
import com.basabali.app.audio.SoundFx
import com.basabali.app.audio.TtsManager
import com.basabali.app.data.ContentRepository
import com.basabali.app.data.UserStore
import com.basabali.app.engine.SessionBuilder
import com.basabali.app.engine.Srs

/** Tiny service locator: read-only course content, one JSON file of user state, shared managers. */
object AppGraph {
    lateinit var content: ContentRepository
        private set
    lateinit var store: UserStore
        private set
    lateinit var builder: SessionBuilder
        private set
    var tts: TtsManager? = null
        private set
    var asr: AsrManager? = null
        private set
    var fx: SoundFx? = null
        private set

    @Volatile
    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        content = ContentRepository(context.assets)
        content.ensureLoaded()
        store = UserStore(context.applicationContext)
        store.load()
        builder = SessionBuilder(content, store)
        Srs.mode = if (store.schedulerMode == 1) Srs.Mode.FSRS else Srs.Mode.LEITNER
        try { tts = TtsManager(context.applicationContext) } catch (_: Exception) { tts = null }
        try { asr = AsrManager(context.applicationContext) } catch (_: Exception) { asr = null }
        try { fx = SoundFx(context.applicationContext) } catch (_: Exception) { fx = null }
        initialized = true
    }
}

class BasaBaliApp : Application()
