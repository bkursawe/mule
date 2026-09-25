package de.praxisit.mule

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Counts all move sequences up to a fixed depth. The counts guard the move generation against regressions.
 */
class PerftTest {
    @ParameterizedTest
    @CsvSource(
        value = [
            "'', 9, '', 9, White, 4, 255024",
            "0 1, 7, 4, 8, Black, 3, 8472",
            "0 4 9 13, 0, 10 12 20 23, 0, White, 5, 35638",
            "0 3 7 8 9 10 16 19 22, 0, 1 2 5 23, 0, Black, 4, 29584",
            "0 1 13 20 22, 0, 3 5 17, 0, Black, 3, 22612"
        ]
    )
    fun `count the move sequences`(
        whiteStones: String,
        whiteStonesToSet: Int,
        blackStones: String,
        blackStonesToSet: Int,
        colorName: String,
        depth: Int,
        expectedCount: Long
    ) {
        val color = if (colorName == "White") White else Black
        val state =
            createState(whiteStones.toFields(), whiteStonesToSet, blackStones.toFields(), blackStonesToSet, color)

        assertThat(perft(state.position, depth)).isEqualTo(expectedCount)
    }

    private fun perft(position: Position, depth: Int): Long =
        if (depth == 0) 1 else Rules.legalMoves(position).sumOf { perft(Rules.apply(position, it), depth - 1) }
}
