package de.praxisit.mule

import java.util.*


fun main() {
    Game().startComputer()
}

class Game {
    private var board = Board()
    private var drawNumber = 0

    fun startComputer() {
        while (board.hasNoLooser) {
            println(board.printedBoard)
            val move = board.chooseMove()
            print("${drawNumber++}: ")
            println(move)
            board = board.draw(move).withSwitchedPlayer
        }
        println(board.printedBoard)
        println(board.winner)

    }

    fun startHuman() {
        val humanColor = askColor()

        while (board.hasNoLooser) {
            println(board.printedBoard)
            val move = if (board.activePlayerColor == humanColor) {
                chooseMove(board)
            } else {
                board.chooseMove()
            }
            print("${drawNumber++}: ")
            println(move)
            board = board.draw(move).withSwitchedPlayer
        }
        println(board.printedBoard)
        println(board.winner)
    }

    private fun chooseMove(board: Board): Move {
        val moves = board.activePlayer.legalMoves(board)
        if (moves.isEmpty()) return NoMove

        moves.mapIndexed { index, move -> "$index: $move" }.forEach { println(it) }
        println("Choose a move by number: ")
        var answer = readln()
        while (answer.toIntOrNull() !in moves.indices) {
            println("Choose a move by number (0 .. ${moves.size}): ")
            answer = readln()
        }
        return moves[answer.toInt()]
    }

    private fun askColor(): Color {
        print("Do you want to play with (W)hite or (B)lack? ")
        val colorInput = readln()
        return when (colorInput.lowercase(Locale.getDefault())) {
            "w", "white" -> White
            "b", "black" -> Black
            else         -> throw IllegalArgumentException("Invalid color input")
        }
    }
}
