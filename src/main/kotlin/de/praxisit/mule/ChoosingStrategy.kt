package de.praxisit.mule

fun interface ChoosingStrategy {
    fun chooseMove(board: Board): Move?
}

class SimpleChoosingStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board): Move? {
        val activePlayer = board.activePlayer
        val sortedMoves = activePlayer.legalMoves(board)
            .sortedByDescending { move ->
                val newBoard = board.draw(move)
                activePlayer.evaluate(newBoard)
            }
        return sortedMoves.firstOrNull()
    }
}

/*lass AlphaBetaStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board): Move? {

    }

    private fun alphaBeta(board: Board, depth: Int, alpha: Double, beta: Double, maximizingPlyer: Boolean): Double {
        if (depth == 0) {
            return board.activePlayer.evaluate(board)
        }
    }
}*/
