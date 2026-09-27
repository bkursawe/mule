package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import de.praxisit.mule.Rating.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MoveRaterTest {
    // Without a time limit the search always reaches the same depth, so the ratings do not depend on the machine
    private val rater = MoveRater(depth = 5, timeLimit = null)

    @Test
    fun `every legal move gets a rating`() {
        val state = createState(listOf(0, 4, 9, 13, 19), 0, listOf(1, 10, 12, 20), 0, White)

        assertThat(rater.rate(state).keys).containsExactlyInAnyOrderElementsOf(state.legalMoves)
    }

    @Test
    fun `at the start no move stands out`() {
        assertThat(rater.rate(GameState()).values).containsOnly(NEUTRAL)
    }

    @Test
    fun `blocking the only threat is good, everything else is bad`() {
        // Black threatens to close 0-1-2
        val state = createState(listOf(4), 8, listOf(0, 1), 7, White)

        val ratings = rater.rate(state)

        assertThat(ratings[SetMove(White, 2.asFieldIndex)]).isEqualTo(GOOD)
        assertThat(ratings.filterKeys { it.toField.index != 2 }.values).containsOnly(BAD)
    }

    @Test
    fun `closing a mule before the opponent blocks it is good`() {
        // White closes 3-4-5 by pushing 1 to 4; otherwise Black blocks with 7 to 4
        val state = createState(listOf(1, 3, 5, 21), 0, listOf(7, 12, 16, 23), 0, White)

        val ratings = rater.rate(state)

        assertThat(ratings.filterKeys { it.isCaptureMove }.values).containsOnly(GOOD)
        assertThat(ratings.filterKeys { !it.isCaptureMove }.values).doesNotContain(GOOD)
        // Stepping away from the mule gives it up
        assertThat(ratings[PushMove(White, 3.asFieldIndex, 10.asFieldIndex)]).isEqualTo(BAD)
    }
}
