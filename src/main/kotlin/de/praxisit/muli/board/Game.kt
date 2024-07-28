package de.praxisit.muli.board

class Game() {
    private var board = Board()
    private var drawNumber = 0

    fun start() {
        while (board.noLooser()) {
            println(board.printBoard())
            val move = board.chooseMove()
            print("${drawNumber++}: ")
            println(move)
            board = board.draw(move)
            board.switchPlayer()
        }
        board.showWinner()
    }
}

fun main() {
    Game().start()
}
