package com.hanzdev.gariskata.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    this.color = color
    strokeWidth = width
}

private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    this.color = color
}

// Tombol menu. Utama berwarna stabilo kuning, sekunder hanya berbingkai tinta.
class InkButton(context: Context, private val primary: Boolean, compact: Boolean = false) : Button(context) {

    private val fill = fillPaint(Palette.PAPER)
    private val border = strokePaint(Palette.INK, context.dp(2f))
    private val focusRing = strokePaint(Palette.BLUE, context.dp(3f))
    private val box = RectF()

    init {
        background = null
        stateListAnimator = null
        isAllCaps = false
        typeface = Fonts.handBold
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (compact) 17f else 21f)
        setTextColor(Palette.INK)
        gravity = Gravity.CENTER
        val minH = context.dpi(if (compact) 48f else 56f)
        minHeight = minH
        minimumHeight = minH
        minWidth = 0
        minimumWidth = 0
        setPadding(context.dpi(18f), context.dpi(8f), context.dpi(18f), context.dpi(8f))
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        val inset = dp(3f)
        val drop = if (isPressed) dp(2f) else 0f
        fill.color = when {
            primary && isPressed -> Palette.YELLOW_PRESSED
            primary -> Palette.YELLOW
            isPressed -> Palette.LINE
            else -> Palette.PAPER
        }
        canvas.save()
        canvas.translate(0f, drop)
        box.set(inset, inset, width - inset, height - inset)
        canvas.drawRoundRect(box, dp(10f), dp(10f), fill)
        canvas.drawRoundRect(box, dp(10f), dp(10f), border)
        super.onDraw(canvas)
        canvas.restore()
        if (isFocused) {
            box.inset(-dp(1f), -dp(1f))
            canvas.drawRoundRect(box, dp(11f), dp(11f), focusRing)
        }
    }
}

// Satu huruf di papan. Huruf benar dilingkari biru, huruf salah dicoret merah.
class LetterKey(context: Context, val letter: Char) : Button(context) {

    enum class Mark { NONE, RIGHT, WRONG, REMOVED }

    private var mark = Mark.NONE
    private var progress = 1f
    private var animator: ValueAnimator? = null
    private var ring = Path()
    private var strokeA = Path()
    private var strokeB = Path()
    private val ringPaint = strokePaint(Palette.BLUE, context.dp(3f))
    private val crossPaint = strokePaint(Palette.RED, context.dp(3f))
    private val focusPaint = strokePaint(Palette.INK, context.dp(2f))
    private val pressPaint = fillPaint(Palette.LINE)
    private val box = RectF()

    init {
        background = null
        stateListAnimator = null
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(0, 0, 0, 0)
        includeFontPadding = false
        gravity = Gravity.CENTER
        typeface = Fonts.handBold
        text = letter.toString()
        setAutoSizeTextTypeUniformWithConfiguration(14, 30, 1, TypedValue.COMPLEX_UNIT_SP)
        setTextColor(Palette.INK)
        contentDescription = "Huruf $letter"
    }

    fun setMark(newMark: Mark, animate: Boolean) {
        if (newMark == mark) return
        animator?.cancel()
        mark = newMark
        setTextColor(
            when (newMark) {
                Mark.RIGHT -> Palette.BLUE
                Mark.WRONG, Mark.REMOVED -> Palette.INK_SOFT
                Mark.NONE -> Palette.INK
            }
        )
        contentDescription = "Huruf $letter" + when (newMark) {
            Mark.RIGHT -> ", ada di kata"
            Mark.WRONG -> ", tidak ada di kata"
            Mark.REMOVED -> ", disingkirkan"
            Mark.NONE -> ""
        }
        if (newMark == Mark.NONE || !animate) {
            progress = 1f
            invalidate()
            return
        }
        progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) * 0.44f
        ring = Pencil.circle(cx, cy, r, letter.code)
        val (a, b) = Pencil.cross(cx, cy, r * 0.62f, letter.code)
        strokeA = a
        strokeB = b
    }

    override fun onDraw(canvas: Canvas) {
        if (isPressed && isEnabled) {
            canvas.drawCircle(width / 2f, height / 2f, min(width, height) * 0.42f, pressPaint)
        }
        super.onDraw(canvas)
        when (mark) {
            Mark.RIGHT -> canvas.drawPath(Pencil.partial(ring, progress), ringPaint)
            Mark.WRONG, Mark.REMOVED -> {
                canvas.drawPath(Pencil.partial(strokeA, min(progress * 2f, 1f)), crossPaint)
                canvas.drawPath(Pencil.partial(strokeB, max(progress * 2f - 1f, 0f)), crossPaint)
            }
            Mark.NONE -> Unit
        }
        if (isFocused) {
            box.set(dp(2f), dp(2f), width - dp(2f), height - dp(2f))
            canvas.drawRoundRect(box, dp(10f), dp(10f), focusPaint)
        }
    }
}

// Tombol bantuan: kaca pembesar, bom, dan lampu. Ikonnya digambar dengan kode.
class HelperButton(context: Context, private val kind: Kind, private val title: String) : Button(context) {

    enum class Kind { KACA, BOM, LAMPU }

    var priceLabel: String = ""
        set(value) { field = value; invalidate() }
    var showCoin: Boolean = true
        set(value) { field = value; invalidate() }
    var dim: Boolean = false
        set(value) { field = value; alpha = if (value) 0.55f else 1f; invalidate() }

    private val fill = fillPaint(Palette.PAPER)
    private val border = strokePaint(Palette.INK, context.dp(2f))
    private val focusRing = strokePaint(Palette.BLUE, context.dp(3f))
    private val ink = strokePaint(Palette.INK, context.dp(3f))
    private val inkThin = strokePaint(Palette.INK, context.dp(2f))
    private val blue = strokePaint(Palette.BLUE, context.dp(4f))
    private val red = strokePaint(Palette.RED, context.dp(2f))
    private val yellow = fillPaint(Palette.YELLOW)
    private val inkFill = fillPaint(Palette.INK)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.handBold
        color = Palette.INK
        textAlign = Paint.Align.CENTER
        textSize = context.spPx(17f)
    }
    private val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.body
        color = Palette.INK
        textAlign = Paint.Align.LEFT
        textSize = context.spPx(14f)
    }
    private val box = RectF()

    init {
        background = null
        stateListAnimator = null
        isAllCaps = false
        text = ""
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.save()
        if (isPressed) canvas.translate(0f, dp(2f))
        fill.color = if (isPressed) Palette.LINE else Palette.PAPER
        box.set(dp(3f), dp(3f), w - dp(3f), h - dp(3f))
        canvas.drawRoundRect(box, dp(12f), dp(12f), fill)
        canvas.drawRoundRect(box, dp(12f), dp(12f), border)

        val cx = w / 2f
        drawIcon(canvas, cx, h * 0.31f)

        while (labelPaint.measureText(title) > w * 0.86f && labelPaint.textSize > 10f) {
            labelPaint.textSize -= 1f
        }
        canvas.drawText(title, cx, h * 0.64f, labelPaint)

        val fm = pricePaint.fontMetrics
        val baseline = h * 0.84f - (fm.ascent + fm.descent) / 2f
        val textW = pricePaint.measureText(priceLabel)
        val coinR = dp(7f)
        val total = if (showCoin) coinR * 2f + dp(5f) + textW else textW
        var x = cx - total / 2f
        if (showCoin) {
            canvas.drawCircle(x + coinR, h * 0.84f, coinR, yellow)
            canvas.drawCircle(x + coinR, h * 0.84f, coinR, inkThin)
            x += coinR * 2f + dp(5f)
        }
        canvas.drawText(priceLabel, x, baseline, pricePaint)
        canvas.restore()

        if (isFocused) {
            box.inset(-dp(1f), -dp(1f))
            canvas.drawRoundRect(box, dp(13f), dp(13f), focusRing)
        }
    }

    private fun drawIcon(c: Canvas, cx: Float, cy: Float) {
        when (kind) {
            Kind.KACA -> {
                c.drawCircle(cx - dp(3f), cy - dp(3f), dp(9f), ink)
                c.drawLine(cx + dp(4f), cy + dp(4f), cx + dp(13f), cy + dp(13f), blue)
            }
            Kind.BOM -> {
                c.drawCircle(cx - dp(2f), cy + dp(3f), dp(10f), inkFill)
                c.drawLine(cx + dp(5f), cy - dp(4f), cx + dp(10f), cy - dp(10f), inkThin)
                c.drawLine(cx + dp(10f), cy - dp(14f), cx + dp(10f), cy - dp(11f), red)
                c.drawLine(cx + dp(13f), cy - dp(11f), cx + dp(16f), cy - dp(12f), red)
            }
            Kind.LAMPU -> {
                c.drawCircle(cx, cy - dp(3f), dp(10f), yellow)
                c.drawCircle(cx, cy - dp(3f), dp(10f), inkThin)
                c.drawRect(cx - dp(5f), cy + dp(7f), cx + dp(5f), cy + dp(12f), inkFill)
                c.drawLine(cx - dp(15f), cy - dp(3f), cx - dp(18f), cy - dp(3f), inkThin)
                c.drawLine(cx + dp(15f), cy - dp(3f), cx + dp(18f), cy - dp(3f), inkThin)
            }
        }
    }
}

// Deretan garis jawaban. Huruf muncul dengan efek membal kecil saat terbuka.
class AnswerView(context: Context) : View(context) {

    private var answer = ""
    private var shown = emptySet<Int>()
    private var popping = emptySet<Int>()
    private var revealAll = false
    private var popT = 1f
    private var popAnim: ValueAnimator? = null

    private val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.handBold
        textAlign = Paint.Align.CENTER
    }
    private val slotPaint = strokePaint(Palette.INK, context.dp(3f))
    private val gap = context.dp(6f)
    private val maxSlot = context.dp(46f)
    private val minSlot = context.dp(26f)
    private val rowH = context.dp(64f)

    fun render(answer: String, guessed: Set<Char>, revealAll: Boolean, animate: Boolean) {
        val sameWord = answer == this.answer
        val now = answer.indices.filter { answer[it] in guessed }.toSet()
        val fresh = now - shown
        this.answer = answer
        this.revealAll = revealAll
        shown = now
        contentDescription = "Jawaban, ${answer.length} huruf: " +
            answer.indices.joinToString(", ") { if (it in now || revealAll) answer[it].toString() else "kosong" }
        if (!sameWord) requestLayout()
        popAnim?.cancel()
        if (animate && sameWord && fresh.isNotEmpty()) {
            popping = fresh
            popAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 320
                addUpdateListener {
                    popT = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) {
                        popping = emptySet()
                        invalidate()
                    }
                })
                start()
            }
        } else {
            popping = emptySet()
            invalidate()
        }
    }

    private fun perRow(w: Float): Int {
        val n = max(answer.length, 1)
        val fitAll = (w - gap * (n - 1)) / n
        return if (fitAll >= minSlot) n else ceil(n / 2f).toInt()
    }

    private fun slot(w: Float, perRow: Int): Float =
        min(maxSlot, (w - gap * (perRow - 1)) / perRow)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val rows = ceil(max(answer.length, 1).toFloat() / perRow(w.toFloat())).toInt()
        setMeasuredDimension(w, (rows * rowH + dp(8f)).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val per = perRow(w)
        val slot = slot(w, per)
        letterPaint.textSize = min(context.spPx(38f), slot * 1.1f)
        for (i in answer.indices) {
            val row = i / per
            val col = i % per
            val inRow = min(per, answer.length - row * per)
            val rowW = inRow * slot + (inRow - 1) * gap
            val x = (w - rowW) / 2f + col * (slot + gap)
            val y = row * rowH + rowH - dp(10f)
            canvas.drawPath(Pencil.line(x, y, x + slot, y, i * 31 + 7, dp(5f)), slotPaint)
            val visible = i in shown || revealAll
            if (!visible) continue
            letterPaint.color = if (i in shown) Palette.BLUE else Palette.RED
            canvas.save()
            if (i in popping) {
                val s = 0.4f + 0.6f * OvershootInterpolator(2f).getInterpolation(popT)
                canvas.scale(s, s, x + slot / 2f, y - letterPaint.textSize * 0.35f)
            }
            canvas.drawText(answer[i].toString(), x + slot / 2f, y - dp(7f), letterPaint)
            canvas.restore()
        }
    }
}

// Enam goresan kesempatan. Yang terpakai ditebalkan merah.
class TallyView(context: Context) : View(context) {

    private var mistakes = 0
    private var max = 6
    private val used = strokePaint(Palette.RED, context.dp(4f))
    private val free = strokePaint(Palette.INK_SOFT, context.dp(2f))

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun set(mistakes: Int, max: Int) {
        this.mistakes = mistakes
        this.max = max
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((max * dp(16f) + dp(8f)).toInt(), dp(30f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        for (i in 0 until max) {
            val x = dp(8f) + i * dp(16f)
            val paint = if (i < mistakes) used else free
            canvas.drawPath(Pencil.line(x, dp(4f), x + dp(1f), height - dp(4f), i + 3, dp(4f)), paint)
        }
    }
}

// Koin: lingkaran stabilo kuning bertanda K, diikuti jumlahnya.
class CoinView(context: Context) : View(context) {

    var value = 0
        private set
    private var anim: ValueAnimator? = null
    private val coin = fillPaint(Palette.YELLOW)
    private val coinEdge = strokePaint(Palette.INK, context.dp(2f))
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.handBold
        color = Palette.INK
        textAlign = Paint.Align.CENTER
        textSize = context.spPx(17f)
    }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.handBold
        color = Palette.INK
        textSize = context.spPx(22f)
    }

    init {
        contentDescription = "Koin: 0"
    }

    fun setValue(v: Int) {
        anim?.cancel()
        value = v
        contentDescription = "Koin: $v"
        requestLayout()
        invalidate()
    }

    fun animateTo(target: Int, enabled: Boolean) {
        if (!enabled || target == value) {
            setValue(target)
            return
        }
        anim?.cancel()
        anim = ValueAnimator.ofInt(value, target).apply {
            duration = 600
            addUpdateListener {
                value = it.animatedValue as Int
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    setValue(target)
                }
            })
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val digits = max(4, value.toString().length)
        val w = dp(36f) + number.measureText("0".repeat(digits))
        setMeasuredDimension(resolveSize(w.toInt(), widthMeasureSpec), dp(44f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val cy = height / 2f
        canvas.drawCircle(dp(16f), cy, dp(14f), coin)
        canvas.drawCircle(dp(16f), cy, dp(14f), coinEdge)
        val mf = mark.fontMetrics
        canvas.drawText("K", dp(16f), cy - (mf.ascent + mf.descent) / 2f, mark)
        val nf = number.fontMetrics
        canvas.drawText(value.toString(), dp(36f), cy - (nf.ascent + nf.descent) / 2f, number)
    }
}

// Sel nomor stage. Selesai dilingkari biru, berikutnya digarisbawahi merah, terkunci putus-putus.
class StageCell(context: Context, private val number: Int, private val status: Status) : Button(context) {

    enum class Status { LOCKED, NEXT, DONE }

    private val border = strokePaint(Palette.INK, context.dp(2f))
    private val dashed = strokePaint(Palette.INK_SOFT, context.dp(2f)).apply {
        pathEffect = DashPathEffect(floatArrayOf(context.dp(5f), context.dp(4f)), 0f)
    }
    private val fill = fillPaint(Palette.PAPER)
    private val ringPaint = strokePaint(Palette.BLUE, context.dp(3f))
    private val underline = strokePaint(Palette.RED, context.dp(3f))
    private val focusRing = strokePaint(Palette.BLUE, context.dp(3f))
    private val box = RectF()

    init {
        background = null
        stateListAnimator = null
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(0, 0, 0, 0)
        gravity = Gravity.CENTER
        typeface = Fonts.handBold
        text = number.toString()
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        setTextColor(if (status == Status.LOCKED) Palette.INK_SOFT else Palette.INK)
        isEnabled = status != Status.LOCKED
        contentDescription = "Stage $number, " + when (status) {
            Status.LOCKED -> "terkunci"
            Status.NEXT -> "belum selesai, bisa dimainkan"
            Status.DONE -> "sudah selesai"
        }
    }

    override fun onDraw(canvas: Canvas) {
        box.set(dp(3f), dp(3f), width - dp(3f), height - dp(3f))
        fill.color = if (isPressed) Palette.LINE else Palette.PAPER
        canvas.drawRoundRect(box, dp(10f), dp(10f), fill)
        canvas.drawRoundRect(box, dp(10f), dp(10f), if (status == Status.LOCKED) dashed else border)
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        when (status) {
            Status.DONE -> canvas.drawPath(Pencil.circle(cx, cy, min(width, height) * 0.3f, number), ringPaint)
            Status.NEXT -> canvas.drawPath(
                Pencil.line(cx - dp(13f), height * 0.78f, cx + dp(13f), height * 0.76f, number, dp(4f)), underline
            )
            Status.LOCKED -> Unit
        }
        if (isFocused) {
            box.inset(-dp(1f), -dp(1f))
            canvas.drawRoundRect(box, dp(11f), dp(11f), focusRing)
        }
    }
}
