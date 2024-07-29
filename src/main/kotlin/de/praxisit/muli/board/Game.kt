package de.praxisit.muli.board

import java.util.*


fun main() {
    Game().start()
}

class Game() {
    private var board = Board()
    private var drawNumber = 0


    fun start() {
        val humanColor = askColor()

        while (board.noLooser()) {
            println(board.printBoard())
            val move = if (board.activePlayerColor == humanColor) {
                chooseMove(board)
            } else {
                board.chooseMove()
            }
            if (move != null) {
                print("${drawNumber++}: ")
                println(move)
                board = board.draw(move).switchPlayer()
            }
        }
        println(board.showWinner())
    }

    private fun chooseMove(board: Board): Move? {
        val moves = board.activePlayer.legalMoves(board)
        if (moves.isEmpty()) return null

        moves.mapIndexed { index, move -> "$index: $move" }.forEach { println(it) }
        println("Choose a move by number: ")
        var answer = readln()
        while (answer.toIntOrNull() !in moves.indices) {
            println("Choose a move by number (0 .. ${moves.size}): ")
            answer = readln()
        }
        return moves[answer.toInt()]
    }

    fun askColor(): Color {
        print("Do you want to play with (W)hite or (B)lack? ")
        val colorInput = readln()
        when (colorInput.lowercase(Locale.getDefault())) {
            "w", "white" -> return White
            "b", "black" -> return Black
            else         -> throw IllegalArgumentException("Invalid color input")
        }
    }
}
