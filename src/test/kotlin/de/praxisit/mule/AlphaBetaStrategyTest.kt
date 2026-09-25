package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

class AlphaBetaStrategyTest {

    @Test
    fun `get move for Black`() {
        val board = createBoard(listOf(0, 3, 7, 8, 9, 10, 16, 19, 22), 0, listOf(1, 2, 5, 23), 0, Black)

        val move = board.chooseMove()

        assertThat(move).isNotEqualTo(NoMove)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "0 3 7 8 9 10 16 19 22, 0, 1 2 5 23, 0, Black",
            "0 4 9 13 19, 0, 1 10 12 20, 0, Black",
            "0 4 9 13 19, 0, 1 10 12 20, 0, White",
            "1 4 6 10 15 21, 0, 2 5 8 12 17 23, 0, Black",
            "0 1, 7, 4, 8, Black"
        ]
    )
    fun `choose a move with the minimax value`(
        whiteStones: String,
        whiteStonesToSet: Int,
        blackStones: String,
        blackStonesToSet: Int,
        colorName: String
    ) {
        val color = if (colorName == "White") White else Black
        val board = createBoard(whiteStones.toFields(), whiteStonesToSet, blackStones.toFields(), blackStonesToSet, color)

        val move = AlphaBetaStrategy(DEPTH).chooseMove(board)

        assertThat(minimax(board.draw(move).withSwitchedPlayer, DEPTH - 1)).isEqualTo(minimax(board, DEPTH))
    }

    @Test
    fun `choose a move on a repeated position`() {
        val board = createRepeatedBoard()

        val move = AlphaBetaStrategy(DEPTH).chooseMove(board)

        assertThat(board.legalMoves).contains(move)
    }

    private fun String.toFields() = split(" ").map { it.toInt() }

    private fun minimax(board: Board, depth: Int): Double = when {
        board.activePlayer.phase == Phase.LOOSE -> board.activePlayer.worstEvaluation
        board.isRepeated                        -> 0.0
        depth == 0                              -> board.evaluation
        else                                    -> {
            val values = board.legalMoves.map { minimax(board.draw(it).withSwitchedPlayer, depth - 1) }
            if (board.activePlayerColor == White) values.maxOrNull() ?: NEGATIVE_INFINITY
            else values.minOrNull() ?: POSITIVE_INFINITY
        }
    }

    companion object {
        private const val DEPTH = 3

        fun createBoard(
            whiteStones: List<Int>,
            whiteStonesToSet: Int,
            blackStones: List<Int>,
            blackStonesToSet: Int,
            activePlayerColor: Color
        ): Board {
            val fields = Array<Field>(24) { _ -> Empty }
            whiteStones.forEach { fields[it] = White }
            blackStones.forEach { fields[it] = Black }
            val white = Player(White, whiteStones.size + whiteStonesToSet, 9 - whiteStonesToSet)
            val black = Player(Black, blackStones.size + blackStonesToSet, 9 - blackStonesToSet)
            val board = Board(fields, white, black, activePlayerColor)
            return board
        }

        /** Plays White 0↔1 and Black 23↔22 back and forth until the start position occurs for the third time. */
        fun createRepeatedBoard(): Board {
            val start = createBoard(listOf(0, 4, 9, 13), 0, listOf(10, 12, 20, 23), 0, White)
            return (1..2).fold(start) { board, _ -> board.playBackAndForth() }
        }

        fun Board.playBackAndForth() = listOf(
            PushMove(White, 0.asFieldIndex, 1.asFieldIndex),
            PushMove(Black, 23.asFieldIndex, 22.asFieldIndex),
            PushMove(White, 1.asFieldIndex, 0.asFieldIndex),
            PushMove(Black, 22.asFieldIndex, 23.asFieldIndex)
        ).fold(this) { board, move -> board.draw(move).withSwitchedPlayer }
    }
}
