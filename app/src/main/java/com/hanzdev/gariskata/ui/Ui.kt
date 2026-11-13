package com.hanzdev.gariskata.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object Palette {
    val PAPER = 0xFFF7F1E3.toInt()
    val INK = 0xFF1E2430.toInt()
    val INK_SOFT = 0xFF4A5160.toInt()
    val BLUE = 0xFF28558A.toInt()
    val RED = 0xFFB5301F.toInt()
    val YELLOW = 0xFFF4C542.toInt()
    val YELLOW_PRESSED = 0xFFE3B02F.toInt()
    val LINE = 0xFFD8E3EF.toInt()
    val MARGIN = 0xFFE0A39A.toInt()
}

object Fonts {
    // Font "casual" bawaan Android berupa tulisan tangan, jadi tidak perlu berkas font tambahan.
    val hand: Typeface = Typeface.create("casual", Typeface.NORMAL)
    val handBold: Typeface = Typeface.create("casual", Typeface.BOLD)
    val body: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
}

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Float): Int = (v * resources.displayMetrics.density).roundToInt()
fun Context.spPx(v: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

fun View.dp(v: Float): Float = context.dp(v)

fun lp(
    w: Int, h: Int, weight: Float = 0f,
    l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0
): LinearLayout.LayoutParams = LinearLayout.LayoutParams(w, h, weight).apply { setMargins(l, t, r, b) }

fun Context.textView(
    text: CharSequence, sp: Float, color: Int = Palette.INK,
    hand: Boolean = true, center: Boolean = true
): TextView = TextView(this).apply {
    this.text = text
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    setTextColor(color)
    typeface = if (hand) Fonts.handBold else Fonts.body
    if (center) gravity = Gravity.CENTER_HORIZONTAL
}

fun Context.cardBackground(fill: Int = Palette.PAPER): GradientDrawable = GradientDrawable().apply {
    setColor(fill)
    cornerRadius = dp(12f)
    setStroke(dpi(2f), Palette.INK)
}

object Pencil {

    fun circle(cx: Float, cy: Float, r: Float, seed: Int): Path {
        val rnd = Random(seed)
        val phase = rnd.nextFloat() * 6.28f
        val start = rnd.nextFloat() * 6.28f
        val sweep = 2f * PI.toFloat() + 0.45f
        val steps = 32
        val path = Path()
        for (i in 0..steps) {
            val f = i / steps.toFloat()
            val a = start + sweep * f
            val rr = r * (1f + 0.04f * sin(a * 3f + phase) + 0.07f * f)
            val x = cx + rr * cos(a)
            val y = cy + rr * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, seed: Int, sag: Float = 0f): Path {
        val rnd = Random(seed)
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f + (rnd.nextFloat() - 0.5f) * sag
        return Path().apply {
            moveTo(x1, y1)
            quadTo(mx, my, x2, y2)
        }
    }

    fun cross(cx: Float, cy: Float, half: Float, seed: Int): Pair<Path, Path> {
        val rnd = Random(seed)
        val j = { (rnd.nextFloat() - 0.5f) * half * 0.25f }
        val a = Path().apply {
            moveTo(cx - half + j(), cy - half + j())
            lineTo(cx + half + j(), cy + half + j())
        }
        val b = Path().apply {
            moveTo(cx + half + j(), cy - half * 0.9f + j())
            lineTo(cx - half * 0.9f + j(), cy + half + j())
        }
        return a to b
    }

    fun partial(path: Path, fraction: Float): Path {
        if (fraction >= 1f) return path
        val out = Path()
        if (fraction <= 0f) return out
        val measure = PathMeasure(path, false)
        measure.getSegment(0f, measure.length * fraction, out, true)
        return out
    }
}

class PaperView(context: Context) : View(context) {

    // Garis buku tulis jadi motif utama: namanya "Garis Kata".
    private val ruled = Paint().apply {
        color = Palette.LINE
        strokeWidth = context.dp(1.5f)
    }
    private val margin = Paint().apply {
        color = Palette.MARGIN
        strokeWidth = context.dp(2f)
    }

    override fun onDraw(canvas: Canvas) {
        val gap = dp(34f)
        var y = gap
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, ruled)
            y += gap
        }
        canvas.drawLine(dp(40f), 0f, dp(40f), height.toFloat(), margin)
    }
}

class ScribbleLine(context: Context, private val color: Int, private val seed: Int) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dp(4f)
        color = this@ScribbleLine.color
    }

    override fun onDraw(canvas: Canvas) {
        val pad = dp(6f)
        canvas.drawPath(
            Pencil.line(pad, height * 0.6f, width - pad, height * 0.4f, seed, dp(10f)),
            paint
        )
    }
}
