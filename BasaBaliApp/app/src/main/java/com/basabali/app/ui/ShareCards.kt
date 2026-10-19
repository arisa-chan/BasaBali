package com.basabali.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.basabali.app.data.ContentRepository
import com.basabali.app.data.UserStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the shareable pictures (sentence cards and progress cards) with
 * plain android.graphics and hands them to the system share sheet through a
 * FileProvider content URI — "proper formatted picture" sharing, not text.
 */
object ShareCards {
    private const val W = 1080
    private const val H = 1350

    private val CREAM = Color.parseColor("#FFF9EE")
    private val CREAM_DEEP = Color.parseColor("#FBE9C8")
    private val INK = Color.parseColor("#2B2118")
    private val INK_SOFT = Color.parseColor("#7A6A58")
    private val GOLD = Color.parseColor("#D99A2B")
    private val GOLD_DEEP = Color.parseColor("#B3781E")
    private val TEAL = Color.parseColor("#0F766E")
    private val CARD = Color.parseColor("#FFFDF6")

    fun sentenceCard(bal: String, en: String, register: String): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        background(c)
        header(c)

        // main card
        val card = RectF(70f, 330f, W - 70f, H - 330f)
        shadow(c, card)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = CARD
        c.drawRoundRect(card, 44f, 44f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = Color.argb(60, 217, 154, 43)
        c.drawRoundRect(card, 44f, 44f, p)
        p.style = Paint.Style.FILL

        // label
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        p.textSize = 30f
        p.color = TEAL
        p.textAlign = Paint.Align.CENTER
        c.drawText("BALINESE · BASA BALI", W / 2f, 410f, p)

        // Balinese sentence
        p.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        p.textSize = 66f
        p.color = INK
        val balLines = wrap(p, bal, card.width() - 140f)
        var y = 520f
        for (line in balLines) {
            c.drawText(line, W / 2f, y, p)
            y += p.textSize * 1.22f
        }

        // English
        p.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        p.textSize = 40f
        p.color = INK_SOFT
        val enLines = wrap(p, en, card.width() - 160f)
        y += 26f
        for (line in enLines) {
            c.drawText(line, W / 2f, y, p)
            y += p.textSize * 1.25f
        }

        // register chip
        chip(c, W / 2f - 150f, card.bottom - 130f, 300f, register)

        // avatar waving at the bottom-right of the card
        AvatarArt.draw(c, W - 190f, H - 240f, 210f, AvatarArt.HAPPY, 0.35f)
        footer(c, "Ngiring malajah Basa Bali!", "$bal")
        return bmp
    }

    fun progressCard(store: UserStore, content: ContentRepository): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        background(c)
        header(c)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        p.textSize = 58f
        p.color = INK
        p.textAlign = Paint.Align.CENTER
        c.drawText("My BasaBali progress", W / 2f, 470f, p)

        // stat blocks (kept clear of the mascot band below)
        val stats = listOf(
            "${store.streak}" to "day streak",
            "${store.xp}" to "XP earned",
            "${store.taughtWordCount()}" to "words learned",
            "${store.doneChunks.size}" to "steps done"
        )
        var sx = 110f
        var sy = 520f
        stats.forEachIndexed { i, s ->
            val r = RectF(sx, sy, sx + 400f, sy + 230f)
            shadow(c, r)
            p.color = CARD
            c.drawRoundRect(r, 36f, 36f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.5f
            p.color = Color.argb(50, 217, 154, 43)
            c.drawRoundRect(r, 36f, 36f, p)
            p.style = Paint.Style.FILL
            // accent bar
            p.color = if (i % 2 == 0) GOLD else TEAL
            c.drawRoundRect(RectF(r.left + 150f, r.top + 30f, r.right - 150f, r.top + 39f), 5f, 5f, p)
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            p.textSize = 80f
            p.color = GOLD_DEEP
            c.drawText(s.first, r.centerX(), r.top + 146f, p)
            p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            p.textSize = 30f
            p.color = INK_SOFT
            c.drawText(s.second, r.centerX(), r.top + 196f, p)
            if (i % 2 == 1) { sx = 110f; sy += 260f } else sx += 460f
        }

        // mascot band: motto on the left, Ayu on the right, both below the grid
        AvatarArt.draw(c, W - 220f, H - 220f, 200f, AvatarArt.HAPPY, 0.7f)
        p.textAlign = Paint.Align.LEFT
        p.color = INK
        p.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        p.textSize = 44f
        c.drawText("Ngiring malajah Basa Bali!", 90f, H - 250f, p)
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        p.textSize = 30f
        p.color = INK_SOFT
        c.drawText("${content.sentences.size} sentences · offline", 90f, H - 195f, p)
        footer(c, "BasaBali", "Learn Balinese from English")
        return bmp
    }

    fun share(context: Context, bitmap: Bitmap, caption: String) {
        try {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val f = File(dir, "basabali_${System.currentTimeMillis()}.png")
            FileOutputStream(f).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share"))
        } catch (_: Exception) {
            // sharing is best-effort; never crash the lesson over it
        }
    }

    // ---------------------------------------------------------------- drawing bits

    private fun background(c: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(
            0f, 0f, W.toFloat(), H.toFloat(),
            intArrayOf(CREAM, CREAM_DEEP, CREAM), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null
        p.color = Color.argb(26, 15, 118, 110)
        c.drawCircle(W - 120f, 140f, 260f, p)
        p.color = Color.argb(30, 217, 154, 43)
        c.drawCircle(90f, H - 120f, 300f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 6f
        p.color = Color.argb(45, 217, 154, 43)
        c.drawCircle(W - 120f, 140f, 190f, p)
        c.drawCircle(90f, H - 120f, 220f, p)
        p.style = Paint.Style.FILL
    }

    private fun header(c: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        p.textSize = 64f
        p.color = GOLD_DEEP
        c.drawText("BasaBali", W / 2f, 170f, p)
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        p.textSize = 30f
        p.color = INK_SOFT
        c.drawText("Basa Bali · learn from English", W / 2f, 214f, p)
    }

    private fun footer(c: Canvas, left: String, right: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        p.textSize = 34f
        p.color = INK
        c.drawText(left, 80f, H - 80f, p)
        p.textAlign = Paint.Align.RIGHT
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        p.textSize = 26f
        p.color = INK_SOFT
        val short = if (right.length > 60) right.take(57) + "…" else right
        c.drawText(short, W - 80f, H - 80f, p)
    }

    private fun chip(c: Canvas, x: Float, y: Float, w: Float, register: String) {
        val (label, color) = when (register) {
            "alus" -> "alus · refined" to GOLD
            "andap" -> "andap · familiar" to TEAL
            else -> "mider · neutral" to Color.parseColor("#9C938A")
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = RectF(x, y, x + w, y + 76f)
        p.color = Color.argb(34, Color.red(color), Color.green(color), Color.blue(color))
        c.drawRoundRect(r, 38f, 38f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = Color.argb(160, Color.red(color), Color.green(color), Color.blue(color))
        c.drawRoundRect(r, 38f, 38f, p)
        p.style = Paint.Style.FILL
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        p.textSize = 32f
        p.color = color
        p.textAlign = Paint.Align.CENTER
        c.drawText(label, r.centerX(), r.centerY() + 11f, p)
    }

    private fun shadow(c: Canvas, r: RectF) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.argb(26, 43, 33, 24)
        c.drawRoundRect(RectF(r.left + 6f, r.top + 10f, r.right + 10f, r.bottom + 12f), 46f, 46f, p)
    }

    private fun wrap(p: Paint, text: String, maxWidth: Float): List<String> {
        val lines = ArrayList<String>()
        var cur = ""
        for (word in text.split(" ")) {
            val candidate = if (cur.isEmpty()) word else "$cur $word"
            if (p.measureText(candidate) <= maxWidth) cur = candidate
            else {
                if (cur.isNotEmpty()) lines += cur
                cur = word
            }
        }
        if (cur.isNotEmpty()) lines += cur
        return lines
    }
}

/** Small picture-share button used on sentences (review rows and lesson feedback). */
@Composable
fun ShareSentenceButton(
    bal: String,
    en: String,
    register: String,
    tint: ComposeColor,
    size: Int = 30
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Surface(
        color = tint.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.size(size.dp).clickable {
            scope.launch {
                val bmp = withContext(Dispatchers.Default) { ShareCards.sentenceCard(bal, en, register) }
                ShareCards.share(context, bmp, "$bal — $en")
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("📤", fontSize = (size / 2.6).sp)
        }
    }
}
