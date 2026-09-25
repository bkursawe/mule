package de.praxisit.mule

import de.praxisit.mule.Board.Companion.CONNECTIONS
import de.praxisit.mule.FieldIndex.Companion.asFieldIndex

@Suppress("kotlin:S1192")
sealed class Move(val color: Color, val toField: FieldIndex, val capturedField: FieldIndex?) {
    abstract fun addCaptureField(field: FieldIndex): Move

    val isCaptureMove: Boolean
        get() = capturedField != null
}

data object NoMove : Move(White, 0.asFieldIndex, null) {
    override fun addCaptureField(field: FieldIndex): Move {
        throw IllegalStateException("Cannot add a capture to NoMove")
    }
}

private const val CAPTURE_FIELD_EQUAL_TO_FIELD = "field == toField"

class SetMove(color: Color, toField: FieldIndex, capturedField: FieldIndex? = null) :
    Move(color, toField, capturedField) {
    init {
        if (toField == capturedField)
            throw IllegalMoveException(this, "toField is captured")
    }

    override fun addCaptureField(field: FieldIndex): SetMove {
        if (field == toField) throw IllegalMoveException(this, CAPTURE_FIELD_EQUAL_TO_FIELD)
        return SetMove(color, toField, field)
    }

    override fun toString() = "SetMove($color, ${toField.index}${capturedField?.let { ", ${it.index}" } ?: ""})"
    override fun equals(other: Any?): Boolean {
        val otherMove = other as? SetMove ?: return false
        return color == otherMove.color && toField == otherMove.toField && capturedField == otherMove.capturedField
    }

    override fun hashCode() = color.hashCode() + toField.hashCode() + capturedField.hashCode()
}

@Suppress("LeakingThis")
sealed class MoveWithFromField(
    color: Color,
    val fromField: FieldIndex,
    toField: FieldIndex,
    capturedField: FieldIndex? = null
) :
    Move(color, toField, capturedField) {
    init {
        if (fromField == toField || toField == capturedField || fromField == capturedField)
            throw IllegalMoveException(this, "duplicate fields")
    }
}

class PushMove(color: Color, fromField: FieldIndex, toField: FieldIndex, capturedField: FieldIndex? = null) :
    MoveWithFromField(color, fromField, toField, capturedField) {
    init {
        if (toField !in CONNECTIONS[fromField.index]) {
            throw IllegalMoveException(this, "toField is not connected to fromField")
        }
    }

    override fun addCaptureField(field: FieldIndex): PushMove {
        if (field == fromField) throw IllegalMoveException(this, "field == fromField")
        if (field == toField) throw IllegalMoveException(this, CAPTURE_FIELD_EQUAL_TO_FIELD)
        return PushMove(color, fromField, toField, field)
    }

    override fun toString() =
        "PushMove($color, ${fromField.index} -> ${toField.index}${capturedField?.let { ", ${it.index}" } ?: ""})"
    override fun equals(other: Any?): Boolean {
        val otherMove = other as? PushMove ?: return false
        return color == otherMove.color && fromField == otherMove.fromField && toField == otherMove.toField && capturedField == otherMove.capturedField
    }

    override fun hashCode() = color.hashCode() + toField.hashCode() + capturedField.hashCode()
}

class JumpMove(color: Color, fromField: FieldIndex, toField: FieldIndex, capturedField: FieldIndex? = null) :
    MoveWithFromField(color, fromField, toField, capturedField) {
    override fun addCaptureField(field: FieldIndex): JumpMove {
        if (field == fromField) throw IllegalMoveException(this, "field == fromField")
        if (field == toField) throw IllegalMoveException(this, CAPTURE_FIELD_EQUAL_TO_FIELD)
        return JumpMove(color, fromField, toField, field)
    }

    override fun toString() =
        "JumpMove($color, ${fromField.index} -> ${toField.index}${capturedField?.let { ", ${it.index}" } ?: ""})"
    override fun equals(other: Any?): Boolean {
        val otherMove = other as? JumpMove ?: return false
        return color == otherMove.color && fromField == otherMove.fromField && toField == otherMove.toField && capturedField == otherMove.capturedField
    }

    override fun hashCode() = color.hashCode() + toField.hashCode() + capturedField.hashCode()
}
