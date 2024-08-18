package de.praxisit.mule

import de.praxisit.mule.Phase.LOOSE

fun interface ChoosingStrategy {
    fun chooseMove(board: Board): Move
}

class SimpleChoosingStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board) = board
        .legalMoves
        .maxByOrNull { move -> board.draw(move).evaluation } ?: NoMove
}

class AlphaBetaStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board) = alphaBeta(board, 5).first

    private fun alphaBeta(
        board: Board,
        depth: Int,
        alpha: Double = Double.MIN_VALUE,
        beta: Double = Double.MAX_VALUE
    ): Pair<Move, Double> {
        if (depth == 0) return Pair(NoMove, board.evaluation)
        if (board.activePlayer.phase == LOOSE) return Pair(NoMove, board.activePlayer.worstEvaluation)
        if (board.isRepeated) return Pair(NoMove, 0.0)

        return if (board.activePlayer.color == White) {
            bestMoveForWhite(board, depth, alpha, beta)
        } else {
            bestMoveForBlack(board, depth, alpha, beta)
        }
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
