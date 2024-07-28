package de.praxisit.muli.board

sealed class Field

data object Empty : Field()

sealed class Color : Field() {
    abstract val opposite: Color
}

data object White : Color() {
    override val opposite: Color = Black
}

data object Black : Color() {
    override val opposite: Color = White
}

