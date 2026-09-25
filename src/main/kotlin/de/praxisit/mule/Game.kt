package de.praxisit.mule

import de.praxisit.mule.GameResult.Ongoing
import kotlin.time.Duration.Companion.seconds

fun main() {
    Game(white = Game.computerPlayer(), black = Game.computerPlayer()).play()
}

/**
 * Plays a game between two players and prints it on the console.
 */
class Game(
    private val white: ChoosingStrategy,
    private val black: ChoosingStrategy,
    private val evaluation: EvaluationStrategy = SimpleEvaluationStrategy()
) {
    fun play(): GameResult {
        var state = GameState()
        var moveNumber = 0
        while (state.result == Ongoing) {
            printState(state)
            val move = player(state.activeColor).chooseMove(state)
            println("${moveNumber++}: $move")
            state = state.play(move)
        }
        printState(state)
        println(ConsoleUi.format(state.result))
        return state.result
    }

    private fun player(color: Color) = if (color == White) white else black

    private fun printState(state: GameState) = println(ConsoleUi.format(state, evaluation.evaluate(state.position)))

    companion object {
        /** A computer player that thinks one second per move. */
        fun computerPlayer() = AlphaBetaStrategy(depth = 20, timeLimit = 1.seconds)

        fun humanAgainstComputer(): Game = if (ConsoleUi.askColor() == White) {
            Game(white = ConsolePlayer(), black = computerPlayer())
        } else {
            Game(white = computerPlayer(), black = ConsolePlayer())
        }
    }
}
