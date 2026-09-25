package de.praxisit.mule

import de.praxisit.mule.Phase.LOOSE
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Chooses a move for the active player of a game that is not finished yet.
 */
fun interface ChoosingStrategy {
    fun chooseMove(state: GameState): Move
}

/**
 * Chooses the move with the best evaluation after one move.
 */
class SimpleChoosingStrategy(
    private val evaluation: EvaluationStrategy = SimpleEvaluationStrategy()
) : ChoosingStrategy {
    override fun chooseMove(state: GameState): Move {
        val score = { move: Move -> evaluation.evaluate(state.play(move).position) }
        val move = if (state.activeColor == White) {
            state.legalMoves.maxByOrNull(score)
        } else {
            state.legalMoves.minByOrNull(score)
        }
        return checkNotNull(move) { "No legal move" }
    }
}

/**
 * Searches [depth] moves ahead with Negamax and alpha-beta pruning.
 * All scores are seen from the player to move.
 */
class AlphaBetaStrategy(
    private val depth: Int = 5,
    private val evaluation: EvaluationStrategy = SimpleEvaluationStrategy()
) : ChoosingStrategy {
    // The root is searched without the terminal checks, so a legal move is returned whenever one exists
    override fun chooseMove(state: GameState): Move {
        val (move, _) = bestMove(state, depth, NEGATIVE_INFINITY, POSITIVE_INFINITY)
        return checkNotNull(move) { "No legal move" }
    }

    private fun negamax(state: GameState, depth: Int, alpha: Double, beta: Double): Double = when {
        state.activePlayer.phase == LOOSE -> NEGATIVE_INFINITY
        state.isRemis                     -> 0.0
        depth == 0                        -> state.activeColor.sign * evaluation.evaluate(state.position)
        else                              -> bestMove(state, depth, alpha, beta).second
    }

    private fun bestMove(state: GameState, depth: Int, alpha: Double, beta: Double): Pair<Move?, Double> {
        var bestMove: Move? = null
        var bestScore = NEGATIVE_INFINITY
        var currentAlpha = alpha
        for (move in state.legalMoves) {
            val score = -negamax(state.play(move), depth - 1, -beta, -currentAlpha)
            if (score > bestScore || bestMove == null) {
                bestScore = score
                bestMove = move
            }
            currentAlpha = maxOf(currentAlpha, score)
            if (currentAlpha >= beta) {
                break
            }
        }
        return Pair(bestMove, bestScore)
    }

    private val Color.sign: Double
        get() = if (this == White) 1.0 else -1.0
}
