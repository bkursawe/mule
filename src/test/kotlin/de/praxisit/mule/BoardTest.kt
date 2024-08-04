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
                val board = Board().setStone(White, 0).setStone(White, 1).setStone(Black, 4)
                val move = SetMove(White, 2.asFieldIndex, 4.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1, 2)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a SetMove with a white capture`() {
                val board = Board().setStone(White, 0).setStone(Black, 1).setStone(Black, 4).switchPlayer()
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
                val board = Board().setStone(White, 9).setStone(White, 1)
                val move = PushMove(White, 9.asFieldIndex, 0.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a PushMove with a capture`() {
                val board = Board().setStone(White, 9).setStone(White, 1).setStone(White, 2).setStone(Black, 4)
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
                val board = Board().setStone(White, 22).setStone(White, 1).setStone(White, 3)
                val move = JumpMove(White, 22.asFieldIndex, 0.asFieldIndex)

                val boardAfter = board.draw(move)

                assertThat(boardAfter.fieldsIndicesWithColor(White).map { it.index }).containsExactlyInAnyOrder(0, 1, 3)
                assertThat(boardAfter.fieldsIndicesWithColor(Black)).isEmpty()
            }

            @Test
            fun `draw a PushMove with a capture`() {
                val board = Board().setStone(White, 22).setStone(White, 1).setStone(White, 2).setStone(Black, 4)
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
                .setStone(White, 5)
                .setStone(Black, 8)

            assertThat(board.getStone(5)).isEqualTo(White)
            assertThat(board.getStone(8)).isEqualTo(Black)
            assertThat(board.fieldsIndicesWithColor(White).map { it.index }).containsExactly(5)
            assertThat(board.fieldsIndicesWithColor(Black).map { it.index }).containsExactly(8)
        }

        @Test
        fun `set a stone to an invalid field`() {
            assertThatThrownBy { emptyBoard.setStone(White, -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { emptyBoard.setStone(White, 24) }
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
            val board = emptyBoard.setStone(White, 10)

            val endBoard = board.moveStone(10, 11)

            assertThat(endBoard.getStone(10)).isEqualTo(Empty)
            assertThat(endBoard.getStone(11)).isEqualTo(White)
        }

        @Test
        fun `from used field to another used field`() {
            val board = emptyBoard
                .setStone(White, 10)
                .setStone(Black, 9)

            assertThatThrownBy { board.moveStone(10, 9) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from unused field to another unused field`() {
            assertThatThrownBy { emptyBoard.moveStone(3, 10) }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `from used field to the same field`() {
            val board = emptyBoard.setStone(Black, 15)

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
            val board = emptyBoard.setStone(White, 1).setStone(White, 2)
                .setStone(Black, 4).setStone(Black, 5)

            assertThat(board.willCloseMule(field, White)).isEqualTo(expected)
        }
    }

    @Nested
    inner class MuleCount {
        val board = emptyBoard
            .setStones(White, 3, 4, 5)
            .setStones(White, 0, 9, 21)
            .setStone(White, 1)
            .setStones(Black, 6, 7, 8)
            .setStones(Black, 18, 19, 20)
            .setStones(Black, 16, 19, 22)
            .setStone(Black, 23)

        @Test
        fun `muleCount for White`() {
            assertThat(board.muleCount(White)).isEqualTo(2)
        }

        @Test
        fun `muleCount for Black`() {
            assertThat(board.muleCount(Black)).isEqualTo(3)
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
                .setStone(White, 1)
                .setStone(White, 2)
                .setStone(Black, 3)
                .setStone(Black, 4)
                .setStone(Black, 5)
                .setStone(Black, 6)

            assertThat(board.capturablePieces(White).map { it.index }).containsExactlyInAnyOrder(1, 2)
            assertThat(board.capturablePieces(Black).map { it.index }).containsExactlyInAnyOrder(6)
        }
    }

    @Nested
    inner class ChooseMove {
        inner class TestEvaluationStrategy : EvaluationStrategy {
            override fun evaluate(board: Board) = when {
                board.getStone(10) != Empty -> 10.0
                else                        -> 0.0
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
            val board = Board(white = white, black = black).setStone(White, 0).setStone(White, 1).setStone(White, 2)
                .setStone(White, 9)
                .setStone(Black, 4).setStone(Black, 10).setStone(Black, 14).setStone(Black, 21)

            assertThat(board.noLooser()).isFalse()
            assertThat(board.showWinner()).isEqualTo("Black is the winner")
        }

        @Test
        fun `black has no moves`() {
            val white = Player(White, 4, 9, Phase.MOVING)
            val black = Player(Black, 4, 9, Phase.MOVING)
            val board =
                Board(white = white, black = black, activePlayerColor = Black).setStone(Black, 0).setStone(Black, 1)
                    .setStone(Black, 2).setStone(Black, 9)
                    .setStone(White, 4).setStone(White, 10).setStone(White, 14).setStone(White, 21)

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
                .setStone(White, 3)
                .setStone(White, 4)
                .setStone(Black, 5)
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

    private fun Board.setStone(color: Color, field: Int) = setStone(color, field.asFieldIndex)
    private fun Board.setStones(color: Color, vararg fields: Int) =
        fields.fold(this) { board, field -> board.setStone(color, field) }

    private fun Board.getStone(field: Int) = getStone(field.asFieldIndex)
    private fun Board.moveStone(from: Int, to: Int) = moveStone(from.asFieldIndex, to.asFieldIndex)
}
