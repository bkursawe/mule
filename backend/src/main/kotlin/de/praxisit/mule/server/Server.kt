package de.praxisit.mule.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

/** Starts the web server on the port given by the environment variable PORT, 8080 by default. */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) { module() }.start(wait = true)
}

@Serializable
data class ErrorDto(val message: String)

fun Application.module(games: GameService = GameService()) {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<GameNotFoundException> { call, cause -> call.respondError(HttpStatusCode.NotFound, cause) }
        exception<BadRequestException> { call, cause -> call.respondError(HttpStatusCode.BadRequest, cause) }
        exception<IllegalArgumentException> { call, cause -> call.respondError(HttpStatusCode.BadRequest, cause) }
        exception<IllegalStateException> { call, cause -> call.respondError(HttpStatusCode.Conflict, cause) }
    }
    routing {
        gameRoutes(games)
        staticResources("/", "static")
    }
}

/**
 * POST /api/games starts a game, GET /api/games/{id} returns it,
 * POST /api/games/{id}/moves plays a move of the human and POST /api/games/{id}/computer-move one of the computer.
 */
fun Route.gameRoutes(games: GameService) {
    route("/api/games") {
        post {
            val request = call.receive<NewGameRequest>()
            call.respond(HttpStatusCode.Created, games.create(request.humanColor.toColor(), request.strength))
        }
        get("/{id}") {
            call.respond(games.get(call.gameId))
        }
        post("/{id}/moves") {
            val move = call.receive<MoveDto>().toMove()
            call.respond(games.playHumanMove(call.gameId, move))
        }
        post("/{id}/computer-move") {
            call.respond(games.playComputerMove(call.gameId))
        }
    }
}

private val RoutingCall.gameId: String
    get() = requireNotNull(parameters["id"])

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, cause: Throwable) =
    respond(status, ErrorDto(cause.message ?: status.description))
