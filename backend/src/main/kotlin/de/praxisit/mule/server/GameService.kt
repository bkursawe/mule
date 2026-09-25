package de.praxisit.mule.server

import de.praxisit.mule.AlphaBetaStrategy
import de.praxisit.mule.ChoosingStrategy
import de.praxisit.mule.Color
import de.praxisit.mule.GameResult.Ongoing
import de.praxisit.mule.GameState
import de.praxisit.mule.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

enum class Strength(val createComputer: () -> ChoosingStrategy) {
    EASY({ AlphaBetaStrategy(depth = 2) }),
    MEDIUM({ AlphaBetaStrategy(depth = 20, timeLimit = 1.seconds) }),
    HARD({ AlphaBetaStrategy(depth = 30, timeLimit = 3.seconds) })
}

class GameNotFoundException(id: String) : RuntimeException("No game with id $id")

/**
 * A game between a human and the computer. Moves are played one after the other, guarded by [mutex].
 */
class GameSession(val id: String, val humanColor: Color, val strength: Strength, now: Instant) {
    private val computer = strength.createComputer()
    private val mutex = Mutex()
    private val moves = mutableListOf<Move>()
    private var state = GameState()

    @Volatile
    var lastAccess: Instant = now
        private set

    suspend fun snapshot(now: Instant) = mutex.withLock {
        lastAccess = now
        toDto(state, moves)
    }

    suspend fun playHumanMove(move: Move, now: Instant) = mutex.withLock {
        lastAccess = now
        check(state.result == Ongoing) { "The game is over" }
        check(state.activeColor == humanColor) { "It is the computer's turn" }
        play(move)
    }

    suspend fun playComputerMove(now: Instant) = mutex.withLock {
        lastAccess = now
        check(state.result == Ongoing) { "The game is over" }
        check(state.activeColor != humanColor) { "It is the human's turn" }
        // The search takes up to a few seconds, so it must not block the server threads
        val move = withContext(Dispatchers.Default) { computer.chooseMove(state) }
        play(move)
    }

    private fun play(move: Move): GameDto {
        state = state.play(move)
        moves += move
        return toDto(state, moves)
    }

    private fun toDto(state: GameState, moves: List<Move>) = state.toDto(id, humanColor, strength, moves)
}

/**
 * Keeps the running games in memory. Games that nobody touched for [maxIdleTime] are removed.
 */
class GameService(
    private val clock: Clock = Clock.systemUTC(),
    private val maxIdleTime: Duration = Duration.ofHours(6)
) {
    private val sessions = ConcurrentHashMap<String, GameSession>()

    suspend fun create(humanColor: Color, strength: Strength): GameDto {
        removeIdleGames()
        val session = GameSession(UUID.randomUUID().toString(), humanColor, strength, clock.instant())
        sessions[session.id] = session
        return session.snapshot(clock.instant())
    }

    suspend fun get(id: String) = session(id).snapshot(clock.instant())

    suspend fun playHumanMove(id: String, move: Move) = session(id).playHumanMove(move, clock.instant())

    suspend fun playComputerMove(id: String) = session(id).playComputerMove(clock.instant())

    private fun session(id: String) = sessions[id] ?: throw GameNotFoundException(id)

    private fun removeIdleGames() {
        val oldestAllowed = clock.instant() - maxIdleTime
        sessions.values.removeIf { it.lastAccess < oldestAllowed }
    }
}
