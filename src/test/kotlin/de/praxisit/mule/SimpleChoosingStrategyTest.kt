package de.praxisit.mule

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleChoosingStrategyTest {
    class TestStrategy(private val field1: Int, private val emptyField: Int? = null) : EvaluationStrategy {
        override fun evaluate(position: Position) =
            when {
                emptyField != null && position.board.getStone(emptyField) == Empty -> 10.0
                position.board.getStone(field1) != Empty                           -> 10.0
                else                                                               -> 0.0
            }
    }

    @Test
    fun `choose the set move of an empty board`() {
        val move = SimpleChoosingStrategy(TestStrategy(5)).chooseMove(GameState())

        assertThat(move).isInstanceOf(SetMove::class.java)
        assertThat(move.toField.index).isEqualTo(5)
    }

    @Test
    fun `choose the push moves of a board`() {
        val state = createState((0..8).toList(), 0, listOf(20, 21, 22, 23), 0, White)

        val move = SimpleChoosingStrategy(TestStrategy(10, 3)).chooseMove(state)

        assertThat(move).isInstanceOf(PushMove::class.java)
        assertThat(move.toField.index).isEqualTo(10)
        assertThat((move as PushMove).fromField.index).isEqualTo(3)
    }

    @Test
    fun `black chooses the move with the lowest evaluation`() {
        val state = createState(listOf(0, 1), 7, listOf(), 9, Black)

        val move = SimpleChoosingStrategy(SimpleEvaluationStrategy()).chooseMove(state)

        assertThat(move.toField.index).isEqualTo(2)
    }
}
