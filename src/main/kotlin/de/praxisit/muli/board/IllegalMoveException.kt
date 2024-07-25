package de.praxisit.muli.board

class IllegalMoveException(val move: Move, reason: String) : Throwable(reason)
