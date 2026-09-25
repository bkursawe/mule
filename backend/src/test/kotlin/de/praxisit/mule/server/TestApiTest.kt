package de.praxisit.mule.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestApiTest {
    @Test
    fun `the test API is off by default`() = testApplication {
        application { module(GameService(), testApi = false) }

        val response = jsonClient().startFrom(TestGameRequest())

        assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `start a game from a position`() = testApplication {
        application { module(GameService(), testApi = true) }
        val request = TestGameRequest(
            activeColor = ColorDto.WHITE,
            white = listOf(0, 1, 14, 20),
            black = listOf(3, 4, 5, 22),
            movesWithoutCapture = 12
        )

        val response = jsonClient().startFrom(request)

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        val game = response.body<GameDto>()
        assertThat(game.board[0]).isEqualTo(ColorDto.WHITE)
        assertThat(game.board[22]).isEqualTo(ColorDto.BLACK)
        assertThat(game.white.stonesOnBoard).isEqualTo(4)
        assertThat(game.movesWithoutCapture).isEqualTo(12)
        // Pushing 14 to 2 closes the mule 0-1-2; the black stones in the mule 3-4-5 are protected
        assertThat(game.legalMoves.filter { it.from == 14 && it.to == 2 }.map { it.capture }).containsExactly(22)
    }

    @Test
    fun `a field used twice is rejected`() = testApplication {
        application { module(GameService(), testApi = true) }

        val response = jsonClient().startFrom(TestGameRequest(white = listOf(0), black = listOf(0)))

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient { install(ContentNegotiation) { json() } }

    private suspend fun HttpClient.startFrom(request: TestGameRequest): HttpResponse = post("/api/test/games") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }
}
