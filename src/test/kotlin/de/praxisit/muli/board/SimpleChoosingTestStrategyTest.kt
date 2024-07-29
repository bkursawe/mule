package de.praxisit.muli.board

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleChoosingTestStrategyTest {
    inner class TestStrategy(private val toField: Int, private val fromField: Int? = null) : EvaluationStrategy {
        override fun evaluate(board: Board, move: Move): Double {
            return when (move) {
                is SetMove           -> if (move.toField == toField) 10.0 else 0.0
                is MoveWithFromField -> if (move.toField == toField && move.fromField == fromField) 10.0 else 0.0
                else                 -> 0.0
            }
        }
    }

    @Test
    fun `choose the set move of an empty board`() {
        val player = Player(White, TestStrategy(5))
        val board = Board(white = player)

        val move = player.chooseMove(board)

        assertThat(move).isInstanceOf(SetMove::class.java)
        assertThat(move!!.toField).isEqualTo(5)
    }

    @Test
    fun `choose the push moves of a board`() {
        val white = Player(White, TestStrategy(10, 3))
        val board = Board().copy(white = white)
            .draw(SetMove(White, 0))
            .draw(SetMove(White, 1))
            .draw(SetMove(White, 2))
            .draw(SetMove(White, 3))
            .draw(SetMove(White, 4))
            .draw(SetMove(White, 5))
            .draw(SetMove(White, 6))
            .draw(SetMove(White, 7))
            .draw(SetMove(White, 8))

        val move = board.activePlayer.chooseMove(board)

        assertThat(move).isInstanceOf(PushMove::class.java)
        assertThat(move!!.toField).isEqualTo(10)
        assertThat((move as? PushMove)?.fromField).isEqualTo(3)
    }
}
