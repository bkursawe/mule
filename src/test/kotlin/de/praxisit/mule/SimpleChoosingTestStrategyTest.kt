package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleChoosingTestStrategyTest {
    inner class TestStrategy(private val field1: Int, private val emptyField: Int? = null) : EvaluationStrategy {
        override fun evaluate(board: Board) =
            when {
                emptyField != null && board.getStone(emptyField.asFieldIndex) == Empty -> 10.0
                board.getStone(field1.asFieldIndex) != Empty                           -> 10.0
                else                                                                   -> 0.0
            }
    }

    @Test
    fun `choose the set move of an empty board`() {
        val player = Player(White, TestStrategy(5))
        val board = Board(white = player)

        val move = player.chooseMove(board)

        assertThat(move).isInstanceOf(SetMove::class.java)
        assertThat(move.toField.index).isEqualTo(5)
    }

    @Test
    fun `choose the push moves of a board`() {
        val white = Player(White, TestStrategy(10, 3))
        val board = Board().copy(white = white)
            .draw(createSetMove(White, 0))
            .draw(createSetMove(White, 1))
            .draw(createSetMove(White, 2))
            .draw(createSetMove(White, 3))
            .draw(createSetMove(White, 4))
            .draw(createSetMove(White, 5))
            .draw(createSetMove(White, 6))
            .draw(createSetMove(White, 7))
            .draw(createSetMove(White, 8))

        val move = board.activePlayer.chooseMove(board)

        assertThat(move).isInstanceOf(PushMove::class.java)
        assertThat(move.toField.index).isEqualTo(10)
        assertThat((move as? PushMove)?.fromField?.index).isEqualTo(3)
    }

    private fun createSetMove(color: Color, field: Int) = SetMove(color, field.asFieldIndex)
}
