package de.praxisit.muli.board

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleEvaluationStrategyTest {
    private val strategy = SimpleEvaluationStrategy()

    @Test
    fun `evaluate move on empty board`() {
        val board = Board()
        val move = SetMove(White, 0)

        val points = strategy.evaluate(board, move)

        assertThat(points).isBetween(2.0, 3.0)
    }

    @Test
    fun `evaluate move with potential mill`() {
        val board = Board().setStone(0, White)
        val move = SetMove(White, 1)

        val points = strategy.evaluate(board, move)

        assertThat(points).isBetween(3.0, 4.0)
    }
}