package com.hanzdev.gariskata.data

import android.content.Context
import com.hanzdev.gariskata.R

data class Puzzle(val sentence: String, val answer: String, val clue: String) {

    // Kalimat lengkap untuk ditampilkan setelah stage selesai.
    fun filled(): String {
        val index = sentence.indexOf(BLANK)
        val before = sentence.substring(0, index).trimEnd()
        val lastWord = before.substringAfterLast(' ', "")
        // Kata pertama kalimat otomatis berhuruf besar, jadi tidak dihitung sebagai nama.
        val afterName = before.contains(' ') && lastWord.firstOrNull()?.isUpperCase() == true
        val proper = index == 0 || answer in ALWAYS_CAPITAL || afterName
        val word = answer.lowercase().let { if (proper) it.replaceFirstChar { c -> c.uppercase() } else it }
        return sentence.replace(BLANK, word)
    }

    companion object {
        const val BLANK = "___"
        private val ALWAYS_CAPITAL = setOf("SELASA", "AGUSTUS", "MERKURIUS", "BUMI", "MINANGKABAU", "BOROBUDUR")

        fun parse(line: String): Puzzle? {
            val parts = line.split("|")
            if (parts.size != 3) return null
            val (sentence, answer, clue) = parts
            val validAnswer = answer.isNotEmpty() && answer.all { it in 'A'..'Z' }
            val oneBlank = sentence.split(BLANK).size == 2
            return if (validAnswer && oneBlank) Puzzle(sentence, answer, clue) else null
        }
    }
}

object PuzzleRepo {
    // Daftar kosong berarti berkas rusak, layar beranda menampilkan pesan galat.
    fun load(context: Context): List<Puzzle> = try {
        context.resources.openRawResource(R.raw.stages).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { Puzzle.parse(it) }.toList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
