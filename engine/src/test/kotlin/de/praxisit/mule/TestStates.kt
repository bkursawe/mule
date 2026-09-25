package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex

fun Board.setStone(color: Color, field: Int) = setStone(color, field.asFieldIndex)

fun Board.setStones(color: Color, vararg fields: Int) = fields.fold(this) { board, field ->
    board.setStone(color, field)
}

fun Board.getStone(field: Int) = getStone(field.asFieldIndex)

fun Board.moveStone(from: Int, to: Int) = moveStone(from.asFieldIndex, to.asFieldIndex)

fun createBoard(whiteStones: List<Int>, blackStones: List<Int>) =
    Board().setStones(White, *whiteStones.toIntArray()).setStones(Black, *blackStones.toIntArray())

/**
 * Creates a game state in which each player owns the stones on the board plus the stones still to set.
 */
fun createState(
    whiteStones: List<Int>,
    whiteStonesToSet: Int,
    blackStones: List<Int>,
    blackStonesToSet: Int,
    activeColor: Color,
    movesWithoutCapture: Int = 0
): GameState {
    val position = Position(
        board = createBoard(whiteStones, blackStones),
        white = Player(White, whiteStones.size + whiteStonesToSet, Player.STONES - whiteStonesToSet),
        black = Player(Black, blackStones.size + blackStonesToSet, Player.STONES - blackStonesToSet),
        activeColor = activeColor
    )
    return GameState(position, movesWithoutCapture)
}

/** A position in the moving phase in which White can play 0↔1 and Black 23↔22. */
fun createBackAndForthState() = createState(listOf(0, 4, 9, 13), 0, listOf(10, 12, 20, 23), 0, White)

/** Plays White 0↔1 and Black 23↔22 back and forth, so the position repeats after these four moves. */
fun GameState.playBackAndForth() = listOf(
    PushMove(White, 0.asFieldIndex, 1.asFieldIndex),
    PushMove(Black, 23.asFieldIndex, 22.asFieldIndex),
    PushMove(White, 1.asFieldIndex, 0.asFieldIndex),
    PushMove(Black, 22.asFieldIndex, 23.asFieldIndex)
).fold(this) { state, move -> state.play(move) }

/** The position of [createBackAndForthState] for the third time. */
fun createRepeatedState() = createBackAndForthState().playBackAndForth().playBackAndForth()

/** Parses space separated field numbers, an empty string gives no fields. */
fun String.toFields() = if (isBlank()) emptyList() else split(" ").map { it.toInt() }
