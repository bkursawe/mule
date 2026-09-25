package de.praxisit.mule.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameApiTest {
    @Test
    fun `start a game`() = apiTest { client ->
        val response = client.startGame(ColorDto.WHITE)

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        val game = response.body<GameDto>()
        assertThat(game.board).hasSize(24).containsOnlyNulls()
        assertThat(game.activeColor).isEqualTo(ColorDto.WHITE)
        assertThat(game.legalMoves).hasSize(24)
        assertThat(game.white.stonesInHand).isEqualTo(9)
        assertThat(game.result.status).isEqualTo(ResultStatus.ONGOING)
    }

    @Test
    fun `get a game`() = apiTest { client ->
        val game = client.startGame(ColorDto.WHITE).body<GameDto>()

        val response = client.get("/api/games/${game.id}")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.body<GameDto>().id).isEqualTo(game.id)
    }

    @Test
    fun `play a move and let the computer answer`() = apiTest { client ->
        val game = client.startGame(ColorDto.WHITE).body<GameDto>()

        val afterHuman = client.playMove(game.id, MoveDto(MoveType.SET, ColorDto.WHITE, to = 4)).body<GameDto>()
        val afterComputer = client.post("/api/games/${game.id}/computer-move").body<GameDto>()

        assertThat(afterHuman.board[4]).isEqualTo(ColorDto.WHITE)
        assertThat(afterHuman.activeColor).isEqualTo(ColorDto.BLACK)
        assertThat(afterComputer.moves).hasSize(2)
        assertThat(afterComputer.moves[1].color).isEqualTo(ColorDto.BLACK)
        assertThat(afterComputer.activeColor).isEqualTo(ColorDto.WHITE)
    }

    @Test
    fun `the computer starts when the human plays black`() = apiTest { client ->
        val game = client.startGame(ColorDto.BLACK).body<GameDto>()

        val afterComputer = client.post("/api/games/${game.id}/computer-move").body<GameDto>()

        assertThat(afterComputer.board.count { it == ColorDto.WHITE }).isEqualTo(1)
        assertThat(afterComputer.activeColor).isEqualTo(ColorDto.BLACK)
    }

    @Test
    fun `an illegal move is rejected`() = apiTest { client ->
        val game = client.startGame(ColorDto.WHITE).body<GameDto>()

        val response = client.playMove(game.id, MoveDto(MoveType.PUSH, ColorDto.WHITE, to = 1, from = 0))

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `a field outside the board is rejected`() = apiTest { client ->
        val game = client.startGame(ColorDto.WHITE).body<GameDto>()

        val response = client.playMove(game.id, MoveDto(MoveType.SET, ColorDto.WHITE, to = 24))

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `the human cannot move on the computer's turn`() = apiTest { client ->
        val game = client.startGame(ColorDto.BLACK).body<GameDto>()

        val response = client.playMove(game.id, MoveDto(MoveType.SET, ColorDto.BLACK, to = 4))

        assertThat(response.status).isEqualTo(HttpStatusCode.Conflict)
    }

    @Test
    fun `the computer cannot move on the human's turn`() = apiTest { client ->
        val game = client.startGame(ColorDto.WHITE).body<GameDto>()

        val response = client.post("/api/games/${game.id}/computer-move")

        assertThat(response.status).isEqualTo(HttpStatusCode.Conflict)
    }

    @Test
    fun `an unknown game is not found`() = apiTest { client ->
        val response = client.get("/api/games/unknown")

        assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `the web frontend is served`() = apiTest { client ->
        val response = client.get("/")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).contains("<title>Mühle</title>")
    }

    @Test
    fun `the health check answers`() = apiTest { client ->
        val response = client.get("/health")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isEqualTo("OK")
    }

    private fun apiTest(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application { module(GameService()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun HttpClient.startGame(humanColor: ColorDto): HttpResponse = post("/api/games") {
        contentType(ContentType.Application.Json)
        setBody(NewGameRequest(humanColor, Strength.EASY))
    }

    private suspend fun HttpClient.playMove(id: String, move: MoveDto): HttpResponse = post("/api/games/$id/moves") {
        contentType(ContentType.Application.Json)
        setBody(move)
    }
}
