package de.praxisit.muli.board

import de.praxisit.muli.board.Phase.JUMPING
import de.praxisit.muli.board.Phase.LOOSE

class Player private constructor(val color: Color, val stones: Int, val stonesSet: Int, val phase: Phase) {

    val remainingStones: Int
        get() = 9 - stonesSet

    constructor(color: Color) : this(color, 9, 0, Phase.SETTING)

    fun loseStone(): Player {
        return if (stones > 4) {
            Player(color, stones - 1, stonesSet, phase)
        } else if (stones == 4) {
            Player(color, 3, stonesSet, JUMPING)
        } else {
            Player(color, 2, stonesSet, LOOSE)
        }
    }

    fun setStone(): Player {
        check(stonesSet != 9)
        return Player(color, stones, stonesSet + 1, phase)
    }

    fun legalMoves(board: Board): List<Move> = when (phase) {
        Phase.SETTING -> settingMoves(board)
        Phase.MOVING  -> pushingMoves(board)
        JUMPING       -> jumpMoves(board)
        LOOSE         -> emptyList()
    }

    private fun settingMoves(board: Board): List<Move> = board.emptyFieldsIndices().map { SetMove(color, it) }

    private fun pushingMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.connectedEmptyFields(fromField).map { emptyField -> PushMove(color, fromField, emptyField) }
    }

    private fun jumpMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.emptyFieldsIndices().map { emptyField -> JumpMove(color, fromField, emptyField) }
    }
}
