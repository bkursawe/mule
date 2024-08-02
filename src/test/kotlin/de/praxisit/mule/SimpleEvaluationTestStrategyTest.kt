package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleEvaluationTestStrategyTest {
    private val strategy = SimpleEvaluationStrategy()

    @Test
    fun `evaluate move on empty board`() {
        val board = Board()
        val move = createSetMove(White, 0)

        val points = strategy.evaluate(board)

        assertThat(points).isBetween(2.0, 3.0)
    }

    @Test
    fun `evaluate move with potential mill`() {
        val board = Board().setStone(0, White)
        val move = createSetMove(White, 1)

        val points = strategy.evaluate(board)

        assertThat(points).isBetween(3.0, 4.0)
    }

    private fun createSetMove(color: Color, field: Int) = SetMove(color, field.asFieldIndex)
    private fun Board.setStone(field: Int, color: Color) = setStone(field.asFieldIndex, color)
}