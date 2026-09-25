package de.praxisit.mule

import de.praxisit.mule.Phase.JUMPING
import de.praxisit.mule.Phase.LOST
import de.praxisit.mule.Phase.MOVING
import de.praxisit.mule.Phase.SETTING
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
            assertThat(player.remainingStones).isEqualTo(remainingStones)
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

    @Test
    fun `lose stones while setting`() {
        var player = Player(White)
        repeat(6) {
            player = player.setStone()
        }
        repeat(6) {
            player = player.loseStone()
            assertThat(player.phase).isEqualTo(SETTING)
        }
        repeat(3) {
            player = player.setStone()
        }
        assertThat(player.phase).isEqualTo(JUMPING)
        assertThat(player.loseStone().phase).isEqualTo(LOST)
    }
}
