package de.praxisit.muli.board

enum class Color {
    NONE,
    WHITE,
    BLACK;

    val opposite: Color
        get() = when (this) {
            WHITE -> BLACK
            BLACK -> WHITE
            NONE  -> NONE
        }
}
