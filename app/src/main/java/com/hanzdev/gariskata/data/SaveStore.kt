package com.hanzdev.gariskata.data

import android.content.Context

class SaveStore(context: Context) {

    private val prefs = context.getSharedPreferences("garis_kata", Context.MODE_PRIVATE)

    init {
        if (prefs.getInt(K_VERSION, 0) == 0) prefs.edit().putInt(K_VERSION, SAVE_VERSION).apply()
    }

    var coins: Int
        get() = prefs.getInt(K_COINS, START_COINS).coerceAtLeast(0)
        set(value) { prefs.edit().putInt(K_COINS, value.coerceAtLeast(0)).apply() }

    var cleared: Int
        get() = prefs.getInt(K_CLEARED, 0).coerceAtLeast(0)
        private set(value) { prefs.edit().putInt(K_CLEARED, value).apply() }

    var soundOn: Boolean
        get() = prefs.getBoolean(K_SOUND, true)
        set(value) { prefs.edit().putBoolean(K_SOUND, value).apply() }

    var hapticOn: Boolean
        get() = prefs.getBoolean(K_HAPTIC, true)
        set(value) { prefs.edit().putBoolean(K_HAPTIC, value).apply() }

    var calmMotion: Boolean
        get() = prefs.getBoolean(K_CALM, false)
        set(value) { prefs.edit().putBoolean(K_CALM, value).apply() }

    fun markCleared(stage: Int) {
        if (stage > cleared) cleared = stage
    }

    fun saveProgress(stage: Int, data: String) {
        prefs.edit().putInt(K_PROGRESS_STAGE, stage).putString(K_PROGRESS_DATA, data).apply()
    }

    fun progressFor(stage: Int): String? =
        if (prefs.getInt(K_PROGRESS_STAGE, 0) == stage) prefs.getString(K_PROGRESS_DATA, null) else null

    fun clearProgress() {
        prefs.edit().remove(K_PROGRESS_STAGE).remove(K_PROGRESS_DATA).apply()
    }

    fun resetGame() {
        prefs.edit().putInt(K_COINS, START_COINS).putInt(K_CLEARED, 0)
            .remove(K_PROGRESS_STAGE).remove(K_PROGRESS_DATA).apply()
    }

    companion object {
        const val START_COINS = 100
        private const val SAVE_VERSION = 1
        private const val K_VERSION = "versi"
        private const val K_COINS = "koin"
        private const val K_CLEARED = "stage_selesai"
        private const val K_SOUND = "suara"
        private const val K_HAPTIC = "getar"
        private const val K_CALM = "kurangi_gerak"
        private const val K_PROGRESS_STAGE = "progres_stage"
        private const val K_PROGRESS_DATA = "progres_data"
    }
}
