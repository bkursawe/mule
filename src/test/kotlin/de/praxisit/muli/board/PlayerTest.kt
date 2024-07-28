package de.praxisit.muli.board

import de.praxisit.muli.board.Phase.JUMPING
import de.praxisit.muli.board.Phase.MOVING
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PlayerTest {
    @Nested
    inner class CreatePlayer {
        @Test
        fun `create white player`() {
            val player = Player(White)

            assertThat(player.color).isEqualTo(White)
            assertThat(player.stones).isEqualTo(9)
        }

        @Test
        fun `create black player`() {
            val player = Player(Black)

            assertThat(player.color).isEqualTo(Black)
            assertThat(player.stones).isEqualTo(9)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "1,8,true",
            "8,1,true",
            "10,,false"
        ]
    )
    fun `set some stones`(stones: Int, remainingStones: Int?, expected: Boolean) {
        var player = Player(White)
        if (expected) {
            for (i in 0 until stones) {
                player = player.setStone()
            }
            assertThat(player.stonesSet).isEqualTo(stones)
            assertThat(player.remainingStones).isEqualTo(9 - stones)
        } else {
            assertThatThrownBy {
                for (i in 0 until stones) {
                    player = player.setStone()
                }
            }.isInstanceOf(IllegalStateException::class.java)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "1,8,SETTING",
            "3,6,SETTING",
            "8,1,SETTING",
            "9,0,MOVING"
        ]
    )
    fun `set stones changes phase`(numberOfStones: Int, numberOfLeftStones: Int, expectedPhase: Phase) {
        var player = Player(White)
        for (i in 0 until numberOfStones) {
            player = player.setStone()
        }

        assertThat(player.color).isEqualTo(White)
        assertThat(player.remainingStones).isEqualTo(numberOfLeftStones)
        assertThat(player.phase).isEqualTo(expectedPhase)
    }

    @Test
    fun `lose stones`() {
        var player = Player(White)
        repeat(9) {
            player = player.setStone()
        }
        repeat(5) {
            player = player.loseStone()
            assertThat(player.phase).isEqualTo(MOVING)
        }
        player = player.loseStone()
        assertThat(player.phase).isEqualTo(JUMPING)
    }

    @Nested
    inner class LegalMoves {
        @Nested
        inner class SettingMoves {
            @Test
            fun `setting on an empty board`() {
                val player = Player(White)
                val board = Board()

                val moves = player.legalMoves(board)

                assertThat(moves).hasSize(24)
                    .hasOnlyElementsOfType(SetMove::class.java)
                    .extracting(Move::color).containsOnly(White)
            }

            @Test
            fun `setting on a board with some stones of one color`() {
                val player = Player(Black)
                val board = Board().setStone(3, Black).setStone(4, Black).setStone(5, Black)

                val moves = player.legalMoves(board)

                assertThat(moves).hasSize(21)
                    .hasOnlyElementsOfType(SetMove::class.java)
                    .extracting(Move::color).containsOnly(Black)
            }

            @Test
            fun `setting on a board with some stones of different colors`() {
                val player = Player(Black)
                val board = Board().setStone(3, Black).setStone(4, White).setStone(5, Black)

                val moves = player.legalMoves(board)

                assertThat(moves).hasSize(21)
                    .hasOnlyElementsOfType(SetMove::class.java)
                    .extracting(Move::color).containsOnly(Black)
            }
        }

        @Nested
        inner class JumpingMoves {
            @Test
            fun `a player with 3 stones on an otherwise empty board`() {
                val white = Player(White).loseStone().loseStone().loseStone().loseStone().loseStone().loseStone()
                val board = Board().setStone(6, White).setStone(7, White).setStone(8, White)

                val moves = white.legalMoves(board)

                assertThat(moves)
                    .hasSize(63)
                    .hasOnlyElementsOfType(JumpMove::class.java)
                    .extracting(Move::color)
                    .containsOnly(White)
            }

            @Test
            fun `a player with 3 stones on a board with other stones`() {
                val white = Player(White).loseStone().loseStone().loseStone().loseStone().loseStone().loseStone()
                val board = Board().setStone(6, White).setStone(7, White).setStone(8, White)
                    .setStone(21, Black).setStone(22, Black).setStone(23, Black)

                val moves = white.legalMoves(board)

                assertThat(moves)
                    .hasSize(54)
                    .hasOnlyElementsOfType(JumpMove::class.java)
                    .extracting(Move::color)
                    .containsOnly(White)
            }
        }
    }

    @Nested
    inner class ChooseMove {
        inner class Strategy(val toField: Int, val fromField: Int? = null) : EvaluationStrategy {
            override fun evaluate(board: Board, move: Move): Double {
                return when (move) {
                    is SetMove           -> if (move.toField == toField) 10.0 else 0.0
                    is MoveWithFromField -> if (move.toField == toField && move.fromField == fromField) 10.0 else 0.0
                    else                 -> 0.0
                }
            }
        }

        @Test
        fun `choose the set move of an empty board`() {
            val player = Player(White, Strategy(5))
            val board = Board()

            val move = player.chooseMove(board)

            assertThat(move).isInstanceOf(SetMove::class.java)
            assertThat(move.toField).isEqualTo(5)
        }

        @Test
        fun `choose the push moves of a board`() {
            val white = Player(White, Strategy(10, 3))
            val board = Board().copy(white = white)
                .draw(SetMove(White, 0))
                .draw(SetMove(White, 1))
                .draw(SetMove(White, 2))
                .draw(SetMove(White, 3))
                .draw(SetMove(White, 4))
                .draw(SetMove(White, 5))
                .draw(SetMove(White, 6))
                .draw(SetMove(White, 7))
                .draw(SetMove(White, 8))

            val move = board.activePlayer.chooseMove(board)

            assertThat(move).isInstanceOf(PushMove::class.java)
            assertThat(move.toField).isEqualTo(10)
            assertThat((move as? PushMove)?.fromField).isEqualTo(3)
        }
    }
}