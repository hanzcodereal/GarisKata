package com.hanzdev.gariskata.game

import com.hanzdev.gariskata.data.Puzzle
import kotlin.random.Random

enum class GuessResult { RIGHT, WRONG, IGNORED }

// Logika murni tanpa Android, supaya mudah diuji dan dipulihkan dari simpanan.
class GameState(val puzzle: Puzzle, private val random: Random = Random.Default) {

    val guessed = LinkedHashSet<Char>()
    val wrong = LinkedHashSet<Char>()
    val removed = LinkedHashSet<Char>()

    var clueShown = false
        private set

    val mistakes: Int get() = wrong.size
    val solved: Boolean get() = puzzle.answer.all { it in guessed }
    val lost: Boolean get() = !solved && mistakes >= MAX_MISTAKES
    val finished: Boolean get() = solved || lost

    fun isOpen(c: Char): Boolean = !finished && c !in guessed && c !in wrong && c !in removed

    fun guess(c: Char): GuessResult {
        if (!isOpen(c)) return GuessResult.IGNORED
        return if (c in puzzle.answer) {
            guessed.add(c)
            GuessResult.RIGHT
        } else {
            wrong.add(c)
            GuessResult.WRONG
        }
    }

    fun revealOne(): Char? {
        if (finished) return null
        val hidden = puzzle.answer.filter { it !in guessed }.toSet().toList()
        if (hidden.isEmpty()) return null
        val letter = hidden[random.nextInt(hidden.size)]
        guessed.add(letter)
        return letter
    }

    fun removeWrong(count: Int): List<Char> {
        if (finished) return emptyList()
        val pool = ('A'..'Z').filter { it !in puzzle.answer && isOpen(it) }
        val picked = pool.shuffled(random).take(count)
        removed.addAll(picked)
        return picked
    }

    fun showClue() {
        clueShown = true
    }

    fun encode(): String =
        "G=${guessed.joinToString("")};W=${wrong.joinToString("")};R=${removed.joinToString("")};C=${if (clueShown) 1 else 0}"

    companion object {
        const val MAX_MISTAKES = 6

        // Mengembalikan null bila data simpanan tidak cocok dengan soalnya.
        fun decode(puzzle: Puzzle, text: String): GameState? {
            val map = text.split(";").mapNotNull {
                val i = it.indexOf('=')
                if (i < 0) null else it.substring(0, i) to it.substring(i + 1)
            }.toMap()
            val g = map["G"] ?: return null
            val w = map["W"] ?: return null
            val r = map["R"] ?: return null
            val all = g + w + r
            if (all.any { it !in 'A'..'Z' } || all.toSet().size != all.length) return null
            if (g.any { it !in puzzle.answer } || (w + r).any { it in puzzle.answer }) return null
            val state = GameState(puzzle)
            state.guessed.addAll(g.toList())
            state.wrong.addAll(w.toList())
            state.removed.addAll(r.toList())
            if (map["C"] == "1") state.showClue()
            return if (state.finished) null else state
        }
    }
}
