package de.praxisit.mule

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import kotlin.random.Random

class WeightedRandomStrategyTest {
    @Test
    fun `choose a legal move`() {
        val state = createState(listOf(0, 4, 9, 13, 19), 0, listOf(1, 10, 12, 20), 0, Black)

        val move = WeightedRandomStrategy(depth = 2, temperature = 10.0).chooseMove(state)

        assertThat(state.legalMoves).contains(move)
    }

    @Test
    fun `close a mule whenever possible`() {
        // White closes 0-1-2 by pushing 14 to 2
        val state = createState(listOf(0, 1, 14, 20), 0, listOf(3, 4, 5, 22), 0, White)

        val moves = (1..20).map { seed -> strategy(seed).chooseMove(state) }

        assertThat(moves).allMatch { it.isCaptureMove && it.toField.index == 2 }
    }

    @Test
    fun `vary between moves of about the same value`() {
        val moves = (1..20).map { seed -> strategy(seed).chooseMove(GameState()) }

        assertThat(moves.distinct()).hasSizeGreaterThan(1)
    }

    @Test
    fun `the same random numbers give the same moves`() {
        val state = GameState()

        assertThat(strategy(7).chooseMove(state)).isEqualTo(strategy(7).chooseMove(state))
    }

    @Test
    fun `the temperature must be positive`() {
        assertThatThrownBy { WeightedRandomStrategy(depth = 1, temperature = 0.0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun strategy(seed: Int) = WeightedRandomStrategy(depth = 1, temperature = 10.0, random = Random(seed))
}
