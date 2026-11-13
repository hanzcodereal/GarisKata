package com.hanzdev.gariskata.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import com.hanzdev.gariskata.MainActivity
import com.hanzdev.gariskata.Sfx
import com.hanzdev.gariskata.game.GameState
import com.hanzdev.gariskata.game.GuessResult
import kotlin.math.PI
import kotlin.math.sin

class GameScreen(private val app: MainActivity, private val stage: Int) : Screen {

    private val puzzle = app.puzzles[stage - 1]
    private var state: GameState = restore()
    private val handler = Handler(Looper.getMainLooper())
    private val keys = LinkedHashMap<Char, LetterKey>()

    private val coinView = CoinView(app)
    private val sentenceView = app.textView(puzzle.sentence.replace(com.hanzdev.gariskata.data.Puzzle.BLANK, "_____"), 30f)
    private val infoView = app.textView("", 17f, Palette.INK_SOFT, hand = false).apply {
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private val answerView = AnswerView(app)
    private val mistakeText = app.textView("", 16f, Palette.INK, hand = false)
    private val tally = TallyView(app)
    private val kaca = HelperButton(app, HelperButton.Kind.KACA, "Kaca")
    private val bom = HelperButton(app, HelperButton.Kind.BOM, "Bom")
    private val lampu = HelperButton(app, HelperButton.Kind.LAMPU, "Lampu")

    private var dialog: Dialog? = null
    private var infoReset: Runnable? = null
    private var shaker: ValueAnimator? = null

    override val view: View = build()

    private fun restore(): GameState =
        app.store.progressFor(stage)?.let { GameState.decode(puzzle, it) } ?: GameState(puzzle)

    private fun build(): View {
        val column = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(app.dpi(12f), app.dpi(8f), app.dpi(12f), app.dpi(16f))
        }

        val top = LinearLayout(app).apply { gravity = Gravity.CENTER_VERTICAL }
        val menu = InkButton(app, false, compact = true).apply {
            text = "Menu"
            setOnClickListener { app.back() }
        }
        top.addView(menu, lp(WRAP, WRAP))
        top.addView(app.textView("Stage $stage", 26f), lp(0, WRAP, 1f))
        top.addView(coinView, lp(WRAP, WRAP))
        column.addView(top, lp(MATCH, WRAP))

        val sentenceBox = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = app.dpi(150f)
            setPadding(app.dpi(12f), app.dpi(8f), app.dpi(12f), app.dpi(8f))
        }
        sentenceBox.addView(sentenceView, lp(MATCH, WRAP))
        sentenceBox.addView(infoView, lp(MATCH, WRAP, t = app.dpi(10f)))
        column.addView(sentenceBox, lp(MATCH, 0, 1f))

        column.addView(answerView, lp(MATCH, WRAP, b = app.dpi(4f)))

        val status = LinearLayout(app).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL }
        status.addView(mistakeText, lp(WRAP, WRAP, r = app.dpi(10f)))
        status.addView(tally, lp(WRAP, WRAP))
        column.addView(status, lp(MATCH, WRAP, b = app.dpi(8f)))

        for (letters in listOf("ABCDEFG", "HIJKLMN", "OPQRSTU", "VWXYZ")) {
            val row = LinearLayout(app).apply {
                weightSum = 7f
                gravity = Gravity.CENTER_HORIZONTAL
            }
            for (c in letters) {
                val key = LetterKey(app, c)
                key.setOnClickListener { onLetter(c) }
                keys[c] = key
                row.addView(key, lp(0, app.dpi(52f), 1f, l = app.dpi(2f), t = app.dpi(3f), r = app.dpi(2f), b = app.dpi(3f)))
            }
            column.addView(row, lp(MATCH, WRAP))
        }

        kaca.priceLabel = KACA_COST.toString()
        kaca.contentDescription = "Kaca pembesar, ungkap satu huruf, biaya $KACA_COST koin"
        kaca.setOnClickListener { useReveal() }
        bom.priceLabel = BOM_COST.toString()
        bom.contentDescription = "Bom, singkirkan $BOM_REMOVES huruf yang tidak ada di kata, biaya $BOM_COST koin"
        bom.setOnClickListener { useBomb() }
        lampu.priceLabel = "Gratis"
        lampu.showCoin = false
        lampu.contentDescription = "Lampu, tampilkan petunjuk, gratis sekali tiap stage"
        lampu.setOnClickListener { useClue() }

        val helpers = LinearLayout(app).apply { weightSum = 3f }
        for (b in listOf(kaca, bom, lampu)) {
            helpers.addView(b, lp(0, app.dpi(96f), 1f, l = app.dpi(6f), r = app.dpi(6f)))
        }
        column.addView(helpers, lp(MATCH, WRAP, t = app.dpi(12f)))

        renderAll()
        return ScrollView(app).apply {
            isFillViewport = true
            clipToPadding = false
            addView(column)
        }
    }

    private fun renderAll() {
        for ((c, key) in keys) {
            key.setMark(markFor(c), false)
        }
        answerView.render(puzzle.answer, state.guessed, false, false)
        infoView.text = baseInfo()
        refreshStatus()
    }

    private fun markFor(c: Char): LetterKey.Mark = when (c) {
        in state.guessed -> LetterKey.Mark.RIGHT
        in state.wrong -> LetterKey.Mark.WRONG
        in state.removed -> LetterKey.Mark.REMOVED
        else -> LetterKey.Mark.NONE
    }

    private fun refreshStatus() {
        mistakeText.text = "Salah ${state.mistakes} dari ${GameState.MAX_MISTAKES}"
        tally.set(state.mistakes, GameState.MAX_MISTAKES)
        coinView.setValue(app.store.coins)
        for ((c, key) in keys) key.isEnabled = state.isOpen(c)
        kaca.dim = state.finished || app.store.coins < KACA_COST
        bom.dim = state.finished || app.store.coins < BOM_COST
        lampu.dim = state.finished || state.clueShown
    }

    private fun baseInfo(): String = when {
        state.clueShown -> "Petunjuk: ${puzzle.clue}."
        stage == 1 && state.guessed.isEmpty() && state.wrong.isEmpty() && state.removed.isEmpty() ->
            "Tekan huruf di bawah untuk menebak kata yang kosong."
        else -> ""
    }

    private fun showInfo(message: String) {
        infoView.text = message
        infoReset?.let { handler.removeCallbacks(it) }
        val reset = Runnable { infoView.text = baseInfo() }
        infoReset = reset
        handler.postDelayed(reset, 2600)
    }

    override fun onKey(letter: Char): Boolean {
        if (dialog != null) return false
        onLetter(letter)
        return true
    }

    private fun onLetter(c: Char) {
        val key = keys[c] ?: return
        val motion = app.motionOn()
        when (state.guess(c)) {
            GuessResult.IGNORED -> return
            GuessResult.RIGHT -> {
                key.setMark(LetterKey.Mark.RIGHT, motion)
                answerView.render(puzzle.answer, state.guessed, false, motion)
                app.sfx.play(Sfx.Sound.BENAR)
                app.haptic(key, true)
                showInfo("Huruf $c ada di kata.")
            }
            GuessResult.WRONG -> {
                key.setMark(LetterKey.Mark.WRONG, motion)
                app.sfx.play(Sfx.Sound.SALAH)
                app.haptic(key, false)
                shake()
                showInfo("Huruf $c tidak ada. Sisa kesempatan ${GameState.MAX_MISTAKES - state.mistakes}.")
            }
        }
        afterMove()
    }

    private fun useReveal() {
        if (state.finished) return
        if (app.store.coins < KACA_COST) {
            showInfo("Koin belum cukup. Butuh $KACA_COST koin.")
            return
        }
        val letter = state.revealOne() ?: return
        app.store.coins -= KACA_COST
        keys[letter]?.setMark(LetterKey.Mark.RIGHT, app.motionOn())
        answerView.render(puzzle.answer, state.guessed, false, app.motionOn())
        app.sfx.play(Sfx.Sound.UNGKAP)
        showInfo("Huruf $letter terungkap.")
        afterMove()
    }

    private fun useBomb() {
        if (state.finished) return
        if (app.store.coins < BOM_COST) {
            showInfo("Koin belum cukup. Butuh $BOM_COST koin.")
            return
        }
        val gone = state.removeWrong(BOM_REMOVES)
        if (gone.isEmpty()) {
            showInfo("Tidak ada lagi huruf yang bisa disingkirkan.")
            return
        }
        app.store.coins -= BOM_COST
        for (c in gone) keys[c]?.setMark(LetterKey.Mark.REMOVED, app.motionOn())
        app.sfx.play(Sfx.Sound.BOM)
        showInfo("${gone.size} huruf yang tidak ada di kata disingkirkan.")
        afterMove()
    }

    private fun useClue() {
        if (state.finished) return
        if (state.clueShown) {
            showInfo("Petunjuk sudah terbuka.")
            return
        }
        state.showClue()
        app.sfx.play(Sfx.Sound.PETUNJUK)
        infoView.text = baseInfo()
        infoReset?.let { handler.removeCallbacks(it) }
        afterMove()
    }

    private fun afterMove() {
        refreshStatus()
        when {
            state.solved -> win()
            state.lost -> lose()
            else -> app.store.saveProgress(stage, state.encode())
        }
    }

    private fun win() {
        app.store.clearProgress()
        val first = stage == app.store.cleared + 1
        val flawless = state.mistakes == 0
        val reward = if (first) FIRST_CLEAR + (if (flawless) FLAWLESS_BONUS else 0) else 0
        if (first) {
            app.store.markCleared(stage)
            app.store.coins += reward
        }
        app.sfx.play(Sfx.Sound.MENANG)
        app.haptic(view, true)
        handler.postDelayed({ showWin(first, flawless, reward) }, if (app.motionOn()) 700L else 150L)
    }

    private fun lose() {
        app.store.clearProgress()
        answerView.render(puzzle.answer, state.guessed, true, false)
        app.sfx.play(Sfx.Sound.KALAH)
        app.haptic(view, false)
        handler.postDelayed({ showLose() }, if (app.motionOn()) 900L else 150L)
    }

    private fun showWin(first: Boolean, flawless: Boolean, reward: Int) {
        val last = stage >= app.puzzles.size
        val box = dialogBox()
        box.addView(app.textView("Benar!", 40f, Palette.BLUE))
        box.addView(app.textView(puzzle.filled(), 24f), lp(MATCH, WRAP, t = app.dpi(12f)))
        val rewardText = when {
            first && flawless -> "Hadiah $FIRST_CLEAR koin ditambah bonus $FLAWLESS_BONUS koin karena tanpa salah."
            first -> "Hadiah $FIRST_CLEAR koin."
            else -> "Stage ini sudah pernah selesai, jadi tidak ada koin tambahan."
        }
        box.addView(app.textView(rewardText, 16f, Palette.INK_SOFT, hand = false), lp(MATCH, WRAP, t = app.dpi(12f)))
        if (last) {
            box.addView(
                app.textView("Semua stage sudah selesai. Stage baru belum tersedia.", 16f, Palette.INK_SOFT, hand = false),
                lp(MATCH, WRAP, t = app.dpi(8f))
            )
        }
        val next = InkButton(app, true).apply {
            text = if (last) "Kembali ke beranda" else "Lanjut ke Stage ${stage + 1}"
            setOnClickListener {
                dismissDialog()
                if (last) app.back() else app.replace(GameScreen(app, stage + 1))
            }
        }
        val menu = InkButton(app, false).apply {
            text = "Menu"
            setOnClickListener {
                dismissDialog()
                app.back()
            }
        }
        box.addView(next, lp(MATCH, WRAP, t = app.dpi(20f)))
        box.addView(menu, lp(MATCH, WRAP, t = app.dpi(10f)))
        present(box, next) { app.back() }
        if (reward > 0) {
            handler.postDelayed({
                app.sfx.play(Sfx.Sound.KOIN)
                coinView.animateTo(app.store.coins, app.motionOn())
            }, 350)
        }
    }

    private fun showLose() {
        val box = dialogBox()
        box.addView(app.textView("Kesempatan habis", 32f, Palette.RED))
        box.addView(app.textView("Jawabannya: ${puzzle.answer}", 24f), lp(MATCH, WRAP, t = app.dpi(12f)))
        box.addView(app.textView(puzzle.filled(), 18f, Palette.INK_SOFT, hand = false), lp(MATCH, WRAP, t = app.dpi(8f)))
        val retry = InkButton(app, true).apply {
            text = "Ulangi Stage $stage"
            setOnClickListener {
                dismissDialog()
                state = GameState(puzzle)
                renderAll()
            }
        }
        val menu = InkButton(app, false).apply {
            text = "Menu"
            setOnClickListener {
                dismissDialog()
                app.back()
            }
        }
        box.addView(retry, lp(MATCH, WRAP, t = app.dpi(20f)))
        box.addView(menu, lp(MATCH, WRAP, t = app.dpi(10f)))
        present(box, retry) { app.back() }
    }

    private fun dialogBox(): LinearLayout = LinearLayout(app).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = app.cardBackground()
        setPadding(app.dpi(24f), app.dpi(24f), app.dpi(24f), app.dpi(24f))
    }

    private fun present(content: View, focus: View, onCancel: () -> Unit) {
        val d = Dialog(app)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(content)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((app.resources.displayMetrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        d.setCanceledOnTouchOutside(false)
        d.setOnCancelListener {
            dialog = null
            onCancel()
        }
        dialog = d
        d.show()
        focus.requestFocus()
    }

    private fun dismissDialog() {
        dialog?.setOnCancelListener(null)
        dialog?.dismiss()
        dialog = null
    }

    // Guncangan teredam, berhenti tepat di posisi 0 supaya jawaban tidak tertinggal miring.
    private fun shake() {
        if (!app.motionOn()) return
        shaker?.cancel()
        val amplitude = app.dp(10f)
        shaker = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 380
            addUpdateListener {
                val f = it.animatedValue as Float
                val x = amplitude * (1f - f) * (1f - f) * sin(f * 6f * PI.toFloat())
                answerView.translationX = x
                sentenceView.translationX = x
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    answerView.translationX = 0f
                    sentenceView.translationX = 0f
                }
            })
            start()
        }
    }

    override fun onHidden() {
        handler.removeCallbacksAndMessages(null)
        shaker?.cancel()
        dismissDialog()
    }

    companion object {
        private const val KACA_COST = 30
        private const val BOM_COST = 50
        private const val BOM_REMOVES = 5
        private const val FIRST_CLEAR = 20
        private const val FLAWLESS_BONUS = 10
    }
}
