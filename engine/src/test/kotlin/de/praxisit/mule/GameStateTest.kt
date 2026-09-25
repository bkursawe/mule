package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import de.praxisit.mule.GameResult.*
import de.praxisit.mule.Phase.JUMPING
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GameStateTest {

    @Nested
    inner class Play {
        @Nested
        inner class SetMove {
            @Test
            fun `play a simple SetMove`() {
                val state = GameState().play(createSetMove(White, 0))

                assertThat(state.board.fieldsIndicesWithColor(White).map { it.index }).containsExactly(0)
                assertThat(state.board.fieldsIndicesWithColor(Black)).isEmpty()
                assertThat(state.position.white.stonesSet).isEqualTo(1)
            }

            @Test
            fun `play a SetMove with a black capture`() {
                val state = createState(listOf(0, 1), 7, listOf(4), 8, White)

                val stateAfter = state.play(createSetMove(White, 2, 4))

                assertThat(stateAfter.board.fieldsIndicesWithColor(White).map { it.index })
                    .containsExactlyInAnyOrder(0, 1, 2)
                assertThat(stateAfter.board.fieldsIndicesWithColor(Black)).isEmpty()
                assertThat(stateAfter.position.black.stones).isEqualTo(8)
            }

            @Test
            fun `play a SetMove with a white capture`() {
                val state = createState(listOf(0), 8, listOf(1, 4), 7, Black)

                val stateAfter = state.play(createSetMove(Black, 7, 0))

                assertThat(stateAfter.board.fieldsIndicesWithColor(White)).isEmpty()
                assertThat(stateAfter.board.fieldsIndicesWithColor(Black).map { it.index })
                    .containsExactlyInAnyOrder(1, 4, 7)
                assertThat(stateAfter.position.white.stones).isEqualTo(8)
            }
        }

        @Nested
        inner class PushMove {
            @Test
            fun `play a simple PushMove`() {
                val state = createState(listOf(9, 1, 13, 20), 0, listOf(5, 8, 17, 23), 0, White)

                val stateAfter = state.play(PushMove(White, 9.asFieldIndex, 0.asFieldIndex))

                assertThat(stateAfter.board.fieldsIndicesWithColor(White).map { it.index })
                    .containsExactlyInAnyOrder(0, 1, 13, 20)
            }

            @Test
            fun `play a PushMove with a capture`() {
                val state = createState(listOf(9, 1, 2, 13), 0, listOf(4, 8, 17, 23), 0, White)

                val stateAfter = state.play(PushMove(White, 9.asFieldIndex, 0.asFieldIndex, 4.asFieldIndex))

                assertThat(stateAfter.board.fieldsIndicesWithColor(White).map { it.index })
                    .containsExactlyInAnyOrder(0, 1, 2, 13)
                assertThat(stateAfter.board.fieldsIndicesWithColor(Black).map { it.index })
                    .containsExactlyInAnyOrder(8, 17, 23)
                assertThat(stateAfter.position.black.phase).isEqualTo(JUMPING)
            }
        }

        @Nested
        inner class JumpMove {
            @Test
            fun `play a simple JumpMove`() {
                val state = createState(listOf(22, 1, 3), 0, listOf(5, 8, 17, 20), 0, White)

                val stateAfter = state.play(JumpMove(White, 22.asFieldIndex, 0.asFieldIndex))

                assertThat(stateAfter.board.fieldsIndicesWithColor(White).map { it.index })
                    .containsExactlyInAnyOrder(0, 1, 3)
            }

            @Test
            fun `play a JumpMove with a capture`() {
                val state = createState(listOf(22, 1, 2), 0, listOf(4, 8, 17, 20), 0, White)

                val stateAfter = state.play(JumpMove(White, 22.asFieldIndex, 0.asFieldIndex, 4.asFieldIndex))

                assertThat(stateAfter.board.fieldsIndicesWithColor(White).map { it.index })
                    .containsExactlyInAnyOrder(0, 1, 2)
                assertThat(stateAfter.board.fieldsIndicesWithColor(Black).map { it.index })
                    .containsExactlyInAnyOrder(8, 17, 20)
            }
        }

        @Test
        fun `play switches the active player`() {
            val state = GameState().play(createSetMove(White, 0))
            assertThat(state.activeColor).isEqualTo(Black)

            val nextState = state.play(createSetMove(Black, 1))
            assertThat(nextState.activeColor).isEqualTo(White)
        }

        @Test
        fun `a move of the wrong color is rejected`() {
            assertThatThrownBy { GameState().play(createSetMove(Black, 0)) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `a move of the wrong phase is rejected`() {
            val state = createState(listOf(0), 8, listOf(4), 8, White)

            assertThatThrownBy { state.play(PushMove(White, 0.asFieldIndex, 1.asFieldIndex)) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `closing a mule without capturing is rejected`() {
            val state = createState(listOf(0, 1), 7, listOf(4), 8, White)

            assertThatThrownBy { state.play(createSetMove(White, 2)) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class Repetition {
        @Test
        fun `the third occurrence of a position is a remis`() {
            val start = createBackAndForthState()

            val second = start.playBackAndForth()
            val third = second.playBackAndForth()

            assertThat(second.position).isEqualTo(start.position)
            assertThat(second.isRepeated).isFalse()
            assertThat(third.isRepeated).isTrue()
            assertThat(third.isRemis).isTrue()
            assertThat(third.result).isEqualTo(Remis)
        }
    }

    @Nested
    inner class MovesWithoutCapture {
        @Test
        fun `a move without capture increments the counter`() {
            val state = createBackAndForthState()
                .play(PushMove(White, 0.asFieldIndex, 1.asFieldIndex))
                .play(PushMove(Black, 23.asFieldIndex, 22.asFieldIndex))

            assertThat(state.movesWithoutCapture).isEqualTo(2)
        }

        @Test
        fun `setting moves do not count`() {
            val state = GameState()
                .play(createSetMove(White, 0))
                .play(createSetMove(Black, 1))

            assertThat(state.movesWithoutCapture).isEqualTo(0)
        }

        @Test
        fun `a setting move resets the counter`() {
            val state = createState(listOf(0, 1), 0, listOf(4, 5), 1, Black, movesWithoutCapture = 10)

            val stateAfter = state.play(createSetMove(Black, 9))

            assertThat(stateAfter.movesWithoutCapture).isEqualTo(0)
        }

        @Test
        fun `a capture resets the counter`() {
            val state = createState(listOf(0, 1), 7, listOf(4), 8, White, movesWithoutCapture = 10)

            val stateAfter = state.play(createSetMove(White, 2, 4))

            assertThat(stateAfter.movesWithoutCapture).isEqualTo(0)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "38,false",
                "39,true"
            ]
        )
        fun `the 40th move without capture is a remis`(movesBefore: Int, expectedRemis: Boolean) {
            val state = createState(
                listOf(0, 4, 9, 13),
                0,
                listOf(10, 12, 20, 23),
                0,
                White,
                movesWithoutCapture = movesBefore
            )

            val stateAfter = state.play(PushMove(White, 0.asFieldIndex, 1.asFieldIndex))

            assertThat(stateAfter.isRemis).isEqualTo(expectedRemis)
            assertThat(stateAfter.result).isEqualTo(if (expectedRemis) Remis else Ongoing)
        }

        @Test
        fun `a game between computer players ends`() {
            val strategy = SimpleChoosingStrategy()

            val states = generateSequence(GameState()) { state ->
                if (state.result == Ongoing) state.play(strategy.chooseMove(state)) else null
            }.take(1000).count()

            assertThat(states).isLessThan(1000)
        }
    }

    @Nested
    inner class Result {
        @Test
        fun `a new game is ongoing`() {
            assertThat(GameState().result).isEqualTo(Ongoing)
        }

        @Test
        fun `white has no moves`() {
            val state = createState(listOf(0, 1, 2, 9), 0, listOf(4, 10, 14, 21), 0, White)

            assertThat(state.result).isEqualTo(Win(Black))
        }

        @Test
        fun `black has no moves`() {
            val state = createState(listOf(4, 10, 14, 21), 0, listOf(0, 1, 2, 9), 0, Black)

            assertThat(state.result).isEqualTo(Win(White))
        }

        @Test
        fun `a player with two stones has lost`() {
            val state = createState(listOf(0, 1), 0, listOf(4, 5, 6), 0, White)

            assertThat(state.result).isEqualTo(Win(Black))
        }
    }

    private fun createSetMove(color: Color, field: Int, capturedField: Int? = null) =
        SetMove(color, field.asFieldIndex, capturedField?.asFieldIndex)
}
