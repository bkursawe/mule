package de.praxisit.muli.board

import de.praxisit.muli.board.Color.BLACK
import de.praxisit.muli.board.Color.WHITE
import de.praxisit.muli.board.Phase.LOOSE

class Game() {
    private val board = Board()
    private val white = Player(WHITE)
    private val black = Player(BLACK)
    private var activePlayer = white

    fun start() {
        while (noLooser()) {
            val move = activePlayer.chooseMove()
            board.draw(move)
            activePlayer = if (activePlayer == white) black else white
        }
        showWinner()
    }

    private fun showWinner() {
        if (white.phase == LOOSE || white.legalMoves(board).isEmpty()) println("White is the winner")
        if (black.phase == LOOSE || black.legalMoves(board).isEmpty()) println("Black is the winner")
    }

    fun noLooser() =
        white.phase != LOOSE && black.phase != LOOSE && activePlayer.legalMoves(board).isNotEmpty()

}