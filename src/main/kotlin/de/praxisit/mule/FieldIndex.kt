package de.praxisit.mule

@JvmInline
value class FieldIndex(val index: Int) {
    init {
        require(index in INDEX_RANGE)
    }

    companion object {
        const val SIZE = 24
        val INDEX_RANGE = 0..<SIZE
        val INDEXES = INDEX_RANGE.map { it.asFieldIndex }
        val Int.asFieldIndex get() = FieldIndex(this)
    }
}
