package de.praxisit.mule

import de.praxisit.mule.AlphaBetaStrategy.Companion.WIN
import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import de.praxisit.mule.GameResult.Remis
import de.praxisit.mule.GameResult.Win
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

class AlphaBetaStrategyTest {
    private val evaluation = ExtendedEvaluationStrategy()

    @Test
    fun `get move for Black`() {
        val state = createState(listOf(0, 3, 7, 8, 9, 10, 16, 19, 22), 0, listOf(1, 2, 5, 23), 0, Black)

        val move = AlphaBetaStrategy().chooseMove(state)

        assertThat(state.legalMoves).contains(move)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "0 3 7 8 9 10 16 19 22, 0, 1 2 5 23, 0, Black",
            "0 4 9 13 19, 0, 1 10 12 20, 0, Black",
            "0 4 9 13 19, 0, 1 10 12 20, 0, White",
            "1 4 6 10 15 21, 0, 2 5 8 12 17 23, 0, Black",
            "0 1, 7, 4, 8, Black",
            "0 1 10 14, 0, 19 21 22, 0, White"
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
        val state = createState(whiteStones.toFields(), whiteStonesToSet, blackStones.toFields(), blackStonesToSet, color)

        val move = AlphaBetaStrategy(DEPTH, evaluation).chooseMove(state)

        assertThat(minimax(state.play(move), DEPTH - 1, 1)).isEqualTo(minimax(state, DEPTH, 0))
    }

    @Test
    fun `win as fast as possible`() {
        val state = createState(listOf(0, 1, 10, 14), 0, listOf(19, 21, 22), 0, White)

        val move = AlphaBetaStrategy(DEPTH).chooseMove(state)

        assertThat(state.play(move).result).isEqualTo(Win(White))
    }

    @Test
    fun `choose a move on a repeated position`() {
        val state = createRepeatedState()

        val move = AlphaBetaStrategy(DEPTH).chooseMove(state)

        assertThat(state.legalMoves).contains(move)
    }

    @Test
    fun `use the given evaluation`() {
        val prefersField10 = EvaluationStrategy { position ->
            if (position.board.getStone(10) == White) 1.0 else 0.0
        }

        val move = AlphaBetaStrategy(1, prefersField10).chooseMove(GameState())

        assertThat(move).isEqualTo(SetMove(White, 10.asFieldIndex))
    }

    @Test
    fun `stop searching when the time is up`() {
        val state = createBackAndForthState()
        val strategy = AlphaBetaStrategy(depth = 30, timeLimit = 100.milliseconds)

        val (move, duration) = measureTimedValue { strategy.chooseMove(state) }

        assertThat(state.legalMoves).contains(move)
        assertThat(duration.inWholeMilliseconds).isLessThan(3000)
    }

    // Plain minimax from White's point of view as a reference for the search
    private fun minimax(state: GameState, depth: Int, ply: Int): Double {
        val result = state.result
        return when {
            result is Win   -> if (result.winner == White) WIN - ply else -(WIN - ply)
            result == Remis -> 0.0
            depth == 0      -> evaluation.evaluate(state.position)
            else            -> {
                val values = state.legalMoves.map { minimax(state.play(it), depth - 1, ply + 1) }
                if (state.activeColor == White) values.max() else values.min()
            }
        }
    }

    companion object {
        private const val DEPTH = 3
    }
}
