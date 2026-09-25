package de.praxisit.mule

import de.praxisit.mule.GameResult.*
import de.praxisit.mule.TranspositionTable.Bound.*
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.time.Duration

/**
 * Chooses a move for the active player of a game that is not finished yet.
 */
fun interface ChoosingStrategy {
    fun chooseMove(state: GameState): Move
}

/**
 * Chooses the move with the best evaluation after one move.
 */
class SimpleChoosingStrategy(
    private val evaluation: EvaluationStrategy = ExtendedEvaluationStrategy()
) : ChoosingStrategy {
    override fun chooseMove(state: GameState): Move {
        val score = { move: Move -> evaluation.evaluate(state.play(move).position) }
        val move = if (state.activeColor == White) {
            state.legalMoves.maxByOrNull(score)
        } else {
            state.legalMoves.minByOrNull(score)
        }
        return checkNotNull(move) { "No legal move" }
    }
}

/**
 * Searches up to [depth] moves ahead with Negamax and alpha-beta pruning. The search deepens
 * iteratively and stops early after [timeLimit]; it then plays the best move of the deepest finished search.
 * All scores are seen from the player to move, a win scores [WIN] minus the moves needed to reach it.
 */
class AlphaBetaStrategy(
    private val depth: Int = 5,
    private val evaluation: EvaluationStrategy = ExtendedEvaluationStrategy(),
    private val timeLimit: Duration? = null
) : ChoosingStrategy {
    // Kept between moves: positions of the last search often come up again
    private val table by lazy { TranspositionTable() }

    override fun chooseMove(state: GameState): Move {
        check(state.legalMoves.isNotEmpty()) { "No legal move" }

        val search = Search(deadline = timeLimit?.let { System.nanoTime() + it.inWholeNanoseconds })
        var bestMove = state.legalMoves.first()
        for (currentDepth in 1..depth) {
            bestMove = search.bestRootMove(state, currentDepth) ?: break
        }
        return bestMove
    }

    private inner class Search(private val deadline: Long?) {
        private val killerMoves = Array(depth + 1) { arrayOfNulls<Move>(2) }
        private var nodes = 0L
        private var finishedDepth = 0

        /** The best move of a search to [depth], or null if the time ran out before it finished. */
        fun bestRootMove(state: GameState, depth: Int): Move? = try {
            // The root is searched without the terminal checks, so a legal move is returned whenever one exists
            val (move, _) = bestMove(state, depth, 0, NEGATIVE_INFINITY, POSITIVE_INFINITY)
            finishedDepth = depth
            move
        } catch (e: SearchTimeout) {
            null
        }

        private fun negamax(state: GameState, depth: Int, ply: Int, alpha: Double, beta: Double): Double {
            checkTime()
            when (state.result) {
                is Win  -> return -(WIN - ply)
                Remis   -> return 0.0
                Ongoing -> Unit
            }
            if (depth == 0) return state.activeColor.sign * evaluation.evaluate(state.position)

            val entry = table[state.key]
            if (entry != null && entry.depth >= depth) {
                val score = entry.score.fromTable(ply)
                when (entry.bound) {
                    EXACT -> return score
                    LOWER -> if (score >= beta) return score
                    UPPER -> if (score <= alpha) return score
                }
            }
            return bestMove(state, depth, ply, alpha, beta).second
        }

        private fun bestMove(
            state: GameState,
            depth: Int,
            ply: Int,
            alpha: Double,
            beta: Double
        ): Pair<Move?, Double> {
            val key = state.key
            var bestMove: Move? = null
            var bestScore = NEGATIVE_INFINITY
            var currentAlpha = alpha
            for (move in orderedMoves(state, table[key]?.move, ply)) {
                val score = -negamax(state.play(move), depth - 1, ply + 1, -beta, -currentAlpha)
                if (score > bestScore || bestMove == null) {
                    bestScore = score
                    bestMove = move
                }
                currentAlpha = maxOf(currentAlpha, score)
                if (currentAlpha >= beta) {
                    if (!move.isCaptureMove) rememberKillerMove(move, ply)
                    break
                }
            }
            val bound = when {
                bestScore <= alpha -> UPPER
                bestScore >= beta  -> LOWER
                else               -> EXACT
            }
            table.store(key, depth, bestScore.toTable(ply), bound, bestMove)
            return Pair(bestMove, bestScore)
        }

        // Good moves first make more cutoffs: the best move found before, captures, then moves that caused cutoffs
        private fun orderedMoves(state: GameState, hashMove: Move?, ply: Int): List<Move> {
            val killers = killerMoves[ply]
            return state.legalMoves.sortedBy { move ->
                when {
                    move == hashMove                         -> 0
                    move.isCaptureMove                       -> 1
                    move == killers[0] || move == killers[1] -> 2
                    else                                     -> 3
                }
            }
        }

        private fun rememberKillerMove(move: Move, ply: Int) {
            val killers = killerMoves[ply]
            if (move != killers[0]) {
                killers[1] = killers[0]
                killers[0] = move
            }
        }

        // The first search always finishes, so there is a move to play
        private fun checkTime() {
            nodes++
            if (deadline != null && finishedDepth > 0 && nodes % 1024 == 0L && System.nanoTime() > deadline) {
                throw SearchTimeout
            }
        }
    }

    private object SearchTimeout : RuntimeException() {
        override fun fillInStackTrace() = this
    }

    companion object {
        const val WIN = 1_000_000.0

        // Wins are stored relative to the stored position, so they stay correct when found on another ply
        private const val WIN_THRESHOLD = WIN - 1000

        private fun Double.toTable(ply: Int) = when {
            this > WIN_THRESHOLD  -> this + ply
            this < -WIN_THRESHOLD -> this - ply
            else                  -> this
        }

        private fun Double.fromTable(ply: Int) = when {
            this > WIN_THRESHOLD  -> this - ply
            this < -WIN_THRESHOLD -> this + ply
            else                  -> this
        }

        private val Color.sign: Double
            get() = if (this == White) 1.0 else -1.0

        // Identifies a game state for the transposition table. The number of stones follows from the board and
        // the stones set; the moves without capture are part of the key because the remis rule depends on them.
        private val GameState.key: Long
            get() = board.stones(White).toLong() or
                    (board.stones(Black).toLong() shl 24) or
                    (position.white.stonesSet.toLong() shl 48) or
                    (position.black.stonesSet.toLong() shl 52) or
                    ((if (activeColor == White) 0L else 1L) shl 56) or
                    (movesWithoutCapture.coerceAtMost(63).toLong() shl 57)
    }
}
