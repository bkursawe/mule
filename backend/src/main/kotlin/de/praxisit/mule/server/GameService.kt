package de.praxisit.mule.server

import de.praxisit.mule.AlphaBetaStrategy
import de.praxisit.mule.ChoosingStrategy
import de.praxisit.mule.Color
import de.praxisit.mule.ExtendedEvaluationStrategy
import de.praxisit.mule.GameResult.Ongoing
import de.praxisit.mule.GameState
import de.praxisit.mule.Move
import de.praxisit.mule.MoveRater
import de.praxisit.mule.WeightedRandomStrategy
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

// Moves this many points worse than the best one are e times less likely, so the weak levels vary their games
private const val VARIETY = 10.0

/**
 * The levels of the computer, weakest first. The two weakest look only at the position after their own move,
 * so a beginner can win against them.
 */
enum class Strength(val createComputer: (computerColor: Color) -> ChoosingStrategy) {
    // Closes every mule it sees, but overlooks the ones of the human
    BEGINNER({ color -> WeightedRandomStrategy(1, VARIETY, ExtendedEvaluationStrategy(overlooked = color.opposite)) }),

    // Sees the open mules of the human, but not what they cost, so it often builds its own instead of blocking
    EASY({ WeightedRandomStrategy(1, VARIETY) }),

    // Also sees the answer of the human
    MEDIUM({ WeightedRandomStrategy(2, 3.0) }),
    HARD({ AlphaBetaStrategy(depth = 20, timeLimit = 1.seconds) }),
    MASTER({ AlphaBetaStrategy(depth = 30, timeLimit = 3.seconds) })
}

class GameNotFoundException(id: String) : RuntimeException("No game with id $id")

/**
 * A game between a human and the computer. Moves are played one after the other, guarded by [mutex].
 */
class GameSession(
    val id: String,
    val humanColor: Color,
    val strength: Strength,
    now: Instant,
    initialState: GameState = GameState()
) {
    private val computer = strength.createComputer(humanColor.opposite)
    private val mutex = Mutex()
    private val moves = mutableListOf<Move>()
    private var state = initialState

    // The ratings have a lock of their own, so the human can move while they are computed
    private val rater by lazy { MoveRater() }
    private val ratingMutex = Mutex()
    private var ratings: RatingsDto? = null

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

    /** Rates the legal moves of the human; the ratings of a position are computed only once. */
    suspend fun rateMoves(now: Instant): RatingsDto {
        val (current, moveNumber) = mutex.withLock {
            lastAccess = now
            check(state.result == Ongoing) { "The game is over" }
            check(state.activeColor == humanColor) { "It is the computer's turn" }
            state to moves.size
        }
        return ratingMutex.withLock {
            ratings?.takeIf { it.moveNumber == moveNumber }
                ?: withContext(Dispatchers.Default) { rater.rate(current).toDto(moveNumber) }.also { ratings = it }
        }
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

    suspend fun create(humanColor: Color, strength: Strength, initialState: GameState = GameState()): GameDto {
        removeIdleGames()
        val session = GameSession(UUID.randomUUID().toString(), humanColor, strength, clock.instant(), initialState)
        sessions[session.id] = session
        return session.snapshot(clock.instant())
    }

    suspend fun get(id: String) = session(id).snapshot(clock.instant())

    suspend fun playHumanMove(id: String, move: Move) = session(id).playHumanMove(move, clock.instant())

    suspend fun playComputerMove(id: String) = session(id).playComputerMove(clock.instant())

    suspend fun rateMoves(id: String) = session(id).rateMoves(clock.instant())

    private fun session(id: String) = sessions[id] ?: throw GameNotFoundException(id)

    private fun removeIdleGames() {
        val oldestAllowed = clock.instant() - maxIdleTime
        sessions.values.removeIf { it.lastAccess < oldestAllowed }
    }
}
