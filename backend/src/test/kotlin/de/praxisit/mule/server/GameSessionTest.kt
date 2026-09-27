package de.praxisit.mule.server

import de.praxisit.mule.Black
import de.praxisit.mule.ChoosingStrategy
import de.praxisit.mule.FieldIndex
import de.praxisit.mule.GameState
import de.praxisit.mule.PushMove
import de.praxisit.mule.SetMove
import de.praxisit.mule.White
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class GameSessionTest {
    private val now = Instant.parse("2026-09-27T10:00:00Z")

    // Always the first legal move: in the setting phase the free field with the lowest number
    private val firstMove = ChoosingStrategy { state -> state.legalMoves.first() }

    private fun session(initialState: GameState = GameState()) =
        GameSession("game", White, Strength.MEDIUM, now, initialState, firstMove)

    private fun set(field: Int) = SetMove(White, FieldIndex(field))

    @Nested
    inner class TakeBack {
        @Test
        fun `take back the move of the human and the answer of the computer`() = runBlocking<Unit> {
            val game = session()
            game.playHumanMove(set(4), now)
            game.playComputerMove(now)

            val afterTakeBack = game.takeBack(now)

            assertThat(afterTakeBack.moves).isEmpty()
            assertThat(afterTakeBack.board).containsOnlyNulls()
            assertThat(afterTakeBack.activeColor).isEqualTo(ColorDto.WHITE)
            assertThat(afterTakeBack.white.stonesInHand).isEqualTo(9)
            assertThat(afterTakeBack.black.stonesInHand).isEqualTo(9)
        }

        @Test
        fun `take back one move after the other`() = runBlocking<Unit> {
            val game = session()
            game.playHumanMove(set(4), now)
            game.playComputerMove(now)
            game.playHumanMove(set(10), now)
            game.playComputerMove(now)

            val afterFirst = game.takeBack(now)
            val afterSecond = game.takeBack(now)

            assertThat(afterFirst.moves.map { it.to }).containsExactly(4, 0)
            assertThat(afterSecond.moves).isEmpty()
        }

        @Test
        fun `the game goes on after taking back`() = runBlocking<Unit> {
            val game = session()
            game.playHumanMove(set(4), now)
            game.playComputerMove(now)
            game.takeBack(now)

            game.playHumanMove(set(10), now)
            val afterComputer = game.playComputerMove(now)

            assertThat(afterComputer.moves.map { it.to }).containsExactly(10, 0)
            assertThat(afterComputer.board[4]).isNull()
        }

        @Test
        fun `a move that ended the game can be taken back`() = runBlocking<Unit> {
            // Pushing 14 to 2 closes 0-1-2 and leaves Black with two stones
            val position = TestGameRequest(white = listOf(0, 1, 14, 20), black = listOf(3, 4, 5)).toGameState()
            val game = session(position)
            val won = game.playHumanMove(PushMove(White, FieldIndex(14), FieldIndex(2), FieldIndex(4)), now)
            assertThat(won.result.status).isEqualTo(ResultStatus.WIN)

            val afterTakeBack = game.takeBack(now)

            assertThat(afterTakeBack.result.status).isEqualTo(ResultStatus.ONGOING)
            assertThat(afterTakeBack.board[14]).isEqualTo(ColorDto.WHITE)
            assertThat(afterTakeBack.board[4]).isEqualTo(ColorDto.BLACK)
        }

        @Test
        fun `without a move of the human there is nothing to take back`() {
            val game = GameSession("game", Black, Strength.MEDIUM, now, computer = firstMove)
            runBlocking { game.playComputerMove(now) }

            assertThatThrownBy { runBlocking { game.takeBack(now) } }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `nothing is taken back while the computer is to move`() {
            val game = session()
            runBlocking { game.playHumanMove(set(4), now) }

            assertThatThrownBy { runBlocking { game.takeBack(now) } }
                .isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun `after taking back, the ratings belong to the new position, not to an earlier one with as many moves`() =
        runBlocking<Unit> {
            val game = session()
            game.playHumanMove(set(4), now)
            game.playComputerMove(now)
            game.rateMoves(now)
            game.takeBack(now)
            game.playHumanMove(set(10), now)
            val position = game.playComputerMove(now)

            val ratings = game.rateMoves(now)

            assertThat(ratings.moveNumber).isEqualTo(2)
            assertThat(ratings.moves.map { it.move }).containsExactlyInAnyOrderElementsOf(position.legalMoves)
        }
}
