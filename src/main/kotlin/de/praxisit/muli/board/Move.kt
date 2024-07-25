package de.praxisit.muli.board

sealed class Move(val color: Color, val toField: Int)

class SetMove(color: Color, toField: Int) : Move(color, toField)

class PushMove(color: Color, val fromField: Int, toField: Int) : Move(color, toField)

class JumpMove(color: Color, val fromField: Int, toField: Int) : Move(color, toField)

