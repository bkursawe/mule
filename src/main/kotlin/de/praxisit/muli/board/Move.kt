package de.praxisit.muli.board

import de.praxisit.muli.board.Board.Companion.CONNECTIONS
import de.praxisit.muli.board.Board.Companion.MAX_FIELDS
import de.praxisit.muli.board.Color.NONE

@Suppress("LeakingThis", "kotlin:S1192")
sealed class Move(val color: Color, val toField: Int, val capturedField: Int?) {
    abstract fun addCaptureField(field: Int): Move

    init {
        if (color == NONE) throw IllegalMoveException(this, "color is NONE")
        if (toField !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "toField is out of range")
        if (capturedField != null && capturedField !in 0..<MAX_FIELDS)
            throw IllegalMoveException(this, "capturedField is out of range")
    }
}

class SetMove(color: Color, toField: Int, capturedField: Int? = null) : Move(color, toField, capturedField) {
    init {
        if (toField == capturedField)
            throw IllegalMoveException(this, "toField is captured")
    }

    override fun addCaptureField(field: Int): SetMove {
        if (field !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "field not in [0, 23]")
        if (field == toField) throw IllegalMoveException(this, "field == toField")
        return SetMove(color, toField, field)
    }

    override fun toString() = "SetMove($color, $toField${capturedField?.let { ", $it" } ?: ""})"
    override fun equals(other: Any?): Boolean {
        val otherMove = other as? SetMove ?: return false
        return color == otherMove.color && toField == otherMove.toField && capturedField == otherMove.capturedField
    }

    override fun hashCode() = color.hashCode() + toField.hashCode() + capturedField.hashCode()
}

class PushMove(color: Color, val fromField: Int, toField: Int, capturedField: Int? = null) :
    Move(color, toField, capturedField) {
    init {
        if (fromField !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "fromField is out of range")
        if (fromField == toField || toField == capturedField || fromField == capturedField)
            throw IllegalMoveException(this, "duplicate fields")
        if (toField !in CONNECTIONS[fromField]) {
            throw IllegalMoveException(this, "toField is not connected to fromField")
        }
    }

    override fun addCaptureField(field: Int): PushMove {
        if (field !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "field not in [0, 23]")
        if (field == fromField) throw IllegalMoveException(this, "field == fromField")
        if (field == toField) throw IllegalMoveException(this, "field == toField")
        return PushMove(color, fromField, toField, field)
    }

    override fun toString() = "PushMove($color, $fromField -> $toField${capturedField?.let { ", $it" } ?: ""})"
    override fun equals(other: Any?): Boolean {
        val otherMove = other as? PushMove ?: return false
        return color == otherMove.color && fromField == otherMove.fromField && toField == otherMove.toField && capturedField == otherMove.capturedField
    }

    override fun hashCode() = color.hashCode() + toField.hashCode() + capturedField.hashCode()
}

class JumpMove(color: Color, val fromField: Int, toField: Int, capturedField: Int? = null) :
    Move(color, toField, capturedField) {
    init {
        if (fromField !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "fromField is out of range")
        if (fromField == toField || toField == capturedField || fromField == capturedField)
            throw IllegalMoveException(this, "duplicate fields")
    }

    override fun addCaptureField(field: Int): JumpMove {
        if (field !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "field not in [0, 23]")
        if (field == fromField) throw IllegalMoveException(this, "field == fromField")
        if (field == toField) throw IllegalMoveException(this, "field == toField")
        return JumpMove(color, fromField, toField, field)
    }

    override fun toString() = "JumpMove($color, $fromField -> $toField${capturedField?.let { ", $it" } ?: ""})"
    override fun equals(other: Any?): Boolean {
        val otherMove = other as? JumpMove ?: return false
        return color == otherMove.color && fromField == otherMove.fromField && toField == otherMove.toField && capturedField == otherMove.capturedField
    }

    override fun hashCode() = color.hashCode() + toField.hashCode() + capturedField.hashCode()
}
