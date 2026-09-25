package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleEvaluationTestStrategyTest {
    private val strategy = SimpleEvaluationStrategy()

    @Test
    fun `evaluate move on empty board`() {
        val board = Board()

        val points = strategy.evaluate(board)

        assertThat(points).isBetween(0.0, 1.0)
    }

    @Test
    fun `evaluate move with more white than black stones`() {
        val board = Board().setStone(0, White)

        val points = strategy.evaluate(board)

        assertThat(points).isBetween(1.0, 2.0)
    }

    private fun Board.setStone(field: Int, color: Color) = setStone(color, field.asFieldIndex)
}