package de.praxisit.mule

/**
 * Remembers search results by game state key. The table has a fixed size,
 * a new entry replaces the entry in its slot.
 */
internal class TranspositionTable(sizeBits: Int = 20) {
    enum class Bound { EXACT, LOWER, UPPER }

    class Entry(val key: Long, val depth: Int, val score: Double, val bound: Bound, val move: Move?)

    private val shift = Long.SIZE_BITS - sizeBits
    private val entries = arrayOfNulls<Entry>(1 shl sizeBits)

    operator fun get(key: Long): Entry? = entries[slot(key)]?.takeIf { it.key == key }

    fun store(key: Long, depth: Int, score: Double, bound: Bound, move: Move?) {
        entries[slot(key)] = Entry(key, depth, score, bound, move)
    }

    // Fibonacci hashing spreads similar keys over the whole table
    private fun slot(key: Long) = ((key * -0x61c8864680b583ebL) ushr shift).toInt()
}
