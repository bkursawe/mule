package de.praxisit.mule

import de.praxisit.mule.Board.Companion.COMPLETABLE_MULES
import de.praxisit.mule.Board.Companion.MULES
import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
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
                val move = SetMove(White, 0.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).containsExactlyInAnyOrder(0.asFieldIndex)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a SetMove with a black capture`() {
                val board = Board().setStone(0, White).setStone(1, White).setStone(4, Black)
                val move = SetMove(White, 2.asFieldIndex, 4.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1, 2)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a SetMove with a white capture`() {
                val board = Board().setStone(0, White).setStone(1, Black).setStone(4, Black).switchPlayer()
                val move = SetMove(Black, 7.asFieldIndex, 0.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White)).isEmpty()
                assertThat(boardAfter.fieldsIndicesWithColor(Black).map { it.index }).containsExactlyInAnyOrder(1, 4, 7)
            }
        }

        @Nested
        inner class PushMove {
            @Test
            fun `draw a simple PushMove`() {
                val board = Board().setStone(9, White).setStone(1, White)
                val move = PushMove(White, 9.asFieldIndex, 0.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a PushMove with a capture`() {
                val board = Board().setStone(9, White).setStone(1, White).setStone(2, White).setStone(4, Black)
                val move = PushMove(White, 9.asFieldIndex, 0.asFieldIndex, 4.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1, 2)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }
        }

        @Nested
        inner class JumpMove {
            @Test
            fun `draw a simple PushMove`() {
                val board = Board().setStone(22, White).setStone(1, White).setStone(3, White)
                val move = JumpMove(White, 22.asFieldIndex, 0.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1, 3)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a PushMove with a capture`() {
                val board = Board().setStone(22, White).setStone(1, White).setStone(2, White).setStone(4, Black)
                val move = JumpMove(White, 22.asFieldIndex, 0.asFieldIndex, 4.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1, 2)
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
            assertThat(board.fieldsIndicesWithColor(White).map { it.index }).containsExactly(5)
            assertThat(board.fieldsIndicesWithColor(Black).map { it.index }).containsExactly(8)
        }

        @Test
        fun `set a stone to an invalid field`() {
            assertThatThrownBy { emptyBoard.setStone(-1, White) }
                .isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { emptyBoard.setStone(24, White) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `get a stone from an invalid field`() {
            assertThatThrownBy { emptyBoard.getStone(-1) }.isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { emptyBoard.getStone(24) }.isInstanceOf(IllegalArgumentException::class.java)
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
            FieldIndex.INDEXES.forEach { field ->
                assertThat(COMPLETABLE_MULES[field.index]).allSatisfy { muleFieldPair ->
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
                "3,false",
                "7,false",
                "22,false"
            ]
        )
        fun `will close mule`(field: FieldIndex, expected: Boolean) {
            val board = emptyBoard.setStone(1, White).setStone(2, White)
                .setStone(4, Black).setStone(5, Black)

            assertThat(board.willCloseMule(field, White)).isEqualTo(expected)
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

            assertThat(board.capturablePieces(White).map { it.index }).containsExactlyInAnyOrder(1, 2)
            assertThat(board.capturablePieces(Black).map { it.index }).containsExactlyInAnyOrder(6)
        }
    }

    @Nested
    inner class ChooseMove {
        inner class TestEvaluationStrategy : EvaluationStrategy {
            override fun evaluate(board: Board): Double {
                return 0.0
            }
        }

        @Test
        fun `choose the player's move`() {
            val player = Player(White, evaluationStrategy = TestEvaluationStrategy())
            val board = Board(white = player)

            val move = board.chooseMove()

            assertThat(move!!.toField.index).isEqualTo(10)
        }
    }

    @Nested
    inner class NoLooser {
        @Test
        fun `no current looser`() {
            val board = Board()
            assertThat(board.noLooser()).isTrue()
            assertThat(board.showWinner()).isEqualTo("No winner yet")
        }

        @Test
        fun `white has no moves`() {
            val white = Player(White, 4, 9, Phase.MOVING)
            val black = Player(Black, 4, 9, Phase.MOVING)
            val board = Board(white = white, black = black).setStone(0, White).setStone(1, White).setStone(2, White)
                .setStone(9, White)
                .setStone(4, Black).setStone(10, Black).setStone(14, Black).setStone(21, Black)

            assertThat(board.noLooser()).isFalse()
            assertThat(board.showWinner()).isEqualTo("Black is the winner")
        }

        @Test
        fun `black has no moves`() {
            val white = Player(White, 4, 9, Phase.MOVING)
            val black = Player(Black, 4, 9, Phase.MOVING)
            val board =
                Board(white = white, black = black, activePlayerColor = Black).setStone(0, Black).setStone(1, Black)
                    .setStone(2, Black).setStone(9, Black)
                    .setStone(4, White).setStone(10, White).setStone(14, White).setStone(21, White)

            assertThat(board.noLooser()).isFalse()
            assertThat(board.showWinner()).isEqualTo("White is the winner")
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

    private fun Board.setStone(field: Int, color: Color) = setStone(field.asFieldIndex, color)
    private fun Board.getStone(field: Int) = getStone(field.asFieldIndex)
    private fun Board.moveStone(from: Int, to: Int) = moveStone(from.asFieldIndex, to.asFieldIndex)
}
