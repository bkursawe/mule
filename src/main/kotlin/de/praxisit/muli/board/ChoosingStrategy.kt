package de.praxisit.muli.board

fun interface ChoosingStrategy {
    fun chooseMove(board: Board): Move?
}

class SimpleChoosingStrategy : ChoosingStrategy {
    override fun chooseMove(board: Board): Move? {
        val activePlayer = board.activePlayer
        val sortedMoves = activePlayer.legalMoves(board)
            .sortedByDescending { move -> activePlayer.evaluate(board, move) }
        return sortedMoves.firstOrNull()
    }
}