package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex

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
/**
 * The stones on the 24 fields as one bit mask per color, bit i stands for field i.
 * A board is immutable: every change returns a new board.
 */
class Board private constructor(private val whiteStones: Int, private val blackStones: Int) {

    constructor() : this(0, 0)

    /** The fields with a stone of [color] as a bit mask. */
    fun stones(color: Color) = if (color == White) whiteStones else blackStones

    /** The empty fields as a bit mask. */
    val emptyFields: Int
        get() = ALL_FIELDS and (whiteStones or blackStones).inv()

    fun getStone(field: FieldIndex): Field = when {
        whiteStones and field.bit != 0 -> White
        blackStones and field.bit != 0 -> Black
        else                           -> Empty
    }

    fun fieldsIndicesWithColor(color: Color) = stones(color).toFieldIndices()

    val emptyFieldsIndices: Set<FieldIndex>
        get() = emptyFields.toFieldIndices()

    fun setStone(color: Color, field: FieldIndex): Board {
        val board = removeStone(field)
        return if (color == White) {
            Board(board.whiteStones or field.bit, board.blackStones)
        } else {
            Board(board.whiteStones, board.blackStones or field.bit)
        }
    }

    fun removeStone(field: FieldIndex) = Board(whiteStones and field.bit.inv(), blackStones and field.bit.inv())

    fun moveStone(fromIndex: FieldIndex, toIndex: FieldIndex): Board {
        val color = getStone(fromIndex)
        require(color is Color)
        require(getStone(toIndex) == Empty)

        return removeStone(fromIndex).setStone(color, toIndex)
    }

    // Stones in a mule may only be captured if every stone is in a mule
    fun capturablePieces(color: Color): Set<FieldIndex> {
        val stones = stones(color)
        val outsideMules = stones and stonesInMules(stones).inv()
        return (if (outsideMules != 0) outsideMules else stones).toFieldIndices()
    }

    private fun stonesInMules(stones: Int) =
        MULE_MASKS.fold(0) { inMules, mule -> if (stones and mule == mule) inMules or mule else inMules }

    fun willCloseMule(field: FieldIndex, color: Color) = closesMule(stones(color), field)

    fun willCloseMule(fromField: FieldIndex, toField: FieldIndex, color: Color) =
        closesMule(stones(color) and fromField.bit.inv(), toField)

    // A stone on field closes a mule if the other two fields of one of its mules are occupied by stones
    private fun closesMule(stones: Int, field: FieldIndex) = MULES_OF_FIELD[field.index].any { mule ->
        val otherFields = mule and field.bit.inv()
        stones and otherFields == otherFields
    }

    fun openMuleCount(color: Color) = Integer.bitCount(closingFields(color))

    /** The empty fields on which a stone of [color] would close a mule, as a bit mask. */
    fun closingFields(color: Color): Int {
        val stones = stones(color)
        val emptyFields = emptyFields
        var closingFields = 0
        for (mule in MULE_MASKS) {
            if (Integer.bitCount(stones and mule) == 2) closingFields = closingFields or (mule and emptyFields)
        }
        return closingFields
    }

    fun muleCount(color: Color): Int {
        val stones = stones(color)
        return MULE_MASKS.count { mule -> stones and mule == mule }
    }

    fun weightedStonesOnBoard(color: Color): Int {
        var weight = 0
        stones(color).forEachField { weight += WEIGHTED_POSITIONS[it.index] }
        return weight
    }

    override fun equals(other: Any?) =
        this === other || other is Board && whiteStones == other.whiteStones && blackStones == other.blackStones

    override fun hashCode() = 31 * whiteStones + blackStones

    companion object {
        private const val ALL_FIELDS = 0xFFFFFF

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

        val COMPLETABLE_MULES: List<List<Pair<FieldIndex, FieldIndex>>> =
            FieldIndex.INDEXES.map { fieldIndex ->
                MULES.filter { mule -> fieldIndex in mule }.map { it - fieldIndex }.map { Pair(it.first(), it.last()) }
            }

        private val MULE_MASKS: IntArray = MULES.map { it.toMask() }.toIntArray()

        private val MULES_OF_FIELD: Array<IntArray> = FieldIndex.INDEXES.map { field ->
            MULE_MASKS.filter { mule -> mule and field.bit != 0 }.toIntArray()
        }.toTypedArray()

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

        /** The connected fields of every field as a bit mask. */
        val NEIGHBORS: IntArray = CONNECTIONS.map { it.toMask() }.toIntArray()

        val WEIGHTED_POSITIONS = CONNECTIONS.map { it.size }.toIntArray()

        private fun List<FieldIndex>.toMask() = fold(0) { mask, field -> mask or field.bit }
    }
}
