package de.praxisit.muli.board

import de.praxisit.muli.board.Color.BLACK
import de.praxisit.muli.board.Color.WHITE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BoardTest {

    val emptyBoard: Board = Board()

    @Nested
    inner class CreateBoard {
        @Test
        fun `create empty board`() {
            assertThat(emptyBoard.emptyFieldsIndices()).hasSize(24)
        }
    }

    @Nested
    inner class SetStone {
        @Test
        fun `set and get stones`() {
            val board = emptyBoard
                .setStone(5, WHITE)
                .setStone(8, BLACK)

            assertThat(board.getStone(5)).isEqualTo(WHITE)
            assertThat(board.getStone(8)).isEqualTo(BLACK)
            assertThat(board.fieldsIndicesWithColor(WHITE)).containsExactly(5)
            assertThat(board.fieldsIndicesWithColor(BLACK)).containsExactly(8)
        }

        @Test
        fun `set a stone to an invalid field`() {
            assertThatThrownBy {
                emptyBoard.setStone(
                    24,
                    WHITE
                )
            }.isInstanceOf(ArrayIndexOutOfBoundsException::class.java)
            assertThatThrownBy {
                emptyBoard.setStone(24, WHITE)
            }.isInstanceOf(ArrayIndexOutOfBoundsException::class.java)
        }

        @Test
        fun `get a stone from an invalid field`() {
            assertThatThrownBy { emptyBoard.getStone(24) }.isInstanceOf(ArrayIndexOutOfBoundsException::class.java)
            assertThatThrownBy { emptyBoard.getStone(24) }.isInstanceOf(ArrayIndexOutOfBoundsException::class.java)
        }
    }

    @Nested
    inner class MoveStone {
        @Test
        fun `from used field to empty field`() {
            val board = emptyBoard.setStone(10, WHITE)

            val endBoard = board.pushStone(10, 11)

            assertThat(endBoard.getStone(10)).isEqualTo(Color.NONE)
            assertThat(endBoard.getStone(11)).isEqualTo(WHITE)
        }

        @Test
        fun `from used field to another used field`() {
            val board = emptyBoard
                .setStone(10, WHITE)
                .setStone(9, BLACK)

            assertThatThrownBy { board.pushStone(10, 9) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from unused field to another unused field`() {
            assertThatThrownBy { emptyBoard.pushStone(3, 10) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from used field to the same field`() {
            val board = emptyBoard.setStone(15, BLACK)

            assertThatThrownBy { board.pushStone(15, 15) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from used field to unreachable unused field`() {
            val board = emptyBoard.setStone(15, BLACK)

            assertThatThrownBy { board.pushStone(15, 17) }.isInstanceOf(IllegalMoveException::class.java)
        }
    }

    @Nested
    inner class JumpStone {
        @Test
        fun `from used field to empty field`() {
            val board = emptyBoard.setStone(10, WHITE)

            val endBoard = board.jumpStone(10, 20)

            assertThat(endBoard.getStone(10)).isEqualTo(Color.NONE)
            assertThat(endBoard.getStone(20)).isEqualTo(WHITE)
        }

        @Test
        fun `from used field to another used field`() {
            val board = emptyBoard
                .setStone(10, WHITE)
                .setStone(9, BLACK)

            assertThatThrownBy { board.jumpStone(10, 9) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from unused field to another unused field`() {
            assertThatThrownBy { emptyBoard.jumpStone(3, 10) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from used field to the same field`() {
            val board = emptyBoard.setStone(15, BLACK)

            assertThatThrownBy { board.jumpStone(15, 15) }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class DrawBoard {
        @Test
        fun `draw empty board`() {
            val output = emptyBoard.drawBoard()
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
            """.trimIndent()

            assertThat(output)
                .hasSameSizeAs(expected)
                .isEqualToIgnoringWhitespace(expected)
        }

        @Test
        fun `draw board with some stones`() {
            val board = emptyBoard
                .setStone(3, WHITE)
                .setStone(4, WHITE)
                .setStone(5, BLACK)
            val output = board.drawBoard()
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
            """.trimIndent()

            assertThat(output)
                .hasSameSizeAs(expected)
                .isEqualToIgnoringWhitespace(expected)
        }
    }
}
