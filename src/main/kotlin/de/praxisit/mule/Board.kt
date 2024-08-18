package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import de.praxisit.mule.Phase.LOOSE

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
    private val white: Player = Player(White),
    private val black: Player = Player(Black),
    internal val activePlayerColor: Color = White,
    private val history: List<Board> = emptyList()
) {

    fun copy(
        fields: Array<Field> = this.fields,
        white: Player = this.white,
        black: Player = this.black,
        activePlayerColor: Color = this.activePlayerColor,
        history: List<Board> = this.history
    ) = Board(fields, white, black, activePlayerColor, history)

    fun fieldsIndicesWithColor(color: Color) =
        fields.withIndex().filter { it.value == color }.map { it.index.asFieldIndex }.toSet()

    val emptyFieldsIndices: Set<FieldIndex> by lazy {
        fields.withIndex().filter { it.value == Empty }.map { it.index.asFieldIndex }.toSet()
    }

    fun draw(move: Move): Board {
        var board = copy(history = history + this)
        if (move is SetMove) board = board.playerSetStone()

        board = when (move) {
            is SetMove -> board.setStone(move.color, move.toField)
            is PushMove -> board.moveStone(move.fromField, move.toField)
            is JumpMove -> board.moveStone(move.fromField, move.toField)
            is NoMove  -> throw IllegalMoveException(NoMove, "Cannot move")
        }

        if (move.capturedField != null) board = board.playerLooseStone().removeStone(move.capturedField)
        return board
    }

    val activePlayer: Player by lazy { if (activePlayerColor == White) white else black }

    val legalMoves: List<Move> by lazy { activePlayer.legalMoves(this) }

    val evaluation: Double by lazy { activePlayer.evaluate(this) }

    private fun playerLooseStone() = if (activePlayerColor == White)
        copy(black = black.loseStone())
    else
        copy(white = white.loseStone())

    private fun playerSetStone() = if (activePlayerColor == White)
        copy(white = white.setStone())
    else
        copy(black = black.setStone())

    internal val withSwitchedPlayer: Board by lazy { copy(activePlayerColor = activePlayerColor.opposite) }

    fun setStone(color: Color, index: FieldIndex): Board {
        val board = copy(fields = fields.copyOf())
        board.fields[index.index] = color
        return board
    }

    private fun removeStone(field: FieldIndex): Board {
        val board = copy(fields = fields.copyOf())
        board.fields[field.index] = Empty
        return board
    }

    fun getStone(field: FieldIndex): Field {
        return fields[field.index]
    }

    fun moveStone(fromIndex: FieldIndex, toIndex: FieldIndex): Board {
        require(fields[fromIndex.index] != Empty)
        require(fields[toIndex.index] == Empty)

        val board = copy(fields = fields.copyOf())
        board.fields[toIndex.index] = fields[fromIndex.index]
        board.fields[fromIndex.index] = Empty
        return board
    }

    fun connectedEmptyFields(field: FieldIndex) = CONNECTIONS[field.index].filter { fields[it.index] == Empty }

    fun chooseMove() = activePlayer.chooseMove(this)

    val isRepeated: Boolean by lazy { history.count { it == this } >= 2 }

    private fun f(index: Int) = when (fields[index]) {
        Empty -> "O"
        White -> "W"
        Black -> "B"
    }

    val printedBoard: String by lazy {
        """
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
             ${if (activePlayer == white) "*" else " "} White: stones = ${white.stones} phase = ${white.phase}
             ${if (activePlayer == black) "*" else " "} Black: stones = ${black.stones} phase = ${black.phase}
        """.trimIndent()
    }

    fun capturablePieces(color: Color) = fieldsIndicesWithColor(color).filter { !willCloseMule(it, color) }.toSet()

    fun willCloseMule(field: FieldIndex, color: Color): Boolean {
        val mules = COMPLETABLE_MULES[field.index]
        val firstMule = mules.first()
        val secondMule = mules.last()
        return firstMule.first.index.asField == color && firstMule.second.index.asField == color ||
                secondMule.first.index.asField == color && secondMule.second.index.asField == color
    }

    fun willCloseMule(fromField: FieldIndex, toField: FieldIndex, color: Color): Boolean {
        val mules = COMPLETABLE_MULES[toField.index]
        val firstMule = mules.first()
        val secondMule = mules.last()
        fun checkMule(firstMule: Pair<FieldIndex, FieldIndex>) =
            firstMule.first != fromField && firstMule.first.index.asField == color &&
                    firstMule.second != fromField && firstMule.second.index.asField == color
        return checkMule(firstMule) || checkMule(secondMule)
    }

    fun imcompleteMillCount(color: Color) = emptyFieldsIndices.count { field -> willCloseMule(field, color) }

    fun muleCount(color: Color) = MULES.count { mule -> mule.all { field -> field.asField == color } }

    private val Int.asField: Field
        get() = fields[this]

    private val FieldIndex.asField: Field
        get() = fields[this.index]

    val winner
        get() = when {
            white.phase == LOOSE || white.legalMoves(this).isEmpty() -> "Black is the winner"
            black.phase == LOOSE || black.legalMoves(this).isEmpty() -> "White is the winner"
            isRepeated                                               -> "It's a remis"
            else                                                     -> "No winner yet"
        }

    val hasNoLooser get() = white.phase != LOOSE && black.phase != LOOSE && activePlayer.legalMoves(this).isNotEmpty()

    fun stonesOnBoard(color: Color) = fields.count { field -> field == color }

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
        ).map { list -> list.map { field -> field.asFieldIndex } }

        val COMPLETABLE_MULES: List<List<Pair<FieldIndex, FieldIndex>>>
            get() = FieldIndex.INDEXES.map { fieldIndex ->
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
        ).map { list -> list.map { field -> field.asFieldIndex } }.toTypedArray()
    }
}