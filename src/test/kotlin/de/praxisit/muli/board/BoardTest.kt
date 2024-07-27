package de.praxisit.muli.board

import de.praxisit.muli.board.Board.Companion.COMPLETABLE_MULES
import de.praxisit.muli.board.Board.Companion.MULES
import de.praxisit.muli.board.Color.BLACK
import de.praxisit.muli.board.Color.WHITE
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

            val endBoard = board.moveStone(10, 11)

            assertThat(endBoard.getStone(10)).isEqualTo(Color.NONE)
            assertThat(endBoard.getStone(11)).isEqualTo(WHITE)
        }

        @Test
        fun `from used field to another used field`() {
            val board = emptyBoard
                .setStone(10, WHITE)
                .setStone(9, BLACK)

            assertThatThrownBy { board.moveStone(10, 9) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from unused field to another unused field`() {
            assertThatThrownBy { emptyBoard.moveStone(3, 10) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from used field to the same field`() {
            val board = emptyBoard.setStone(15, BLACK)

            assertThatThrownBy { board.moveStone(15, 15) }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
    
    @Nested
    inner class CompletableMules {
        @Test
        fun `every field is in 2 mules`() {
            assertThat(MULES).hasSize(MULES.size)
            (0..<24).forEach {field ->
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
            val board = emptyBoard.setStone(1, WHITE).setStone(2, WHITE)
                .setStone(4, BLACK).setStone(5, BLACK)

            assertThat(board.willCloseMule(field, WHITE))
        }
    }

    @Nested
    inner class CapturablePieces {
        @Test
        fun `no capturable pieces in empty board`() {
            assertThat(emptyBoard.capturablePieces(WHITE)).isEmpty()
            assertThat(emptyBoard.capturablePieces(BLACK)).isEmpty()
        }

        @Test
        fun `find capturable pieces in complex board`() {
            val board = emptyBoard
            .setStone(1, WHITE)
                .setStone(2, WHITE)
                .setStone(3, BLACK)
                .setStone(4, BLACK)
                .setStone(5, BLACK)
                .setStone(6, BLACK)

            assertThat(board.capturablePieces(WHITE)).containsExactlyInAnyOrder(1, 2)
            assertThat(board.capturablePieces(BLACK)).containsExactlyInAnyOrder(6)
        }
    }

    @Nested
    inner class printBoard {
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
                .setStone(3, WHITE)
                .setStone(4, WHITE)
                .setStone(5, BLACK)
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
        assertThat(emptyBoard.activePlayerColor).isEqualTo(WHITE)
        val board1 = emptyBoard.changePlayer()
        assertThat(board1.activePlayerColor).isEqualTo(BLACK)
        val board2 = board1.changePlayer()
        assertThat(board2.activePlayerColor).isEqualTo(WHITE)
    }
}
