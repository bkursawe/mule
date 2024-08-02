package de.praxisit.mule

fun interface EvaluationStrategy {
    fun evaluate(board: Board): Double
}

class SimpleEvaluationStrategy : EvaluationStrategy {
    override fun evaluate(board: Board): Double {
        val activePlayerColor = board.activePlayerColor
        val pointsForActive = board.stonesOnBoard(activePlayerColor) +
                board.imcompleteMillCount(activePlayerColor)
        val oppositePlayerColor = activePlayerColor.opposite
        val pointsForOpposite = board.stonesOnBoard(oppositePlayerColor) +
                board.imcompleteMillCount(oppositePlayerColor)

        return (pointsForActive - pointsForOpposite).toDouble()
    }
}
