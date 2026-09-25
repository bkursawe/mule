package de.praxisit.mule

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleEvaluationTestStrategyTest {
    private val strategy = SimpleEvaluationStrategy()

    @Test
    fun `evaluate move on empty board`() {
        val points = strategy.evaluate(Position())

        assertThat(points).isBetween(0.0, 1.0)
    }

    @Test
    fun `evaluate move with more white than black stones`() {
        val position = Position(board = Board().setStone(White, 0))

        val points = strategy.evaluate(position)

        assertThat(points).isBetween(1.0, 2.0)
    }
}
