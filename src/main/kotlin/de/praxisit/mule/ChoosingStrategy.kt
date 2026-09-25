package de.praxisit.mule

import de.praxisit.mule.Phase.LOOSE
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

fun interface ChoosingStrategy {
    fun chooseMove(board: Board): Move
}

class SimpleChoosingStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board): Move {
        val evaluation = { move: Move -> board.draw(move).evaluation }
        return if (board.activePlayerColor == White) {
            board.legalMoves.maxByOrNull(evaluation)
        } else {
            board.legalMoves.minByOrNull(evaluation)
        } ?: NoMove
    }
}

class AlphaBetaStrategy(private val depth: Int = 5) : ChoosingStrategy {
    // The root is searched without the terminal checks, so a legal move is returned whenever one exists
    override fun chooseMove(board: Board) = bestMove(board, depth, NEGATIVE_INFINITY, POSITIVE_INFINITY).first

    private fun alphaBeta(board: Board, depth: Int, alpha: Double, beta: Double): Pair<Move, Double> {
        if (board.activePlayer.phase == LOOSE) return Pair(NoMove, board.activePlayer.worstEvaluation)
        if (board.isRemis) return Pair(NoMove, 0.0)
        if (depth == 0) return Pair(NoMove, board.evaluation)

        return bestMove(board, depth, alpha, beta)
    }

    private fun bestMove(board: Board, depth: Int, alpha: Double, beta: Double): Pair<Move, Double> =
        if (board.activePlayer.color == White) {
            bestMoveForWhite(board, depth, alpha, beta)
        } else {
            bestMoveForBlack(board, depth, alpha, beta)
        }

    private fun bestMoveForWhite(
        board: Board,
        depth: Int,
        alpha: Double,
        beta: Double
    ): Pair<Move, Double> {
        var bestMove: Move = NoMove
        var maxEval = Double.NEGATIVE_INFINITY
        var currentAlpha = alpha
        for (move in board.legalMoves) {
            val (_, eval) = alphaBeta(board.draw(move).withSwitchedPlayer, depth - 1, currentAlpha, beta)
            if (eval > maxEval || bestMove == NoMove) {
                maxEval = eval
                bestMove = move
            }
            currentAlpha = maxOf(currentAlpha, eval)
            if (beta <= currentAlpha) {
                break
            }
        }
        return Pair(bestMove, maxEval)
    }

    private fun bestMoveForBlack(
        board: Board,
        depth: Int,
        alpha: Double,
        beta: Double
    ): Pair<Move, Double> {
        var bestMove: Move = NoMove
        var minEval = Double.POSITIVE_INFINITY
        var currentBeta = beta
        for (move in board.legalMoves) {
            val (_, eval) = alphaBeta(board.draw(move).withSwitchedPlayer, depth - 1, alpha, currentBeta)
            if (eval < minEval || bestMove == NoMove) {
                minEval = eval
                bestMove = move
            }
            currentBeta = minOf(currentBeta, eval)
            if (currentBeta <= alpha) {
                break
            }
        }
        return Pair(bestMove, minEval)
    }
}
