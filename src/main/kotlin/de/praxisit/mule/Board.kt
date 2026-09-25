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
 * The stones on the 24 fields. A board is immutable: every change returns a new board.
 */
class Board private constructor(private val fields: Array<Field>) {

    constructor() : this(Array(FieldIndex.SIZE) { _ -> Empty })

    fun getStone(field: FieldIndex): Field = fields[field.index]

    fun fieldsIndicesWithColor(color: Color) =
        fields.withIndex().filter { it.value == color }.map { it.index.asFieldIndex }.toSet()

    val emptyFieldsIndices: Set<FieldIndex> by lazy {
        fields.withIndex().filter { it.value == Empty }.map { it.index.asFieldIndex }.toSet()
    }

    fun setStone(color: Color, field: FieldIndex) = withField(field, color)

    fun removeStone(field: FieldIndex) = withField(field, Empty)

    fun moveStone(fromIndex: FieldIndex, toIndex: FieldIndex): Board {
        require(fields[fromIndex.index] != Empty)
        require(fields[toIndex.index] == Empty)

        val newFields = fields.copyOf()
        newFields[toIndex.index] = fields[fromIndex.index]
        newFields[fromIndex.index] = Empty
        return Board(newFields)
    }

    private fun withField(field: FieldIndex, value: Field): Board {
        val newFields = fields.copyOf()
        newFields[field.index] = value
        return Board(newFields)
    }

    fun connectedEmptyFields(field: FieldIndex) = CONNECTIONS[field.index].filter { fields[it.index] == Empty }

    // Stones in a mule may only be captured if every stone is in a mule
    fun capturablePieces(color: Color): Set<FieldIndex> {
        val stones = fieldsIndicesWithColor(color)
        return stones.filter { !willCloseMule(it, color) }.toSet().ifEmpty { stones }
    }

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

    fun weightedStonesOnBoard(color: Color) = fields.indices.filter { fields[it] == color }.sumOf { WEIGHTED_POSITIONS[it] }

    private val Int.asField: Field
        get() = fields[this]

    private val FieldIndex.asField: Field
        get() = fields[this.index]

    override fun equals(other: Any?) =
        this === other || other is Board && hashCode() == other.hashCode() && fields.contentEquals(other.fields)

    override fun hashCode() = fieldsHash

    private val fieldsHash: Int by lazy { fields.contentHashCode() }

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

        val COMPLETABLE_MULES: List<List<Pair<FieldIndex, FieldIndex>>> =
            FieldIndex.INDEXES.map { fieldIndex ->
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

        val WEIGHTED_POSITIONS = CONNECTIONS.map { it.size }
    }
}
