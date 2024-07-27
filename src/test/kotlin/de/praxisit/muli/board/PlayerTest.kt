package de.praxisit.muli.board

import de.praxisit.muli.board.Color.BLACK
import de.praxisit.muli.board.Color.WHITE
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
            val player = Player(WHITE)

            assertThat(player.color).isEqualTo(WHITE)
            assertThat(player.stones).isEqualTo(9)
        }

        @Test
        fun `create black player`() {
            val player = Player(BLACK)

            assertThat(player.color).isEqualTo(BLACK)
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
        var player = Player(WHITE)
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
        var player = Player(WHITE)
        for (i in 0 until numberOfStones) {
            player = player.setStone()
        }

        assertThat(player.color).isEqualTo(WHITE)
        assertThat(player.remainingStones).isEqualTo(numberOfLeftStones)
        assertThat(player.phase).isEqualTo(expectedPhase)
    }

    @Test
    fun `lose stones`() {
        var player = Player(WHITE)
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
                val player = Player(WHITE)
                val board = Board()

                val moves = player.legalMoves(board)

                assertThat(moves).hasSize(24)
                    .hasOnlyElementsOfType(SetMove::class.java)
                    .extracting(Move::color).containsOnly(WHITE)
            }

            @Test
            fun `setting on a board with some stones of one color`() {
                val player = Player(BLACK)
                val board = Board().setStone(3, BLACK).setStone(4, BLACK).setStone(5, BLACK)

                val moves = player.legalMoves(board)

                assertThat(moves).hasSize(21)
                    .hasOnlyElementsOfType(SetMove::class.java)
                    .extracting(Move::color).containsOnly(BLACK)
            }

            @Test
            fun `setting on a board with some stones of different colors`() {
                val player = Player(BLACK)
                val board = Board().setStone(3, BLACK).setStone(4, WHITE).setStone(5, BLACK)

                val moves = player.legalMoves(board)

                assertThat(moves).hasSize(21)
                    .hasOnlyElementsOfType(SetMove::class.java)
                    .extracting(Move::color).containsOnly(BLACK)
            }
        }

        @Nested
        inner class JumpingMoves {
            @Test
            fun `a player with 3 stones on an otherwise empty board`() {
                val white = Player(WHITE).loseStone().loseStone().loseStone().loseStone().loseStone().loseStone()
                val board = Board().setStone(6, WHITE).setStone(7, WHITE).setStone(8, WHITE)

                val moves = white.legalMoves(board)

                assertThat(moves)
                    .hasSize(63)
                    .hasOnlyElementsOfType(JumpMove::class.java)
                    .extracting(Move::color)
                    .containsOnly(WHITE)
            }

            @Test
            fun `a player with 3 stones on a board with other stones`() {
                val white = Player(WHITE).loseStone().loseStone().loseStone().loseStone().loseStone().loseStone()
                val board = Board().setStone(6, WHITE).setStone(7, WHITE).setStone(8, WHITE)
                    .setStone(21, BLACK).setStone(22, BLACK).setStone(23, BLACK)

                val moves = white.legalMoves(board)

                assertThat(moves)
                    .hasSize(54)
                    .hasOnlyElementsOfType(JumpMove::class.java)
                    .extracting(Move::color)
                    .containsOnly(WHITE)
            }
        }
    }

}