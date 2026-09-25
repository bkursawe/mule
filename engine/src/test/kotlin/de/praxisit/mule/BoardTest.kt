package de.praxisit.mule

import de.praxisit.mule.Board.Companion.COMPLETABLE_MULES
import de.praxisit.mule.Board.Companion.MULES
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
            assertThat(emptyBoard.emptyFieldsIndices).hasSize(24)
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
        fun `setting a stone does not change the original board`() {
            emptyBoard.setStone(White, 5)

            assertThat(emptyBoard.getStone(5)).isEqualTo(Empty)
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

    @Test
    fun `remove a stone`() {
        val board = emptyBoard.setStones(White, 3, 4)

        val endBoard = board.removeStone(FieldIndex(3))

        assertThat(endBoard.fieldsIndicesWithColor(White).map { it.index }).containsExactly(4)
    }

    @Test
    fun `boards with the same stones are equal`() {
        val board = emptyBoard.setStone(White, 0)

        assertThat(board).isEqualTo(Board().setStone(White, 0))
        assertThat(board.hashCode()).isEqualTo(Board().setStone(White, 0).hashCode())
        assertThat(board).isNotEqualTo(board.setStone(Black, 1))
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

        @Test
        fun `all pieces are capturable if every piece is in a mule`() {
            val board = emptyBoard.setStones(Black, 3, 4, 5)

            assertThat(board.capturablePieces(Black).map { it.index }).containsExactlyInAnyOrder(3, 4, 5)
        }
    }

    @Test
    fun `weighted stones count the connections of each occupied field`() {
        val board = emptyBoard.setStones(White, 0, 4).setStone(Black, 1)

        assertThat(board.weightedStonesOnBoard(White)).isEqualTo(6)
        assertThat(board.weightedStonesOnBoard(Black)).isEqualTo(3)
    }
}
