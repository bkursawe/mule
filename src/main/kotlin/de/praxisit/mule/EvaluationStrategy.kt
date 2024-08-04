package de.praxisit.mule

fun interface EvaluationStrategy {
    fun evaluate(board: Board): Double
}

class SimpleEvaluationStrategy : EvaluationStrategy {
    override fun evaluate(board: Board): Double {
        val pointsForWhite = board.stonesOnBoard(White) +
                board.imcompleteMillCount(White) +
                board.muleCount(White)
        val pointsForBlack = board.stonesOnBoard(Black) +
                board.imcompleteMillCount(Black) +
                board.muleCount(White)

        return (pointsForWhite - pointsForBlack).toDouble()
    }
}
