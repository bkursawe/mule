package de.praxisit.mule

object Display {
    fun printedBoard(board: Board): String {
        fun f(index: Int) = when (board.fields[index]) {
            Empty -> "O"
            White -> "W"
            Black -> "B"
        }

        return """
             ${f(0)}--------${f(1)}--------${f(2)}
             |        |        |
             |  ${f(3)}-----${f(4)}-----${f(5)}  |
             |  |     |     |  |
             |  |  ${f(6)}--${f(7)}--${f(8)}  |  |
             |  |  |     |  |  |
             ${f(9)}--${f(10)}--${f(11)}     ${f(12)}--${f(13)}--${f(14)}
             |  |  |     |  |  |
             |  |  ${f(15)}--${f(16)}--${f(17)}  |  |
             |  |     |     |  |
             |  ${f(18)}-----${f(19)}-----${f(20)}  |
             |        |        |
             ${f(21)}--------${f(22)}--------${f(23)}
             ${if (board.activePlayer == board.white) "*" else " "} White: stones = ${board.white.stones} phase = ${board.white.phase}
             ${if (board.activePlayer == board.black) "*" else " "} Black: stones = ${board.black.stones} phase = ${board.black.phase}
             Evaluation: ${board.evaluation}
             
        """.trimIndent()
    }
}