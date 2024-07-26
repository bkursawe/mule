package de.praxisit.muli.board

import de.praxisit.muli.board.Board.Companion.CONNECTIONS
import de.praxisit.muli.board.Board.Companion.MAX_FIELDS
import de.praxisit.muli.board.Color.NONE

@Suppress("LeakingThis", "kotlin:S1192")
sealed class Move(val color: Color, val toField: Int, val capturedField: Int?) {
    abstract fun addCaptureField(captureField: Int): Move

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

    override fun addCaptureField(captureField: Int): SetMove {
        if (captureField !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "captureField not in [0, 23]")
        if (captureField == toField) throw IllegalMoveException(this, "captureField == toField")
        return SetMove(color, toField, captureField)
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

    override fun addCaptureField(captureField: Int): PushMove {
        if (captureField !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "captureField not in [0, 23]")
        if (captureField == fromField) throw IllegalMoveException(this, "captureField == fromField")
        if (captureField == toField) throw IllegalMoveException(this, "captureField == toField")
        return PushMove(color, fromField, toField, captureField)
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

    override fun addCaptureField(captureField: Int): JumpMove {
        if (captureField !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "captureField not in [0, 23]")
        if (captureField == fromField) throw IllegalMoveException(this, "captureField == fromField")
        if (captureField == toField) throw IllegalMoveException(this, "captureField == toField")
        return JumpMove(color, fromField, toField, captureField)
    }

    override fun toString() = "JumpMove($color, $fromField -> $toField${capturedField?.let { ", $it" } ?: ""})"
    override fun equals(other: Any?): Boolean {
        val otherMove = other as? JumpMove ?: return false
        return color == otherMove.color && fromField == otherMove.fromField && toField == otherMove.toField && capturedField == otherMove.capturedField
    }

    override fun hashCode() = color.hashCode() + toField.hashCode() + capturedField.hashCode()
}
