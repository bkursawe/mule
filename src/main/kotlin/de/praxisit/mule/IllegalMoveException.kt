package de.praxisit.mule

class IllegalMoveException(val move: Move, reason: String) : Throwable(reason)
