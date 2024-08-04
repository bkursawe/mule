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
        beta: Double = Double.MAX_VALUE
    ): Pair<Move, Double> {
        if (depth == 0) {
            return Pair(NoMove, board.evaluation)
        }

        var bestMove: Move = NoMove
        return if (board.activePlayer.color == White) {
            var maxEval = Double.MIN_VALUE
            var currentAlpha = alpha
            for (move in board.legalMoves) {
                val (_, eval) = alphaBeta(board.draw(move).switchPlayer(), depth - 1, currentAlpha, beta)
                if (maxEval < eval) {
                    maxEval = eval
                    bestMove = move
                }
                currentAlpha = maxOf(currentAlpha, eval)
                if (beta <= currentAlpha) {
                    break
                }
            }
            println("ab(max) - $depth - $bestMove - $maxEval")
            Pair(bestMove, maxEval)
        } else {
            var minEval = Double.MAX_VALUE
            var currentBeta = beta
            for (move in board.legalMoves) {
                val (_, eval) = alphaBeta(board.draw(move).switchPlayer(), depth - 1, alpha, currentBeta)
                if (minEval > eval) {
                    minEval = eval
                    bestMove = move
                }
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= alpha) {
                    break
                }
            }
            println("ab(min) - $depth - $bestMove - $minEval")
            Pair(bestMove, minEval)
        }
    }
}
