package de.praxisit.mule

import de.praxisit.mule.GameResult.*
import de.praxisit.mule.Phase.LOOSE

/**
 * Everything that decides which moves are legal: the stones on the board, the stones of both players
 * and whose turn it is.
 */
data class Position(
    val board: Board = Board(),
    val white: Player = Player(White),
    val black: Player = Player(Black),
    val activeColor: Color = White
) {
    val activePlayer: Player
        get() = player(activeColor)

    fun player(color: Color) = if (color == White) white else black
}

/**
 * A position within a game. Besides the position it knows what the remis rules need:
 * the positions played so far and the number of moves since the last capture.
 */
class GameState(
    val position: Position = Position(),
    val movesWithoutCapture: Int = 0,
    private val history: List<Position> = emptyList()
) {
    val board: Board
        get() = position.board

    val activeColor: Color
        get() = position.activeColor

    val activePlayer: Player
        get() = position.activePlayer

    val legalMoves: List<Move> by lazy { Rules.legalMoves(position) }

    /** Plays a legal move of the active player and hands the turn to the opponent. */
    fun play(move: Move): GameState {
        require(move in legalMoves) { "Illegal move: $move" }

        return GameState(
            position = Rules.apply(position, move),
            movesWithoutCapture = if (move.isCaptureMove) 0 else movesWithoutCapture + 1,
            history = history + position
        )
    }

    val isRepeated: Boolean by lazy { history.count { it == position } >= 2 }

    val isRemis: Boolean
        get() = movesWithoutCapture >= MOVES_WITHOUT_CAPTURE_FOR_REMIS || isRepeated

    val result: GameResult by lazy {
        when {
            position.white.phase == LOOSE -> Win(Black)
            position.black.phase == LOOSE -> Win(White)
            legalMoves.isEmpty()          -> Win(activeColor.opposite)
            isRemis                       -> Remis
            else                          -> Ongoing
        }
    }

    companion object {
        const val MOVES_WITHOUT_CAPTURE_FOR_REMIS = 50
    }
}

sealed interface GameResult {
    data object Ongoing : GameResult
    data object Remis : GameResult
    data class Win(val winner: Color) : GameResult
}
