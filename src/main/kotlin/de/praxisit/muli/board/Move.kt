package de.praxisit.muli.board

import de.praxisit.muli.board.Board.Companion.CONNECTIONS
import de.praxisit.muli.board.Board.Companion.MAX_FIELDS
import de.praxisit.muli.board.Color.NONE

@Suppress("LeakingThis")
sealed class Move(val color: Color, val toField: Int, val capturedField: Int?) {
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

    override fun toString() = "SetMove($color, $toField${capturedField?.let { ", $it" } ?: ""})"
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

    override fun toString() = "PushMove($color, $fromField -> $toField${capturedField?.let { ", $it" } ?: ""})"
}

class JumpMove(color: Color, val fromField: Int, toField: Int, capturedField: Int? = null) :
    Move(color, toField, capturedField) {
    init {
        if (fromField !in 0..<MAX_FIELDS) throw IllegalMoveException(this, "fromField is out of range")
        if (fromField == toField || toField == capturedField || fromField == capturedField)
            throw IllegalMoveException(this, "duplicate fields")
    }

    override fun toString() = "JumpMove($color, $fromField -> $toField${capturedField?.let { ", $it" } ?: ""})"
}

