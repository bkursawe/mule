package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RulesTest {
    @Nested
    inner class SettingMoves {
        @Test
        fun `setting on an empty board`() {
            val moves = Rules.legalMoves(Position())

            assertThat(moves).hasSize(24)
                .hasOnlyElementsOfType(SetMove::class.java)
                .extracting(Move::color).containsOnly(White)
        }

        @Test
        fun `setting on a board with some stones of one color`() {
            val state = createState(listOf(), 9, listOf(3, 4, 5), 6, Black)

            val moves = Rules.legalMoves(state.position)

            assertThat(moves).hasSize(21)
                .hasOnlyElementsOfType(SetMove::class.java)
                .extracting(Move::color).containsOnly(Black)
        }

        @Test
        fun `setting on a board with some stones of different colors`() {
            val state = createState(listOf(4), 8, listOf(3, 5), 7, Black)

            val moves = Rules.legalMoves(state.position)

            assertThat(moves).hasSize(21)
                .hasOnlyElementsOfType(SetMove::class.java)
                .extracting(Move::color).containsOnly(Black)
        }
    }

    @Test
    fun `pushing moves go to connected empty fields`() {
        val state = createState(listOf(0, 4, 9, 13), 0, listOf(10, 12, 20, 23), 0, White)

        val moves = Rules.legalMoves(state.position)

        assertThat(moves)
            .hasSize(8)
            .hasOnlyElementsOfType(PushMove::class.java)
            .extracting(Move::color)
            .containsOnly(White)
    }

    @Nested
    inner class JumpingMoves {
        @Test
        fun `a player with 3 stones on an otherwise empty board`() {
            val state = createState(listOf(6, 7, 8), 0, listOf(), 0, White)

            val moves = Rules.legalMoves(state.position)

            assertThat(moves)
                .hasSize(63)
                .hasOnlyElementsOfType(JumpMove::class.java)
                .extracting(Move::color)
                .containsOnly(White)
        }

        @Test
        fun `a player with 3 stones on a board with other stones`() {
            val state = createState(listOf(6, 7, 8), 0, listOf(21, 22, 23), 0, White)

            val moves = Rules.legalMoves(state.position)

            assertThat(moves)
                .hasSize(54)
                .hasOnlyElementsOfType(JumpMove::class.java)
                .extracting(Move::color)
                .containsOnly(White)
        }
    }

    @Test
    fun `a player who has lost has no moves`() {
        val state = createState(listOf(0, 1), 0, listOf(3, 4, 5), 0, White)

        assertThat(Rules.legalMoves(state.position)).isEmpty()
    }

    @Nested
    inner class CaptureMoves {
        @Test
        fun `capture from a mule if all opponent stones are in mules`() {
            val state = createState(listOf(0, 1), 7, listOf(3, 4, 5), 6, White)

            val moves = Rules.legalMoves(state.position).filter { it.toField.index == 2 }

            assertThat(moves.map { it.capturedField?.index }).containsExactlyInAnyOrder(3, 4, 5)
        }

        @Test
        fun `close a mule without capture if the opponent has no stones on the board`() {
            val state = createState(listOf(0, 1), 7, listOf(), 9, White)

            val moves = Rules.legalMoves(state.position).filter { it.toField.index == 2 }

            assertThat(moves).containsExactly(SetMove(White, 2.asFieldIndex))
        }
    }
}
