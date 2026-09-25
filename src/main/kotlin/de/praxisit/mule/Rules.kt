package de.praxisit.mule

import de.praxisit.mule.Phase.*

/**
 * The rules of the game: which moves are legal in a position and how a move changes it.
 */
object Rules {
    fun legalMoves(position: Position): List<Move> {
        val board = position.board
        val color = position.activeColor
        val moves = when (position.activePlayer.phase) {
            SETTING -> settingMoves(board, color)
            MOVING  -> pushingMoves(board, color)
            JUMPING -> jumpMoves(board, color)
            LOOSE   -> emptyList()
        }
        return extendMovesByCaptures(moves, board, color)
    }

    /** Applies [move] without checking whether it is legal, use [GameState.play] instead. */
    internal fun apply(position: Position, move: Move): Position {
        var board = when (move) {
            is SetMove           -> position.board.setStone(move.color, move.toField)
            is MoveWithFromField -> position.board.moveStone(move.fromField, move.toField)
        }
        if (move.capturedField != null) board = board.removeStone(move.capturedField)

        val mover = position.activePlayer
        val opponent = position.player(mover.color.opposite)
        val newMover = if (move is SetMove) mover.setStone() else mover
        val newOpponent = if (move.isCaptureMove) opponent.loseStone() else opponent

        val (white, black) = if (mover.color == White) newMover to newOpponent else newOpponent to newMover
        return Position(board, white, black, opponent.color)
    }

    private fun extendMovesByCaptures(moves: List<Move>, board: Board, color: Color): List<Move> {
        val (captureMoves, normalMoves) = moves.partition { move ->
            when (move) {
                is SetMove           -> board.willCloseMule(move.toField, color)
                is MoveWithFromField -> board.willCloseMule(move.fromField, move.toField, color)
            }
        }
        val capturablePieces = board.capturablePieces(color.opposite)
        if (capturablePieces.isEmpty()) return moves

        return captureMoves.flatMap { move ->
            capturablePieces.map { captureField -> move.addCaptureField(captureField) }
        } + normalMoves
    }

    private fun settingMoves(board: Board, color: Color): List<Move> =
        board.emptyFieldsIndices.map { SetMove(color, it) }

    private fun pushingMoves(board: Board, color: Color): List<Move> =
        board.fieldsIndicesWithColor(color).flatMap { fromField ->
            board.connectedEmptyFields(fromField).map { emptyField -> PushMove(color, fromField, emptyField) }
        }

    private fun jumpMoves(board: Board, color: Color): List<Move> =
        board.fieldsIndicesWithColor(color).flatMap { fromField ->
            board.emptyFieldsIndices.map { emptyField -> JumpMove(color, fromField, emptyField) }
        }
}
