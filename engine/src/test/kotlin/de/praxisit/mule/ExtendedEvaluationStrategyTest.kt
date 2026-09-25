package de.praxisit.mule

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtendedEvaluationStrategyTest {
    private val strategy = ExtendedEvaluationStrategy(
        stoneWeight = 100.0,
        muleWeight = 10.0,
        closableMuleWeight = 20.0,
        mobilityWeight = 2.0
    )

    @Test
    fun `the start position is even`() {
        assertThat(strategy.evaluate(Position())).isEqualTo(0.0)
    }

    @Test
    fun `the evaluation is symmetric`() {
        val state = createState(listOf(0, 1, 14, 20), 0, listOf(4, 10, 16), 0, White)
        val mirrored = createState(listOf(4, 10, 16), 0, listOf(0, 1, 14, 20), 0, White)

        assertThat(strategy.evaluate(mirrored.position)).isEqualTo(-strategy.evaluate(state.position))
    }

    @Test
    fun `every stone counts, also in hand`() {
        val state = createState(listOf(), 9, listOf(), 8, White)

        assertThat(strategy.evaluate(state.position)).isEqualTo(100.0)
    }

    @Test
    fun `a mule counts`() {
        val withMule = createState(listOf(0, 1, 2), 6, listOf(), 9, White)
        // Same field weights and no two stones in a common mule
        val withoutMule = createState(listOf(1, 3, 23), 6, listOf(), 9, White)

        val muleValue = strategy.evaluate(withMule.position) - strategy.evaluate(withoutMule.position)

        assertThat(muleValue).isEqualTo(10.0)
    }

    @Test
    fun `in the moving phase a mule can only be closed by a neighbouring stone`() {
        val closable = createState(listOf(0, 1, 14, 20), 0, listOf(4, 10, 16, 22), 0, White)
        val notClosable = createState(listOf(0, 1, 13, 20), 0, listOf(4, 10, 16, 22), 0, White)

        assertThat(closableMuleValue(closable)).isEqualTo(20.0)
        assertThat(closableMuleValue(notClosable)).isEqualTo(0.0)
    }

    @Test
    fun `mobility only counts in the moving phase`() {
        val setting = createState(listOf(0), 8, listOf(), 9, White)
        val moving = createState(listOf(0, 4, 13, 20), 0, listOf(), 0, White)

        val mobilityOnly = ExtendedEvaluationStrategy(0.0, 0.0, 0.0, 1.0)

        assertThat(mobilityOnly.evaluate(setting.position) - weights(setting)).isEqualTo(0.0)
        // 0 -> 1, 9; 4 -> 1, 3, 5, 7; 13 -> 5, 12, 14; 20 -> 19
        assertThat(mobilityOnly.evaluate(moving.position) - weights(moving)).isEqualTo(10.0)
    }

    private fun closableMuleValue(state: GameState) =
        ExtendedEvaluationStrategy(0.0, 0.0, 20.0, 0.0).evaluate(state.position) - weights(state)

    private fun weights(state: GameState) =
        (state.board.weightedStonesOnBoard(White) - state.board.weightedStonesOnBoard(Black)).toDouble()
}
