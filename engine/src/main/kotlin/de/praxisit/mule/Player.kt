package de.praxisit.mule

import de.praxisit.mule.Phase.*

/**
 * The stones of one player. [stones] counts all stones the player still owns, on the board and in hand.
 */
data class Player(val color: Color, val stones: Int = STONES, val stonesSet: Int = 0) {
    val remainingStones: Int
        get() = STONES - stonesSet

    val phase: Phase
        get() = when {
            stones < 3         -> LOST
            stonesSet < STONES -> SETTING
            stones == 3        -> JUMPING
            else               -> MOVING
        }

    fun loseStone(): Player {
        check(phase != LOST)

        return copy(stones = stones - 1)
    }

    fun setStone(): Player {
        check(phase == SETTING)

        return copy(stonesSet = stonesSet + 1)
    }

    companion object {
        const val STONES = 9
    }
}
