package com.basabali.app.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * "Ayu" — BasaBali's one Balinese mascot. Drawn with primitives (no image
 * assets) so the same figure appears in the exercises, on the completion
 * screen and inside shared pictures. Her look nods to everyday Balinese
 * custom: a cream kebaya with a gold sash and a frangipani (kamboja) flower
 * tucked by her ear — the flower Balinese women wear and that every offering
 * (canang sari) carries.
 */
object AvatarArt {
    const val IDLE = 0
    const val HAPPY = 1
    const val ENCOURAGING = 2

    private val SKIN = 0xFFE8B98A.toInt()
    private val SKIN_SHADE = 0xFFD9A576.toInt()
    private val HAIR = 0xFF2B1D14.toInt()
    private val HAIR_LIGHT = 0xFF4A3323.toInt()
    private val KEBAYA = 0xFFFDF8EC.toInt()
    private val KEBAYA_SHADE = 0xFFEDE1C8.toInt()
    private val SASH = 0xFFD99A2B.toInt()
    private val SASH_DEEP = 0xFFB3781E.toInt()
    private val FLOWER = 0xFFFFF7E4.toInt()
    private val FLOWER_CORE = 0xFFE8A93B.toInt()
    private val CHEEK = 0x33D96C6C.toInt()
    private val EYE = 0xFF2B2118.toInt()
    private val MOUTH = 0xFF8A4B3A.toInt()

    /**
     * [cx], [cy] are the centre of the drawing box; [size] its width.
     * [t] is a monotonically growing time in seconds; [mood] is IDLE/HAPPY/ENCOURAGING.
     */
    fun draw(canvas: Canvas, cx: Float, cy: Float, size: Float, mood: Int, t: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val bob = if (mood == HAPPY) sin(t * 3.1f) * 1.6f else sin(t * 1.7f) * 1.0f
        val s = size / 100f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(s, s)
        canvas.translate(0f, bob - 6f)

        // ---- hair back + side strands
        p.color = HAIR
        canvas.drawOval(RectF(-21f, -34f, 21f, 6f), p)
        canvas.drawRoundRect(RectF(-21f, -22f, -11f, 14f), 5f, 5f, p)
        canvas.drawRoundRect(RectF(11f, -22f, 21f, 14f), 5f, 5f, p)

        // ---- neck + kebaya torso
        p.color = SKIN_SHADE
        canvas.drawRoundRect(RectF(-5f, 2f, 5f, 12f), 3f, 3f, p)
        p.color = KEBAYA
        canvas.drawRoundRect(RectF(-24f, 10f, 24f, 48f), 13f, 13f, p)
        p.color = KEBAYA_SHADE
        canvas.drawRoundRect(RectF(-24f, 38f, 24f, 48f), 9f, 9f, p)
        // sleeves / arms
        p.color = KEBAYA
        canvas.drawRoundRect(RectF(-31f, 14f, -20f, 40f), 6f, 6f, p)
        canvas.drawRoundRect(RectF(20f, 14f, 31f, 40f), 6f, 6f, p)
        p.color = SKIN
        canvas.drawCircle(-25.5f, 42f, 4.6f, p)
        canvas.drawCircle(25.5f, 42f, 4.6f, p)
        // gold sash across the kebaya
        p.color = SASH
        val sash = Path().apply {
            moveTo(-20f, 16f); lineTo(14f, 46f); lineTo(20f, 41f); lineTo(-13f, 11f); close()
        }
        canvas.drawPath(sash, p)
        p.color = SASH_DEEP
        canvas.drawRect(RectF(-24f, 44f, 24f, 48f), p)

        // ---- head
        p.color = SKIN
        canvas.drawOval(RectF(-16f, -30f, 16f, 4f), p)
        // hair front (fringe + bun curve)
        p.color = HAIR
        val fringe = Path().apply {
            moveTo(-16.5f, -18f)
            cubicTo(-16f, -31f, 16f, -31f, 16.5f, -18f)
            cubicTo(10f, -23f, -10f, -23f, -16.5f, -18f)
            close()
        }
        canvas.drawPath(fringe, p)
        p.color = HAIR_LIGHT
        canvas.drawOval(RectF(-8f, -30f, 8f, -24f), p)

        // ---- frangipani flower behind the left ear
        p.color = FLOWER
        for (i in 0 until 5) {
            val a = (i * 72 - 90) * Math.PI.toFloat() / 180f
            val fx = -17f + cos(a) * 4.6f
            val fy = -14f + sin(a) * 4.6f
            canvas.drawCircle(fx, fy, 4.2f, p)
        }
        p.color = FLOWER_CORE
        canvas.drawCircle(-17f, -14f, 3.0f, p)

        // ---- cheeks + brows
        p.color = CHEEK
        canvas.drawCircle(-9.5f, -8f, 3.4f, p)
        canvas.drawCircle(9.5f, -8f, 3.4f, p)
        p.color = HAIR
        p.strokeWidth = 1.7f
        p.style = Paint.Style.STROKE
        canvas.drawLine(-11f, -17f, -4.5f, -19f, p)
        canvas.drawLine(4.5f, -19f, 11f, -17f, p)
        if (mood == ENCOURAGING) canvas.drawLine(-11f, -19.5f, -4.5f, -17.5f, p)
        p.style = Paint.Style.FILL

        // ---- eyes (blink every few seconds)
        val blink = (t % 4.3f) < 0.14f
        p.color = EYE
        if (blink) {
            p.strokeWidth = 2.4f
            p.style = Paint.Style.STROKE
            canvas.drawLine(-9f, -11.5f, -4f, -11.5f, p)
            canvas.drawLine(4f, -11.5f, 9f, -11.5f, p)
            p.style = Paint.Style.FILL
        } else if (mood == HAPPY) {
            p.strokeWidth = 2.6f
            p.style = Paint.Style.STROKE
            canvas.drawArc(RectF(-10f, -16f, -3f, -9f), 200f, 140f, false, p)
            canvas.drawArc(RectF(3f, -16f, 10f, -9f), 200f, 140f, false, p)
            p.style = Paint.Style.FILL
        } else {
            canvas.drawOval(RectF(-9.2f, -14.4f, -3.8f, -8.6f), p)
            canvas.drawOval(RectF(3.8f, -14.4f, 9.2f, -8.6f), p)
            p.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(-7.6f, -12.6f, 1.0f, p)
            canvas.drawCircle(5.4f, -12.6f, 1.0f, p)
        }

        // ---- mouth
        p.color = MOUTH
        p.strokeWidth = 2.2f
        p.style = Paint.Style.STROKE
        when (mood) {
            HAPPY -> {
                p.style = Paint.Style.FILL
                val mouth = Path().apply {
                    moveTo(-5.5f, -3.5f)
                    cubicTo(-2f, 2.5f, 2f, 2.5f, 5.5f, -3.5f)
                    cubicTo(2f, -0.5f, -2f, -0.5f, -5.5f, -3.5f)
                    close()
                }
                canvas.drawPath(mouth, p)
            }
            ENCOURAGING -> canvas.drawArc(RectF(-5f, -2f, 5f, 5f), 200f, 140f, false, p)
            else -> canvas.drawArc(RectF(-4.5f, -4.5f, 4.5f, 2.5f), 15f, 150f, false, p)
        }
        p.style = Paint.Style.FILL
        canvas.restore()
    }
}

/** Compose wrapper with the idle bob + blink animation. */
@Composable
fun BudayaAvatar(mood: Int, avatarSize: Dp = 64.dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "avatar")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "avatar-time"
    )
    ComposeCanvas(modifier.size(avatarSize)) {
        drawIntoCanvas { canvas ->
            AvatarArt.draw(
                canvas = canvas.nativeCanvas,
                cx = size.width / 2f,
                cy = size.height / 2f,
                size = size.minDimension,
                mood = mood,
                t = t
            )
        }
    }
}
