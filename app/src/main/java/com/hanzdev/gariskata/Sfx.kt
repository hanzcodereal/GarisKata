package com.hanzdev.gariskata

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.random.Random

class Sfx(context: Context) {

    enum class Sound(val res: Int) {
        TAP(R.raw.sfx_tap),
        BENAR(R.raw.sfx_benar),
        SALAH(R.raw.sfx_salah),
        MENANG(R.raw.sfx_menang),
        KALAH(R.raw.sfx_kalah),
        KOIN(R.raw.sfx_koin),
        BOM(R.raw.sfx_bom),
        UNGKAP(R.raw.sfx_ungkap),
        PETUNJUK(R.raw.sfx_petunjuk)
    }

    var enabled = true

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Sound, Int>()

    init {
        for (s in Sound.values()) ids[s] = pool.load(context, s.res, 1)
    }

    // Nada tap dibuat sedikit acak supaya tidak terdengar seperti mesin.
    fun play(sound: Sound, vary: Boolean = false) {
        if (!enabled) return
        val id = ids[sound] ?: return
        val rate = if (vary) 0.95f + Random.nextFloat() * 0.1f else 1f
        pool.play(id, 0.9f, 0.9f, 1, 0, rate)
    }

    fun release() {
        pool.release()
    }
}
