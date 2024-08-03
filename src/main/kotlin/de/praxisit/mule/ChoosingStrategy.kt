package de.praxisit.mule

fun interface ChoosingStrategy {
    fun chooseMove(board: Board): Move?
}

class SimpleChoosingStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board) = board
        .legalMoves
        .maxByOrNull { move -> board.draw(move).evaluation }
}

class AlphaBetaStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board) = alphaBeta(board, 3).first

    private fun alphaBeta(
        board: Board,
        depth: Int,
        alpha: Double = Double.MIN_VALUE,
        beta: Double = Double.MAX_VALUE,
        maximizingPlyer: Boolean = true
    ): Pair<Move, Double> {
        if (depth == 0) {
            return Pair(NoMove, board.evaluation)
        }

        var bestMove: Move = NoMove
        return if (maximizingPlyer) {
            var maxEval = Double.MIN_VALUE
            var currentAlpha = alpha
            for (move in board.legalMoves) {
                val (_, eval) = alphaBeta(board.draw(move), depth - 1, currentAlpha, beta, false)
                if (maxEval < eval) {
                    maxEval = eval
                    bestMove = move
                }
                currentAlpha = maxOf(currentAlpha, eval)
                if (beta <= currentAlpha) {
                    break
                }
            }
            Pair(bestMove, maxEval)
        } else {
            var minEval = Double.MAX_VALUE
            var currentBeta = beta
            for (move in board.legalMoves) {
                val (_, eval) = alphaBeta(board.draw(move), depth - 1, alpha, currentBeta, true)
                if (minEval > eval) {
                    minEval = eval
                    bestMove = move
                }
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= alpha) {
                    break
                }
            }
            Pair(bestMove, minEval)
        }
    }
}
