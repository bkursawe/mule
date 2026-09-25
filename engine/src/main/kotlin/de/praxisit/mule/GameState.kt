package de.praxisit.mule

import de.praxisit.mule.GameResult.*
import de.praxisit.mule.Phase.LOST
import kotlin.LazyThreadSafetyMode.NONE

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
 * the previous states and the number of moves since the last capture.
 */
class GameState private constructor(
    val position: Position,
    val movesWithoutCapture: Int,
    private val previous: GameState?
) {
    constructor(
        position: Position = Position(),
        movesWithoutCapture: Int = 0
    ) : this(position, movesWithoutCapture, null)

    val board: Board
        get() = position.board

    val activeColor: Color
        get() = position.activeColor

    val activePlayer: Player
        get() = position.activePlayer

    val legalMoves: List<Move> by lazy(NONE) { Rules.legalMoves(position) }

    /** Plays a legal move of the active player and hands the turn to the opponent. */
    fun play(move: Move): GameState {
        require(move in legalMoves) { "Illegal move: $move" }

        return GameState(
            position = Rules.apply(position, move),
            movesWithoutCapture = if (move.isCaptureMove) 0 else movesWithoutCapture + 1,
            previous = this
        )
    }

    // Positions before the last capture had more stones, so only the states since then can repeat
    val isRepeated: Boolean by lazy(NONE) {
        var occurrences = 0
        var state = previous
        var steps = 0
        while (state != null && steps < movesWithoutCapture) {
            if (state.position == position) occurrences++
            state = state.previous
            steps++
        }
        occurrences >= 2
    }

    val isRemis: Boolean
        get() = movesWithoutCapture >= MOVES_WITHOUT_CAPTURE_FOR_REMIS || isRepeated

    val result: GameResult by lazy(NONE) {
        when {
            position.white.phase == LOST  -> Win(Black)
            position.black.phase == LOST  -> Win(White)
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
