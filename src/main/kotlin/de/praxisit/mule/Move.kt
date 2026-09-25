package de.praxisit.mule

import de.praxisit.mule.Board.Companion.CONNECTIONS

sealed class Move {
    abstract val color: Color
    abstract val toField: FieldIndex
    abstract val capturedField: FieldIndex?

    abstract fun addCaptureField(field: FieldIndex): Move

    val isCaptureMove: Boolean
        get() = capturedField != null

    protected val capturedFieldText: String
        get() = capturedField?.let { ", ${it.index}" } ?: ""
}

private const val CAPTURE_FIELD_EQUAL_TO_FIELD = "field == toField"
private const val CAPTURE_FIELD_EQUAL_TO_FROM_FIELD = "field == fromField"

data class SetMove(
    override val color: Color,
    override val toField: FieldIndex,
    override val capturedField: FieldIndex? = null
) : Move() {
    init {
        if (toField == capturedField) throw IllegalMoveException(this, "toField is captured")
    }

    override fun addCaptureField(field: FieldIndex): SetMove {
        if (field == toField) throw IllegalMoveException(this, CAPTURE_FIELD_EQUAL_TO_FIELD)
        return copy(capturedField = field)
    }

    override fun toString() = "SetMove($color, ${toField.index}$capturedFieldText)"
}

sealed class MoveWithFromField : Move() {
    abstract val fromField: FieldIndex

    // Called by the init blocks of the subclasses: the properties are not initialised before
    protected fun requireDistinctFields() {
        if (fromField == toField || toField == capturedField || fromField == capturedField) {
            throw IllegalMoveException(this, "duplicate fields")
        }
    }

    protected fun requireValidCaptureField(field: FieldIndex) {
        if (field == fromField) throw IllegalMoveException(this, CAPTURE_FIELD_EQUAL_TO_FROM_FIELD)
        if (field == toField) throw IllegalMoveException(this, CAPTURE_FIELD_EQUAL_TO_FIELD)
    }
}

data class PushMove(
    override val color: Color,
    override val fromField: FieldIndex,
    override val toField: FieldIndex,
    override val capturedField: FieldIndex? = null
) : MoveWithFromField() {
    init {
        requireDistinctFields()
        if (toField !in CONNECTIONS[fromField.index]) {
            throw IllegalMoveException(this, "toField is not connected to fromField")
        }
    }

    override fun addCaptureField(field: FieldIndex): PushMove {
        requireValidCaptureField(field)
        return copy(capturedField = field)
    }

    override fun toString() = "PushMove($color, ${fromField.index} -> ${toField.index}$capturedFieldText)"
}

data class JumpMove(
    override val color: Color,
    override val fromField: FieldIndex,
    override val toField: FieldIndex,
    override val capturedField: FieldIndex? = null
) : MoveWithFromField() {
    init {
        requireDistinctFields()
    }

    override fun addCaptureField(field: FieldIndex): JumpMove {
        requireValidCaptureField(field)
        return copy(capturedField = field)
    }

    override fun toString() = "JumpMove($color, ${fromField.index} -> ${toField.index}$capturedFieldText)"
}
