package de.praxisit.muli.board

import de.praxisit.muli.board.Board.Companion.COMPLETABLE_MULES
import de.praxisit.muli.board.Board.Companion.MULES
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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
    inner class Draw {
        @Nested
        inner class SetMove {
            @Test
            fun `draw a simple SetMove`() {
                val board = Board()
                val move = SetMove(White, 0)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).containsExactlyInAnyOrder(0)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a SetMove with a capture`() {
                val board = Board().setStone(0, White).setStone(1, White).setStone(4, Black)
                val move = SetMove(White, 2, 4)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).containsExactlyInAnyOrder(0, 1, 2)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }
        }

        @Nested
        inner class PushMove {
            @Test
            fun `draw a simple PushMove`() {
                val board = Board().setStone(9, White).setStone(1, White)
                val move = PushMove(White, 9, 0)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).containsExactlyInAnyOrder(0, 1)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a PushMove with a capture`() {
                val board = Board().setStone(9, White).setStone(1, White).setStone(2, White).setStone(4, Black)
                val move = PushMove(White, 9, 0, 4)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).containsExactlyInAnyOrder(0, 1, 2)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }
        }

        @Nested
        inner class JumpMove {
            @Test
            fun `draw a simple PushMove`() {
                val board = Board().setStone(22, White).setStone(1, White).setStone(3, White)
                val move = JumpMove(White, 22, 0)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).containsExactlyInAnyOrder(0, 1, 3)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a PushMove with a capture`() {
                val board = Board().setStone(22, White).setStone(1, White).setStone(2, White).setStone(4, Black)
                val move = JumpMove(White, 22, 0, 4)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).containsExactlyInAnyOrder(0, 1, 2)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }
        }
    }

    @Nested
    inner class SetStone {
        @Test
        fun `set and get stones`() {
            val board = emptyBoard
                .setStone(5, White)
                .setStone(8, Black)

            assertThat(board.getStone(5)).isEqualTo(White)
            assertThat(board.getStone(8)).isEqualTo(Black)
            assertThat(board.fieldsIndicesWithColor(White)).containsExactly(5)
            assertThat(board.fieldsIndicesWithColor(Black)).containsExactly(8)
        }

        @Test
        fun `set a stone to an invalid field`() {
            assertThatThrownBy {
                emptyBoard.setStone(
                    24,
                    White
                )
            }.isInstanceOf(ArrayIndexOutOfBoundsException::class.java)
            assertThatThrownBy {
                emptyBoard.setStone(24, White)
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
            val board = emptyBoard.setStone(10, White)

            val endBoard = board.moveStone(10, 11)

            assertThat(endBoard.getStone(10)).isEqualTo(Empty)
            assertThat(endBoard.getStone(11)).isEqualTo(White)
        }

        @Test
        fun `from used field to another used field`() {
            val board = emptyBoard
                .setStone(10, White)
                .setStone(9, Black)

            assertThatThrownBy { board.moveStone(10, 9) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from unused field to another unused field`() {
            assertThatThrownBy { emptyBoard.moveStone(3, 10) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from used field to the same field`() {
            val board = emptyBoard.setStone(15, Black)

            assertThatThrownBy { board.moveStone(15, 15) }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class CompletableMules {
        @Test
        fun `every field is in 2 mules`() {
            assertThat(MULES).hasSize(MULES.size)
            (0..<24).forEach { field ->
                assertThat(COMPLETABLE_MULES[field]).allSatisfy { muleFieldPair ->
                    assertThat(listOf(muleFieldPair.first, muleFieldPair.second)).doesNotContain(field)
                }
            }
        }
    }

    @Nested
    inner class WillCloseMule {
        @ParameterizedTest
        @CsvSource(
            value = [
                "0,true",
                "3,true",
                "7,false",
                "22,false"
            ]
        )
        fun `will close mule`(field: Int, expected: Boolean) {
            val board = emptyBoard.setStone(1, White).setStone(2, White)
                .setStone(4, Black).setStone(5, Black)

            assertThat(board.willCloseMule(field, White))
        }
    }

    @Nested
    inner class CapturablePieces {
        @Test
        fun `no capturable pieces in empty board`() {
            assertThat(emptyBoard.capturablePieces(White)).isEmpty()
            assertThat(emptyBoard.capturablePieces(Black)).isEmpty()
        }

        @Test
        fun `find capturable pieces in complex board`() {
            val board = emptyBoard
                .setStone(1, White)
                .setStone(2, White)
                .setStone(3, Black)
                .setStone(4, Black)
                .setStone(5, Black)
                .setStone(6, Black)

            assertThat(board.capturablePieces(White)).containsExactlyInAnyOrder(1, 2)
            assertThat(board.capturablePieces(Black)).containsExactlyInAnyOrder(6)
        }
    }

    @Nested
    inner class PrintBoard {
        @Test
        fun `draw empty board`() {
            val output = emptyBoard.printBoard()
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
                .setStone(3, White)
                .setStone(4, White)
                .setStone(5, Black)
            val output = board.printBoard()
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

    @Test
    fun `board change player`() {
        assertThat(emptyBoard.activePlayerColor).isEqualTo(White)
        val board1 = emptyBoard.switchPlayer()
        assertThat(board1.activePlayerColor).isEqualTo(Black)
        val board2 = board1.switchPlayer()
        assertThat(board2.activePlayerColor).isEqualTo(White)
    }
}
