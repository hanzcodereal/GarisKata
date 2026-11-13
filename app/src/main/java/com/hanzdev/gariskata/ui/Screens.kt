package com.hanzdev.gariskata.ui

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import com.hanzdev.gariskata.MainActivity
import com.hanzdev.gariskata.data.SaveStore
import kotlin.math.min

interface Screen {
    val view: View
    fun onShown() {}
    fun onHidden() {}
    fun onKey(letter: Char): Boolean = false
    fun onBack(): Boolean = false
}

// Kerangka layar sekunder: tombol kembali, judul, lalu isi yang bisa digulir.
private fun scaffold(app: MainActivity, title: String, content: View): View {
    val column = LinearLayout(app).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(app.dpi(16f), app.dpi(12f), app.dpi(16f), app.dpi(24f))
    }
    val top = LinearLayout(app).apply { gravity = Gravity.CENTER_VERTICAL }
    val back = InkButton(app, false, compact = true).apply {
        text = "Kembali"
        setOnClickListener { app.back() }
    }
    top.addView(back, lp(WRAP, WRAP))
    top.addView(app.textView(title, 26f), lp(0, WRAP, 1f, l = app.dpi(8f), r = app.dpi(8f)))
    column.addView(top, lp(MATCH, WRAP, b = app.dpi(16f)))
    column.addView(content, lp(MATCH, WRAP))
    return ScrollView(app).apply {
        isFillViewport = true
        clipToPadding = false
        addView(column)
    }
}

class HomeScreen(private val app: MainActivity) : Screen {

    private val coin = CoinView(app)
    private val play = InkButton(app, true)
    private val status = app.textView("", 16f, Palette.INK_SOFT, hand = false)
    override val view: View = build()

    private fun build(): View {
        val width = min(app.resources.displayMetrics.widthPixels - app.dpi(48f), app.dpi(380f))
        val column = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(app.dpi(24f), app.dpi(24f), app.dpi(24f), app.dpi(24f))
        }
        column.addView(View(app), lp(MATCH, 0, 1f))
        column.addView(app.textView("Garis Kata", 56f))
        column.addView(ScribbleLine(app, Palette.RED, 5), lp(app.dpi(220f), app.dpi(18f)))
        column.addView(
            app.textView("Tebak kata yang hilang dari kalimat, satu huruf demi satu huruf.", 17f, Palette.INK_SOFT, hand = false),
            lp(width, WRAP, t = app.dpi(14f))
        )
        column.addView(View(app), lp(MATCH, 0, 1f))

        val coinRow = LinearLayout(app).apply { gravity = Gravity.CENTER_VERTICAL }
        coinRow.addView(app.textView("Koinmu", 18f, hand = false), lp(WRAP, WRAP, r = app.dpi(8f)))
        coinRow.addView(coin, lp(WRAP, WRAP))
        column.addView(coinRow, lp(WRAP, WRAP, b = app.dpi(8f)))
        column.addView(status, lp(width, WRAP, b = app.dpi(16f)))

        play.setOnClickListener {
            val total = app.puzzles.size
            val next = if (app.store.cleared >= total) 1 else app.store.cleared + 1
            app.go(GameScreen(app, next))
        }
        column.addView(play, lp(width, WRAP, b = app.dpi(12f)))
        column.addView(menuButton("Pilih Stage") { app.go(StageSelectScreen(app)) }, lp(width, WRAP, b = app.dpi(12f)))
        column.addView(menuButton("Pengaturan") { app.go(SettingsScreen(app)) }, lp(width, WRAP, b = app.dpi(12f)))
        column.addView(menuButton("Tentang dan Dukung") { app.go(AboutScreen(app)) }, lp(width, WRAP))
        column.addView(View(app), lp(MATCH, 0, 0.5f))

        return ScrollView(app).apply {
            isFillViewport = true
            addView(column)
        }
    }

    private fun menuButton(label: String, action: () -> Unit) = InkButton(app, false).apply {
        text = label
        setOnClickListener { action() }
    }

    override fun onShown() {
        val total = app.puzzles.size
        val cleared = app.store.cleared.coerceAtMost(total)
        coin.setValue(app.store.coins)
        status.setTextColor(Palette.INK_SOFT)
        when {
            total == 0 -> {
                play.text = "Data stage tidak terbaca"
                play.isEnabled = false
                status.text = "Pasang ulang aplikasi untuk memulihkan data stage."
                status.setTextColor(Palette.RED)
            }
            cleared >= total -> {
                play.text = "Ulangi dari Stage 1"
                status.text = "Semua $total stage sudah selesai. Stage baru belum tersedia."
            }
            cleared == 0 -> {
                play.text = "Mulai Stage 1"
                status.text = "$total stage tersedia."
            }
            else -> {
                play.text = "Lanjut ke Stage ${cleared + 1}"
                status.text = "Selesai $cleared dari $total stage."
            }
        }
    }
}

class StageSelectScreen(private val app: MainActivity) : Screen {

    private val grid = LinearLayout(app).apply { orientation = LinearLayout.VERTICAL }
    private val summary = app.textView("", 16f, Palette.INK_SOFT, hand = false)
    override val view: View = build()

    private fun build(): View {
        val content = LinearLayout(app).apply { orientation = LinearLayout.VERTICAL }
        content.addView(summary, lp(MATCH, WRAP, b = app.dpi(12f)))
        content.addView(grid, lp(MATCH, WRAP))
        return scaffold(app, "Pilih Stage", content)
    }

    override fun onShown() {
        val total = app.puzzles.size
        val cleared = app.store.cleared.coerceAtMost(total)
        grid.removeAllViews()
        if (total == 0) {
            summary.text = "Data stage tidak terbaca. Pasang ulang aplikasi."
            return
        }
        summary.text = "Selesai $cleared dari $total stage. Stage berikutnya terbuka setelah yang ini selesai."
        val columns = 5
        var stage = 1
        while (stage <= total) {
            val row = LinearLayout(app).apply { weightSum = columns.toFloat() }
            for (i in 0 until columns) {
                val n = stage + i
                if (n > total) {
                    row.addView(View(app), lp(0, app.dpi(56f), 1f, l = app.dpi(4f), r = app.dpi(4f)))
                    continue
                }
                val status = when {
                    n <= cleared -> StageCell.Status.DONE
                    n == cleared + 1 -> StageCell.Status.NEXT
                    else -> StageCell.Status.LOCKED
                }
                val cell = StageCell(app, n, status).apply { setOnClickListener { app.go(GameScreen(app, n)) } }
                row.addView(cell, lp(0, app.dpi(56f), 1f, l = app.dpi(4f), r = app.dpi(4f)))
            }
            grid.addView(row, lp(MATCH, WRAP, t = app.dpi(4f), b = app.dpi(4f)))
            stage += columns
        }
    }
}

class SettingsScreen(private val app: MainActivity) : Screen {

    private val status = app.textView("", 16f, Palette.BLUE, hand = false)
    override val view: View = build()

    private fun build(): View {
        val content = LinearLayout(app).apply { orientation = LinearLayout.VERTICAL }
        content.addView(
            toggle("Suara", "Efek suara saat menebak, memakai bantuan, dan menyelesaikan stage.", app.store.soundOn) {
                app.store.soundOn = it
                app.sfx.enabled = it
            },
            lp(MATCH, WRAP, b = app.dpi(12f))
        )
        content.addView(
            toggle("Getar", "Getaran singkat saat huruf benar atau salah.", app.store.hapticOn) {
                app.store.hapticOn = it
            },
            lp(MATCH, WRAP, b = app.dpi(12f))
        )
        content.addView(
            toggle("Kurangi gerakan", "Matikan guncangan layar dan animasi coretan pensil.", app.store.calmMotion) {
                app.store.calmMotion = it
            },
            lp(MATCH, WRAP, b = app.dpi(24f))
        )
        val reset = InkButton(app, false).apply {
            text = "Hapus progres dan koin"
            setOnClickListener { confirmReset() }
        }
        content.addView(reset, lp(MATCH, WRAP, b = app.dpi(12f)))
        content.addView(status, lp(MATCH, WRAP))
        return scaffold(app, "Pengaturan", content)
    }

    private fun toggle(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val card = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            background = app.cardBackground()
            setPadding(app.dpi(16f), app.dpi(10f), app.dpi(16f), app.dpi(12f))
        }
        val sw = Switch(app).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(Palette.INK)
            typeface = Fonts.handBold
            minHeight = app.dpi(48f)
            isChecked = checked
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
        card.addView(sw, lp(MATCH, WRAP))
        card.addView(app.textView(hint, 15f, Palette.INK_SOFT, hand = false, center = false), lp(MATCH, WRAP))
        return card
    }

    private fun confirmReset() {
        AlertDialog.Builder(app)
            .setTitle("Hapus progres?")
            .setMessage("Semua stage terkunci lagi dan koin kembali ke ${SaveStore.START_COINS}. Ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                app.store.resetGame()
                status.text = "Progres dan koin sudah direset."
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}

class AboutScreen(private val app: MainActivity) : Screen {

    private val status = app.textView("", 16f, Palette.RED, hand = false)
    override val view: View = build()

    private fun build(): View {
        val version = (try {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }) ?: "tidak diketahui"

        val content = LinearLayout(app).apply { orientation = LinearLayout.VERTICAL }
        val card = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            background = app.cardBackground()
            setPadding(app.dpi(16f), app.dpi(16f), app.dpi(16f), app.dpi(16f))
        }
        card.addView(app.textView("Garis Kata", 28f, center = false), lp(MATCH, WRAP))
        card.addView(app.textView("Versi $version", 15f, Palette.INK_SOFT, hand = false, center = false), lp(MATCH, WRAP, b = app.dpi(12f)))
        card.addView(info("Pengembang", "Hanz Dev Official"), lp(MATCH, WRAP, b = app.dpi(8f)))
        card.addView(info("Kontak", "hanzzcodee@gmail.com"), lp(MATCH, WRAP, b = app.dpi(8f)))
        card.addView(info("Dukung", "saweria.co/hanzreally"), lp(MATCH, WRAP, b = app.dpi(12f)))
        card.addView(
            app.textView(
                "Game ini tidak memakai iklan dan tidak meminta akses internet. Progres tersimpan di perangkatmu.",
                15f, Palette.INK_SOFT, hand = false, center = false
            ),
            lp(MATCH, WRAP)
        )
        content.addView(card, lp(MATCH, WRAP, b = app.dpi(16f)))

        val mail = InkButton(app, false).apply {
            text = "Kirim email ke pengembang"
            setOnClickListener {
                open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hanzzcodee@gmail.com")).putExtra(Intent.EXTRA_SUBJECT, "Garis Kata"))
            }
        }
        val support = InkButton(app, true).apply {
            text = "Dukung lewat Saweria"
            setOnClickListener { open(Intent(Intent.ACTION_VIEW, Uri.parse("https://saweria.co/hanzreally"))) }
        }
        content.addView(support, lp(MATCH, WRAP, b = app.dpi(12f)))
        content.addView(mail, lp(MATCH, WRAP, b = app.dpi(12f)))
        content.addView(status, lp(MATCH, WRAP))
        return scaffold(app, "Tentang", content)
    }

    private fun info(label: String, value: String): View {
        val box = LinearLayout(app).apply { orientation = LinearLayout.VERTICAL }
        box.addView(app.textView(label, 14f, Palette.INK_SOFT, hand = false, center = false), lp(MATCH, WRAP))
        box.addView(app.textView(value, 19f, center = false), lp(MATCH, WRAP))
        return box
    }

    private fun open(intent: Intent) {
        try {
            app.startActivity(intent)
            status.text = ""
        } catch (e: ActivityNotFoundException) {
            status.text = "Tidak ada aplikasi di perangkat ini yang bisa membuka tautan tersebut."
        }
    }
}
