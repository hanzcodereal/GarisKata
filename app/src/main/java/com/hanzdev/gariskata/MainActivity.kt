package com.hanzdev.gariskata

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import com.hanzdev.gariskata.data.Puzzle
import com.hanzdev.gariskata.data.PuzzleRepo
import com.hanzdev.gariskata.data.SaveStore
import com.hanzdev.gariskata.ui.HomeScreen
import com.hanzdev.gariskata.ui.MATCH
import com.hanzdev.gariskata.ui.PaperView
import com.hanzdev.gariskata.ui.Screen

class MainActivity : Activity() {

    lateinit var store: SaveStore
        private set
    lateinit var sfx: Sfx
        private set
    var puzzles: List<Puzzle> = emptyList()
        private set

    private lateinit var host: FrameLayout
    private val stack = ArrayList<Screen>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SaveStore(this)
        sfx = Sfx(this).also { it.enabled = store.soundOn }
        puzzles = PuzzleRepo.load(this)

        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)

        host = FrameLayout(this)
        val root = FrameLayout(this)
        root.addView(PaperView(this), FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(host, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)

        root.setOnApplyWindowInsetsListener { _, insets ->
            applyInsets(insets)
            insets
        }
        useDarkBarIcons()
        go(HomeScreen(this))
    }

    @Suppress("DEPRECATION")
    private fun applyInsets(insets: WindowInsets) {
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            host.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        } else {
            host.setPadding(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom
            )
        }
    }

    // Latar kertas terang, jadi ikon status dan navigasi harus gelap.
    @Suppress("DEPRECATION")
    private fun useDarkBarIcons() {
        if (Build.VERSION.SDK_INT >= 30) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(mask, mask)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    fun go(screen: Screen) {
        stack.lastOrNull()?.onHidden()
        stack.add(screen)
        show(screen)
        if (stack.size > 1) sfx.play(Sfx.Sound.TAP, vary = true)
    }

    fun replace(screen: Screen) {
        stack.lastOrNull()?.onHidden()
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(screen)
        show(screen)
    }

    fun back() {
        if (stack.size <= 1) {
            finish()
            return
        }
        stack.removeAt(stack.lastIndex).onHidden()
        show(stack.last())
        sfx.play(Sfx.Sound.TAP, vary = true)
    }

    private fun show(screen: Screen) {
        host.removeAllViews()
        host.addView(screen.view, FrameLayout.LayoutParams(MATCH, MATCH))
        screen.onShown()
    }

    // Animasi dihentikan bila pemain memilih kurangi gerakan atau skala animasi sistem nol.
    fun motionOn(): Boolean {
        if (store.calmMotion) return false
        return Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    fun haptic(view: View, positive: Boolean) {
        if (!store.hapticOn) return
        val type = if (Build.VERSION.SDK_INT >= 30) {
            if (positive) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        } else {
            if (positive) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(type)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (stack.lastOrNull()?.onBack() == true) return
        back()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
            val letter = 'A' + (keyCode - KeyEvent.KEYCODE_A)
            if (stack.lastOrNull()?.onKey(letter) == true) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        stack.lastOrNull()?.onHidden()
        sfx.release()
        super.onDestroy()
    }
}

