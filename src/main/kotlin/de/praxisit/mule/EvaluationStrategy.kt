package de.praxisit.mule

fun interface EvaluationStrategy {
    fun evaluate(board: Board): Double
}

class SimpleEvaluationStrategy : EvaluationStrategy {
    override fun evaluate(board: Board): Double {
        val pointsForWhite = board.stonesOnBoard(White) +
                5 * board.imcompleteMillCount(White) +
                10 * board.muleCount(White)
        val pointsForBlack = board.stonesOnBoard(Black) +
                5 * board.imcompleteMillCount(Black) +
                10 * board.muleCount(Black)

        return (pointsForWhite - pointsForBlack).toDouble()
    }
}
