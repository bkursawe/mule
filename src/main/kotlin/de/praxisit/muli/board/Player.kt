package de.praxisit.muli.board

import de.praxisit.muli.board.Phase.*

class Player private constructor(val color: Color, var stones: Int, var stonesSet: Int, var phase: Phase) {

    val remainingStones: Int
        get() = 9 - stonesSet

    constructor(color: Color) : this(color, 9, 0, SETTING)

    fun loseStone(): Player {
        if (stones > 4) {
            stones -= 1
        } else if (stones == 4) {
            stones = 3
            phase = JUMPING
        } else {
            stones = 2
            phase = LOOSE
        }
        return this
    }

    fun setStone() {
        check(stonesSet != 9)
        check(phase == SETTING)

        stonesSet += 1
        phase = if (stonesSet == 9) MOVING else SETTING
    }

    fun legalMoves(board: Board): List<Move> = when (phase) {
        SETTING -> settingMoves(board)
        MOVING  -> pushingMoves(board)
        JUMPING -> jumpMoves(board)
        LOOSE   -> emptyList()
    }

    private fun settingMoves(board: Board): List<Move> {
        val (captureMoves, normalMoves) = board.emptyFieldsIndices().partition { move -> board.willCloseMule(move, color) }
        return captureMoves.flatMap { field -> board.capturablePieces(color.opposite).map { SetMove(color, field, it) } } +
                normalMoves.map { SetMove(color, it) }
    }

    private fun pushingMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.connectedEmptyFields(fromField).map { emptyField -> PushMove(color, fromField, emptyField) }
    }

    private fun jumpMoves(board: Board): List<Move> = board.fieldsIndicesWithColor(color).flatMap { fromField ->
        board.emptyFieldsIndices().map { emptyField -> JumpMove(color, fromField, emptyField) }
    }

    fun chooseMove(board: Board): Move {
        return legalMoves(board).first()
    }
}
