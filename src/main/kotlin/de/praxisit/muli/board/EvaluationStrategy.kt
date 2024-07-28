package de.praxisit.muli.board

fun interface EvaluationStrategy {
    fun evaluate(board: Board, move: Move): Double
}

class SimpleEvaluationStrategy : EvaluationStrategy {
    override fun evaluate(board: Board, move: Move): Double {
        val activePlayerColor = board.activePlayerColor
        require(activePlayerColor == move.color)
        val pointsForActive = board.stonesOnBoard(activePlayerColor) +
                board.imcompleteMillCount(activePlayerColor) +
                board.potentialMill(move) +
                if (move.isCaptureMove) 5 else 0
        val oppositePlayerColor = activePlayerColor.opposite
        val pointsForOpposite = board.stonesOnBoard(oppositePlayerColor) +
                board.imcompleteMillCount(oppositePlayerColor)

        return (pointsForActive - pointsForOpposite).toDouble()
    }
}
