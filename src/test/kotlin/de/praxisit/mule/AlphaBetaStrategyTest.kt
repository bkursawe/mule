package de.praxisit.mule

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AlphaBetaStrategyTest {

    @Test
    fun `get move for Black`() {
        val board = createBoard(listOf(0, 3, 7, 8, 9, 10, 16, 19, 22), 0, listOf(1, 2, 5, 23), 0, Black)

        val move = board.chooseMove()

        assertThat(move).isNotEqualTo(NoMove)
    }

    companion object {
        fun createBoard(
            whiteStones: List<Int>,
            whiteStonesToSet: Int,
            blackStones: List<Int>,
            blackStonesToSet: Int,
            activePlayerColor: Color
        ): Board {
            fun phase(stonesToSet: Int, stonesOnBoard: Int) = when {
                stonesToSet > 0    -> Phase.SETTING
                stonesOnBoard > 3  -> Phase.MOVING
                stonesOnBoard == 3 -> Phase.JUMPING
                else               -> Phase.LOOSE
            }

            val fields = Array<Field>(24) { _ -> Empty }
            whiteStones.forEach { fields[it] = White }
            blackStones.forEach { fields[it] = Black }
            val white = Player(White, whiteStones.size, 9 - whiteStonesToSet, phase(whiteStonesToSet, whiteStones.size))
            val black = Player(Black, blackStones.size, 9 - blackStonesToSet, phase(blackStonesToSet, blackStones.size))
            val board = Board(fields, white, black, activePlayerColor)
            return board
        }
    }
}