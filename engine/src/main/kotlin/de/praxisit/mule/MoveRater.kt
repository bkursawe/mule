package de.praxisit.mule

import de.praxisit.mule.Rating.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How a move compares with the other legal moves of the same position. */
enum class Rating { GOOD, NEUTRAL, BAD }

/**
 * Rates the legal moves of the active player for someone who learns the game. All moves are scored with
 * [AlphaBetaStrategy.scoreMoves]; a move counts with the mean of its scores from the two deepest searches, because a
 * search that ends on the own move judges it more hopefully than one that ends on the answer of the opponent.
 * Then each move is compared with the best move and with the typical one, the median:
 * - [GOOD]: at most [goodMargin] points below the best move and at least [goodGain] points above the median.
 *   A move is only good if it stands out, so when most moves are about equal none of them is.
 * - [BAD]: [badLoss] points or more below the best move, for example because it lets the opponent close a mule.
 * - [NEUTRAL]: all others.
 */
class MoveRater(
    depth: Int = 8,
    timeLimit: Duration? = 600.milliseconds,
    evaluation: EvaluationStrategy = ExtendedEvaluationStrategy(),
    private val goodMargin: Double = 10.0,
    private val goodGain: Double = 25.0,
    private val badLoss: Double = 60.0
) {
    private val search = AlphaBetaStrategy(depth, evaluation, timeLimit)

    fun rate(state: GameState): Map<Move, Rating> {
        val deepest = search.scoreMoves(state).takeLast(2)
        val scores = deepest.last().mapValues { (move, _) -> deepest.map { it.getValue(move) }.average() }
        val best = scores.values.max()
        val median = scores.values.sorted()[scores.size / 2]
        return scores.mapValues { (_, score) ->
            when {
                best - score >= badLoss                                  -> BAD
                best - score <= goodMargin && score - median >= goodGain -> GOOD
                else                                                     -> NEUTRAL
            }
        }
    }
}
