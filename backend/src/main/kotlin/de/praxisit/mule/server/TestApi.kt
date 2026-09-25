package de.praxisit.mule.server

import de.praxisit.mule.Black
import de.praxisit.mule.Board
import de.praxisit.mule.FieldIndex
import de.praxisit.mule.GameState
import de.praxisit.mule.Player
import de.praxisit.mule.Player.Companion.STONES
import de.praxisit.mule.Position
import de.praxisit.mule.White
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

/**
 * A position to start a game from. Each player owns the stones on the board plus the stones in hand.
 */
@Serializable
data class TestGameRequest(
    val humanColor: ColorDto = ColorDto.WHITE,
    val strength: Strength = Strength.EASY,
    val activeColor: ColorDto = ColorDto.WHITE,
    val white: List<Int> = emptyList(),
    val black: List<Int> = emptyList(),
    val whiteStonesInHand: Int = 0,
    val blackStonesInHand: Int = 0,
    val movesWithoutCapture: Int = 0
) {
    fun toGameState(): GameState {
        require((white + black).toSet().size == white.size + black.size) { "A field is used twice" }
        require(white.size + whiteStonesInHand <= STONES && black.size + blackStonesInHand <= STONES) {
            "A player has at most $STONES stones"
        }
        require(whiteStonesInHand >= 0 && blackStonesInHand >= 0 && movesWithoutCapture >= 0) {
            "Counts must not be negative"
        }
        val board = white.fold(Board()) { board, field -> board.setStone(White, FieldIndex(field)) }
            .let { black.fold(it) { board, field -> board.setStone(Black, FieldIndex(field)) } }
        val position = Position(
            board = board,
            white = Player(White, white.size + whiteStonesInHand, STONES - whiteStonesInHand),
            black = Player(Black, black.size + blackStonesInHand, STONES - blackStonesInHand),
            activeColor = activeColor.toColor()
        )
        return GameState(position, movesWithoutCapture)
    }
}

/**
 * Only for automated tests: POST /api/test/games starts a game from any position,
 * so end-to-end tests do not have to play their way there against the computer.
 */
fun Route.testRoutes(games: GameService) {
    post("/api/test/games") {
        val request = call.receive<TestGameRequest>()
        val game = games.create(request.humanColor.toColor(), request.strength, request.toGameState())
        call.respond(HttpStatusCode.Created, game)
    }
}
