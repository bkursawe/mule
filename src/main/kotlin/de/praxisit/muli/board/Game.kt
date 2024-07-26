package de.praxisit.muli.board

import de.praxisit.muli.board.Color.BLACK
import de.praxisit.muli.board.Color.WHITE
import de.praxisit.muli.board.Phase.LOOSE

class Game() {
    private var white = Player(WHITE)
    private var black = Player(BLACK)
    private var board = Board()
    private var activePlayer = white

    fun start() {
        while (noLooser()) {
            println(board.printBoard())
            val move = activePlayer.chooseMove(board)
            println(move)
            if (move is SetMove) {
                activePlayer.setStone()
            }
            if (move.capturedField != null) {
                activePlayer.opposite().loseStone()
            }
            board = board.draw(move)
            activePlayer = if (activePlayer == white) black else white
        }
        showWinner()
    }

    private fun Player.opposite() = if (this == white) black else white

    private fun showWinner() {
        if (white.phase == LOOSE || white.legalMoves(board).isEmpty()) println("White is the winner")
        if (black.phase == LOOSE || black.legalMoves(board).isEmpty()) println("Black is the winner")
        println("No winner yet")
    }

    fun noLooser() =
        white.phase != LOOSE && black.phase != LOOSE && activePlayer.legalMoves(board).isNotEmpty()

}

fun main() {
    Game().start()
}