package de.praxisit.muli.board

import de.praxisit.muli.board.Phase.*

class Player private constructor(val color: Color, val stones: Int, val stonesSet: Int, val phase: Phase) {

    val remainingStones: Int
        get() = 9 - stonesSet

    constructor(color: Color) : this(color, 9, 0, SETTING)

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
        check(phase == SETTING)

        return Player(color, stones, stonesSet + 1, if (stonesSet == 8) MOVING else SETTING)
    }

    fun legalMoves(board: Board): List<Move> {
        val moves = when (phase) {
            SETTING -> settingMoves(board)
            MOVING  -> pushingMoves(board)
            JUMPING -> jumpMoves(board)
            LOOSE   -> emptyList()
        }
        return extendMovesByCaptures(moves, board)
    }

    private fun extendMovesByCaptures(moves: List<Move>, board: Board): List<Move> {
        val (captureMoves, normalMoves) = moves.partition { move -> board.willCloseMule(move.toField, color) }
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

    fun chooseMove(board: Board): Move {
        return legalMoves(board).first()
    }
}
