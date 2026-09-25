package de.praxisit.mule

/**
 * Evaluates a position: positive values are good for White, negative values are good for Black.
 */
fun interface EvaluationStrategy {
    fun evaluate(position: Position): Double
}

class SimpleEvaluationStrategy : EvaluationStrategy {
    override fun evaluate(position: Position): Double {
        val board = position.board
        val pointsForWhite = board.weightedStonesOnBoard(White) +
                5 * board.imcompleteMillCount(White) +
                10 * board.muleCount(White)
        val pointsForBlack = board.weightedStonesOnBoard(Black) +
                5 * board.imcompleteMillCount(Black) +
                10 * board.muleCount(Black)

        return (pointsForWhite - pointsForBlack).toDouble()
    }
}
