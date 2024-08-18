package de.praxisit.mule

import de.praxisit.mule.Phase.*
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

class Player internal constructor(
    val color: Color,
    val stones: Int,
    val stonesSet: Int,
    val phase: Phase,
    private val evaluationStrategy: EvaluationStrategy = SimpleEvaluationStrategy(),
    private val choosingStrategy: ChoosingStrategy = AlphaBetaStrategy()
) : EvaluationStrategy by evaluationStrategy, ChoosingStrategy by choosingStrategy {
    val remainingStones: Int
        get() = 9 - stonesSet

    val worstEvaluation = if (color == White) NEGATIVE_INFINITY else POSITIVE_INFINITY

    constructor(color: Color) : this(color, 9, 0, SETTING)

    constructor(color: Color, evaluationStrategy: EvaluationStrategy) : this(
        color,
        9,
        0,
        SETTING,
        evaluationStrategy,
        SimpleChoosingStrategy()
    )

    private fun copy(
        color: Color = this.color,
        stones: Int = this.stones,
        stonesSet: Int = this.stonesSet,
        phase: Phase = this.phase,
        evaluationStrategy: EvaluationStrategy = this.evaluationStrategy,
        choosingStrategy: ChoosingStrategy = this.choosingStrategy
    ) = Player(color, stones, stonesSet, phase, evaluationStrategy, choosingStrategy)

    fun loseStone(): Player {
        return when {
            stones > 4  -> copy(stones = stones - 1)
            stones == 4 -> copy(stones = 3, phase = JUMPING)
            else        -> copy(stones = 2, phase = LOOSE)
        }
    }

    fun setStone(): Player {
        check(stonesSet != 9)
        check(phase == SETTING)

        return copy(stonesSet = stonesSet + 1, phase = if (stonesSet == 8) MOVING else SETTING)
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
        return captureMoves.flatMap { move ->
            board.capturablePieces(color.opposite).map { captureField -> move.addCaptureField(captureField) }
        } + normalMoves
    }

    private fun settingMoves(board: Board): List<Move> {
        return board.emptyFieldsIndices().map { SetMove(color, it) }
    }

    private fun pushingMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.connectedEmptyFields(fromField).map { emptyField -> PushMove(color, fromField, emptyField) }
    }

    private fun jumpMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.emptyFieldsIndices().map { emptyField -> JumpMove(color, fromField, emptyField) }
    }

}
