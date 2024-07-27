package de.praxisit.muli.board

import de.praxisit.muli.board.Color.*
import de.praxisit.muli.board.Phase.LOOSE

//
//   0--------1--------2
//   |        |        |
//   |  3-----4-----5  |
//   |  |     |     |  |
//   |  |  6--7--8  |  |
//   |  |  |     |  |  |
//   9-10-11    12-13-14
//   |  |  |     |  |  |
//   |  | 15-16-17  |  |
//   |  |     |     |  |
//   | 18----19----20  |
//   |        |        |
//  21-------22-------23
//
class Board {
    private val fields: Array<Color>
    private val mules: Set<Int>
    private val white: Player
    private val black: Player
    val activePlayerColor: Color

    constructor() {
        fields = Array(24) { _ -> NONE }
        mules = emptySet()
        white = Player(WHITE)
        black = Player(BLACK)
        activePlayerColor = WHITE
    }

    constructor(initFields: Array<Color>, initMules: Set<Int>, white: Player, black: Player, activePlayer: Color) {
        fields = initFields.copyOf()
        mules = initMules
        this.white = white
        this.black = black
        this.activePlayerColor = activePlayer
    }

    fun fieldsIndicesWithColor(color: Color) = fields.withIndex().filter { it.value == color }.map { it.index }.toSet()
    fun emptyFieldsIndices() = fieldsIndicesWithColor(NONE)

    fun draw(move: Move): Board {
        var board = this
        if (move is SetMove) board = board.playerSetStone()

        board = when (move) {
            is SetMove  -> board.setStone(move.toField, move.color)
            is PushMove -> board.moveStone(move.fromField, move.toField)
            is JumpMove -> board.moveStone(move.fromField, move.toField)
        }

        if (move.capturedField != null) board = playerLooseStone().setStone(move.capturedField, NONE)
        return board.changePlayer()
    }

    private fun playerLooseStone() = if (activePlayerColor == WHITE)
        Board(fields, mules, white, black.loseStone(), activePlayerColor)
    else
        Board(fields, mules, white.loseStone(), black, activePlayerColor)

    private fun playerSetStone() = if (activePlayerColor == WHITE)
        Board(fields, mules, white.setStone(), black, activePlayerColor)
    else
        Board(fields, mules, white, black.setStone(), activePlayerColor)

    private fun Color.opposite() = if (this == WHITE) BLACK else WHITE

    private fun Color.player() = if (this == WHITE) white else black

    internal fun changePlayer() = Board(fields, mules, white, black, activePlayerColor.opposite())

    fun setStone(index: Int, color: Color): Board {
        val board = Board(fields, mules, white, black, activePlayerColor)
        board.fields[index] = color
        return board
    }

    fun getStone(index: Int): Color {
        return fields[index]
    }

    fun moveStone(fromIndex: Int, toIndex: Int): Board {
        require(fields[fromIndex] != NONE)
        require(fields[toIndex] == NONE)

        val board = Board(fields, mules, white, black, activePlayerColor)
        board.fields[toIndex] = fields[fromIndex]
        board.fields[fromIndex] = NONE
        return board
    }

    fun connectedEmptyFields(index: Int) = CONNECTIONS[index].filter { fields[it] == NONE }

    fun chooseMove() = activePlayerColor.player().chooseMove(this)

    fun printBoard(): String {
        fun f(index: Int) = when (fields[index]) {
            NONE  -> "O"
            WHITE -> "W"
            BLACK -> "B"
        }
        return """
             ${f(0)}--------${f(1)}--------${f(2)}
             |        |        |
             |  ${f(3)}-----${f(4)}-----${f(5)}  |
             |  |     |     |  |
             |  |  ${f(6)}--${f(7)}--${f(8)}  |  |
             |  |  |     |  |  |
             ${f(9)}--${f(10)}--${f(11)}     ${f(12)}--${f(13)}--${f(14)}
             |  |  |     |  |  |
             |  |  ${f(15)}--${f(16)}--${f(17)}  |  |
             |  |     |     |  |
             |  ${f(18)}-----${f(19)}-----${f(20)}  |
             |        |        |
             ${f(21)}--------${f(22)}--------${f(23)}
        """.trimIndent()
    }

    fun capturablePieces(color: Color): Set<Int> {
        return fieldsIndicesWithColor(color).filter { !willCloseMule(it, color) }.toSet()
    }

    fun willCloseMule(field: Int, color: Color) =
        COMPLETABLE_MULES[field].first().first.color == color &&
                COMPLETABLE_MULES[field].first().second.color == color ||
                COMPLETABLE_MULES[field].last().first.color == color &&
                COMPLETABLE_MULES[field].last().second.color == color

    private val Int.color: Color
        get() = fields[this]

    fun showWinner() {
        if (white.phase == LOOSE || white.legalMoves(this).isEmpty()) println("Black is the winner")
        else if (black.phase == LOOSE || black.legalMoves(this).isEmpty()) println("White is the winner")
        else println("No winner yet")
    }

    fun noLooser() =
        white.phase != LOOSE && black.phase != LOOSE && activePlayerColor.player().legalMoves(this).isNotEmpty()


    companion object {
        val MULES = arrayOf(
            listOf(0, 1, 2),
            listOf(3, 4, 5),
            listOf(6, 7, 8),
            listOf(9, 10, 11),
            listOf(12, 13, 14),
            listOf(15, 16, 17),
            listOf(18, 19, 20),
            listOf(21, 22, 23),
            listOf(0, 9, 21),
            listOf(3, 10, 18),
            listOf(6, 11, 15),
            listOf(1, 4, 7),
            listOf(16, 19, 22),
            listOf(8, 12, 17),
            listOf(5, 13, 20),
            listOf(2, 14, 23)
        )

        val COMPLETABLE_MULES: List<List<Pair<Int, Int>>>
            get() = (0..<24).map { fieldIndex ->
                MULES.filter { mule -> fieldIndex in mule }.map { it - fieldIndex }.map { Pair(it.first(), it.last()) }
            }

        val CONNECTIONS = arrayOf(
            listOf(1, 9),
            listOf(0, 2, 4),
            listOf(1, 14),
            listOf(4, 10),
            listOf(1, 3, 5, 7),
            listOf(4, 13),
            listOf(7, 11),
            listOf(4, 6, 8),
            listOf(7, 12),
            listOf(0, 10, 21),
            listOf(3, 9, 11, 18),
            listOf(6, 10, 15),
            listOf(8, 13, 17),
            listOf(5, 12, 14, 20),
            listOf(2, 13, 23),
            listOf(11, 16),
            listOf(15, 17, 19),
            listOf(12, 16),
            listOf(10, 19),
            listOf(16, 18, 20, 22),
            listOf(13, 19),
            listOf(9, 22),
            listOf(19, 21, 23),
            listOf(14, 22)
        )

        const val MAX_FIELDS = 24
    }
}