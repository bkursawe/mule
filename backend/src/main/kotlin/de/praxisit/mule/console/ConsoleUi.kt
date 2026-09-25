package de.praxisit.mule.console

import de.praxisit.mule.*
import de.praxisit.mule.GameResult.*
import java.util.*

/**
 * Text output and input for playing on the console.
 */
object ConsoleUi {
    fun format(state: GameState, evaluation: Double): String {
        val board = state.board
        fun f(index: Int) = when (board.getStone(FieldIndex(index))) {
            Empty -> "O"
            White -> "W"
            Black -> "B"
        }
        fun marker(color: Color) = if (state.activeColor == color) "*" else " "
        val white = state.position.white
        val black = state.position.black

        return """
             ${f(0)}--------${f(1)}--------${f(2)}
             |        |        |
             |  ${f(3)}-----${f(4)}-----${f(5)}  |
             |  |     |     |  |
             |  |  ${f(6)}--${f(7)}--${f(8)}  |  |
             |  |  |     |  |  |
             ${f(9)}--${f(10)}--${f(11)}     ${f(12)}--${f(13)}--${f(14)}
             |  |  |     |  |  |
             |  |  ${f(15)}--${f(16)}--${f(17)}  |  |
             |  |     |     |  |
             |  ${f(18)}-----${f(19)}-----${f(20)}  |
             |        |        |
             ${f(21)}--------${f(22)}--------${f(23)}
             ${marker(White)} White: stones = ${white.stones} phase = ${white.phase}
             ${marker(Black)} Black: stones = ${black.stones} phase = ${black.phase}
             Evaluation: $evaluation

        """.trimIndent()
    }

    fun format(result: GameResult) = when (result) {
        Ongoing -> "No winner yet"
        Remis   -> "It's a remis"
        is Win  -> "${result.winner} is the winner"
    }

    fun askColor(): Color {
        print("Do you want to play with (W)hite or (B)lack? ")
        val colorInput = readln()
        return when (colorInput.lowercase(Locale.getDefault())) {
            "w", "white" -> White
            "b", "black" -> Black
            else         -> throw IllegalArgumentException("Invalid color input")
        }
    }
}

/**
 * A human player who chooses the moves on the console.
 */
class ConsolePlayer : ChoosingStrategy {
    override fun chooseMove(state: GameState): Move {
        val moves = state.legalMoves
        moves.mapIndexed { index, move -> "$index: $move" }.forEach { println(it) }
        println("Choose a move by number: ")
        var answer = readln()
        while (answer.toIntOrNull() !in moves.indices) {
            println("Choose a move by number (0 .. ${moves.lastIndex}): ")
            answer = readln()
        }
        return moves[answer.toInt()]
    }
}
