package de.praxisit.mule

import de.praxisit.mule.Phase.*
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * [stones] counts all stones the player still owns, on the board and in hand.
 */
class Player internal constructor(
    val color: Color,
    val stones: Int,
    val stonesSet: Int,
    private val evaluationStrategy: EvaluationStrategy = SimpleEvaluationStrategy(),
    private val choosingStrategy: ChoosingStrategy = AlphaBetaStrategy()
) : EvaluationStrategy by evaluationStrategy, ChoosingStrategy by choosingStrategy {
    val remainingStones: Int
        get() = 9 - stonesSet

    val phase: Phase
        get() = when {
            stones < 3    -> LOOSE
            stonesSet < 9 -> SETTING
            stones == 3   -> JUMPING
            else          -> MOVING
        }

    val worstEvaluation = if (color == White) NEGATIVE_INFINITY else POSITIVE_INFINITY

    constructor(color: Color) : this(color, 9, 0)

    constructor(color: Color, evaluationStrategy: EvaluationStrategy) : this(
        color,
        9,
        0,
        evaluationStrategy,
        SimpleChoosingStrategy()
    )

    private fun copy(
        color: Color = this.color,
        stones: Int = this.stones,
        stonesSet: Int = this.stonesSet,
        evaluationStrategy: EvaluationStrategy = this.evaluationStrategy,
        choosingStrategy: ChoosingStrategy = this.choosingStrategy
    ) = Player(color, stones, stonesSet, evaluationStrategy, choosingStrategy)

    fun loseStone(): Player {
        check(phase != LOOSE)

        return copy(stones = stones - 1)
    }

    fun setStone(): Player {
        check(phase == SETTING)

        return copy(stonesSet = stonesSet + 1)
    }

    fun legalMoves(board: Board): List<Move> {
        val moves = when (phase) {
            SETTING      -> settingMoves(board)
            MOVING       -> pushingMoves(board)
            JUMPING      -> jumpMoves(board)
            LOOSE, REMIS -> emptyList()
        }
        return extendMovesByCaptures(moves, board)
    }

    private fun extendMovesByCaptures(moves: List<Move>, board: Board): List<Move> {
        val (captureMoves, normalMoves) = moves.partition { move ->
            when (move) {
                is SetMove           -> board.willCloseMule(move.toField, color)
                is MoveWithFromField -> board.willCloseMule(move.fromField, move.toField, color)
                is NoMove -> throw IllegalMoveException(NoMove, "Cannot move")
            }
        }
        val capturablePieces = board.capturablePieces(color.opposite)
        if (capturablePieces.isEmpty()) return moves

        return captureMoves.flatMap { move ->
            capturablePieces.map { captureField -> move.addCaptureField(captureField) }
        } + normalMoves
    }

    private fun settingMoves(board: Board): List<Move> {
        return board.emptyFieldsIndices.map { SetMove(color, it) }
    }

    private fun pushingMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.connectedEmptyFields(fromField).map { emptyField -> PushMove(color, fromField, emptyField) }
    }

    private fun jumpMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.emptyFieldsIndices.map { emptyField -> JumpMove(color, fromField, emptyField) }
    }

}
