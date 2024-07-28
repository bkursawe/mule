package de.praxisit.muli.board

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
class Board(
    private val fields: Array<Field> = Array(24) { _ -> Empty },
    private val mules: Set<Int> = emptySet(),
    private val white: Player = Player(White),
    private val black: Player = Player(Black),
    internal val activePlayerColor: Color = White
) {

    fun copy(
        fields: Array<Field> = this.fields,
        mules: Set<Int> = this.mules,
        white: Player = this.white,
        black: Player = this.black,
        activePlayer: Color = this.activePlayerColor
    ) = Board(fields, mules, white, black, activePlayer)

    fun fieldsIndicesWithColor(color: Color) = fields.withIndex().filter { it.value == color }.map { it.index }.toSet()
    fun emptyFieldsIndices() = fields.withIndex().filter { it.value == Empty }.map { it.index }.toSet()

    fun draw(move: Move): Board {
        var board = this
        if (move is SetMove) board = board.playerSetStone()

        board = when (move) {
            is SetMove  -> board.setStone(move.toField, move.color)
            is PushMove -> board.moveStone(move.fromField, move.toField)
            is JumpMove -> board.moveStone(move.fromField, move.toField)
            else -> error("no further move class")
        }

        if (move.capturedField != null) board = board.playerLooseStone().removeStone(move.capturedField)
        return board
    }

    val activePlayer: Player
        get() = if (activePlayerColor == White) white else black

    private fun playerLooseStone() = if (activePlayerColor == White)
        copy(black = black.loseStone())
    else
        copy(white = white.loseStone())

    private fun playerSetStone() = if (activePlayerColor == White)
        copy(white = white.setStone())
    else
        copy(black = black.setStone())

    internal fun switchPlayer() = copy(activePlayer = activePlayerColor.opposite)

    fun setStone(index: Int, color: Color): Board {
        val board = copy(fields = fields.copyOf())
        board.fields[index] = color
        return board
    }

    private fun removeStone(field: Int): Board {
        val board = copy(fields = fields.copyOf())
        board.fields[field] = Empty
        return board
    }

    fun getStone(index: Int): Field {
        return fields[index]
    }

    fun moveStone(fromIndex: Int, toIndex: Int): Board {
        require(fields[fromIndex] != Empty)
        require(fields[toIndex] == Empty)

        val board = copy(fields = fields.copyOf())
        board.fields[toIndex] = fields[fromIndex]
        board.fields[fromIndex] = Empty
        return board
    }

    fun connectedEmptyFields(index: Int) = CONNECTIONS[index].filter { fields[it] == Empty }

    fun chooseMove() = activePlayer.chooseMove(this)

    fun printBoard(): String {
        fun f(index: Int) = when (fields[index]) {
            Empty -> "O"
            White -> "W"
            Black -> "B"
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

    fun willCloseMule(field: Int, color: Color): Boolean {
        val mules = COMPLETABLE_MULES[field]
        val firstMule = mules.first()
        val secondMule = mules.last()
        return firstMule.first.field == color && firstMule.second.field == color ||
                secondMule.first.field == color && secondMule.second.field == color
    }

    fun willCloseMule(fromField: Int, toField: Int, color: Color): Boolean {
        val mules = COMPLETABLE_MULES[toField]
        val firstMule = mules.first()
        val secondMule = mules.last()
        fun checkMule(firstMule: Pair<Int, Int>) =
            firstMule.first != fromField && firstMule.first.field == color &&
                    firstMule.second != fromField && firstMule.second.field == color
        return checkMule(firstMule) || checkMule(secondMule)
    }

    fun imcompleteMillCount(color: Color) = emptyFieldsIndices().count { field -> willCloseMule(field, color) }

    private val Int.field: Field
        get() = fields[this]

    fun showWinner() {
        if (white.phase == LOOSE || white.legalMoves(this).isEmpty()) println("Black is the winner")
        else if (black.phase == LOOSE || black.legalMoves(this).isEmpty()) println("White is the winner")
        else println("No winner yet")
    }

    fun noLooser() =
        white.phase != LOOSE && black.phase != LOOSE && activePlayer.legalMoves(this).isNotEmpty()

    fun stonesOnBoard(color: Color) = fields.count { field -> field == color }

    fun potentialMill(move: Move): Int =
        MULES.filter { move.toField in it }
            .count { muleFields -> !muleFields.map { fields[it] }.any { it == move.color.opposite } }


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