package de.praxisit.mule

@JvmInline
value class FieldIndex(val index: Int) {
    init {
        require(index in INDEX_RANGE)
    }

    /** The bit of this field in a bit mask over all fields. */
    val bit: Int
        get() = 1 shl index

    companion object {
        const val SIZE = 24
        val INDEX_RANGE = 0..<SIZE
        val INDEXES = INDEX_RANGE.map { it.asFieldIndex }
        val Int.asFieldIndex get() = FieldIndex(this)
    }
}

/** Calls [action] for every field of this bit mask in ascending order. */
inline fun Int.forEachField(action: (FieldIndex) -> Unit) {
    var remaining = this
    while (remaining != 0) {
        action(FieldIndex(Integer.numberOfTrailingZeros(remaining)))
        remaining = remaining and (remaining - 1)
    }
}

/** The fields of this bit mask in ascending order. */
fun Int.toFieldIndices(): Set<FieldIndex> {
    val fields = LinkedHashSet<FieldIndex>()
    forEachField { fields.add(it) }
    return fields
}
