package de.praxisit.mule.console

import de.praxisit.mule.*
import de.praxisit.mule.GameResult.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConsoleUiTest {
    @Nested
    inner class PrintBoard {
        @Test
        fun `draw empty board`() {
            val output = ConsoleUi.format(GameState(), 0.0)
            val expected = """
                O--------O--------O
                |        |        |
                |  O-----O-----O  |
                |  |     |     |  |
                |  |  O--O--O  |  |
                |  |  |     |  |  |
                O--O--O     O--O--O
                |  |  |     |  |  |
                |  |  O--O--O  |  |
                |  |     |     |  |
                |  O-----O-----O  |
                |        |        |
                O--------O--------O
                * White: stones = 9 phase = SETTING
                  Black: stones = 9 phase = SETTING
                Evaluation: 0.0

            """.trimIndent()

            assertThat(output)
                .hasSameSizeAs(expected)
                .isEqualToIgnoringWhitespace(expected)
        }

        @Test
        fun `draw board with some stones`() {
            val board = Board()
                .setStone(White, FieldIndex(3))
                .setStone(White, FieldIndex(4))
                .setStone(Black, FieldIndex(5))
            val output = ConsoleUi.format(GameState(Position(board)), 4.0)
            val expected = """
                O--------O--------O
                |        |        |
                |  W-----W-----B  |
                |  |     |     |  |
                |  |  O--O--O  |  |
                |  |  |     |  |  |
                O--O--O     O--O--O
                |  |  |     |  |  |
                |  |  O--O--O  |  |
                |  |     |     |  |
                |  O-----O-----O  |
                |        |        |
                O--------O--------O
                * White: stones = 9 phase = SETTING
                  Black: stones = 9 phase = SETTING
                Evaluation: 4.0

            """.trimIndent()

            assertThat(output)
                .hasSameSizeAs(expected)
                .isEqualToIgnoringWhitespace(expected)
        }
    }

    @Test
    fun `format the results`() {
        assertThat(ConsoleUi.format(Ongoing)).isEqualTo("No winner yet")
        assertThat(ConsoleUi.format(Remis)).isEqualTo("It's a remis")
        assertThat(ConsoleUi.format(Win(White))).isEqualTo("White is the winner")
        assertThat(ConsoleUi.format(Win(Black))).isEqualTo("Black is the winner")
    }
}
