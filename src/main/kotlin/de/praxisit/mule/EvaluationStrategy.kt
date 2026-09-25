package de.praxisit.mule

import de.praxisit.mule.Phase.*

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
            5 * board.openMuleCount(White) +
            10 * board.muleCount(White)
        val pointsForBlack = board.weightedStonesOnBoard(Black) +
            5 * board.openMuleCount(Black) +
            10 * board.muleCount(Black)

        return (pointsForWhite - pointsForBlack).toDouble()
    }
}

/**
 * Evaluates for both players their stones (on the board and in hand), their mules, the mules they can close
 * with their next move, their mobility in the moving phase and the connections of their fields.
 * The default weights won most games against [SimpleEvaluationStrategy] in engine matches.
 */
class ExtendedEvaluationStrategy(
    private val stoneWeight: Double = 100.0,
    private val muleWeight: Double = 10.0,
    private val closableMuleWeight: Double = 40.0,
    private val mobilityWeight: Double = 5.0
) : EvaluationStrategy {
    override fun evaluate(position: Position) = score(position, White) - score(position, Black)

    private fun score(position: Position, color: Color): Double {
        val board = position.board
        val player = position.player(color)
        val mobility = if (player.phase == MOVING) mobility(board, color) else 0
        return stoneWeight * player.stones +
            muleWeight * board.muleCount(color) +
            closableMuleWeight * closableMules(board, color, player.phase) +
            mobilityWeight * mobility +
            board.weightedStonesOnBoard(color)
    }

    // In the moving phase a mule can only be closed by pushing a neighbouring stone that is not part of it
    private fun closableMules(board: Board, color: Color, phase: Phase): Int = when (phase) {
        SETTING, JUMPING -> Integer.bitCount(board.closingFields(color))
        MOVING           -> {
            var closable = 0
            board.closingFields(color).forEachField { field ->
                var canClose = false
                (Board.NEIGHBORS[field.index] and board.stones(color)).forEachField { neighbour ->
                    if (board.willCloseMule(neighbour, field, color)) canClose = true
                }
                if (canClose) closable++
            }
            closable
        }
        LOST             -> 0
    }

    private fun mobility(board: Board, color: Color): Int {
        val emptyFields = board.emptyFields
        var moves = 0
        board.stones(color).forEachField { moves += Integer.bitCount(Board.NEIGHBORS[it.index] and emptyFields) }
        return moves
    }
}
