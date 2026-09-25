package de.praxisit.mule

import de.praxisit.mule.GameResult.Ongoing

fun main() {
    Game(white = AlphaBetaStrategy(), black = AlphaBetaStrategy()).play()
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
        fun humanAgainstComputer(): Game = if (ConsoleUi.askColor() == White) {
            Game(white = ConsolePlayer(), black = AlphaBetaStrategy())
        } else {
            Game(white = AlphaBetaStrategy(), black = ConsolePlayer())
        }
    }
}
