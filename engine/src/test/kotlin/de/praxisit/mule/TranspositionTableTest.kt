package de.praxisit.mule

import de.praxisit.mule.FieldIndex.Companion.asFieldIndex
import de.praxisit.mule.TranspositionTable.Bound.EXACT
import de.praxisit.mule.TranspositionTable.Bound.LOWER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TranspositionTableTest {
    private val table = TranspositionTable(sizeBits = 4)

    @Test
    fun `find a stored entry`() {
        val move = SetMove(White, 3.asFieldIndex)
        table.store(42L, 3, 1.5, EXACT, move)

        val entry = table[42L]

        assertThat(entry?.depth).isEqualTo(3)
        assertThat(entry?.score).isEqualTo(1.5)
        assertThat(entry?.bound).isEqualTo(EXACT)
        assertThat(entry?.move).isEqualTo(move)
    }

    @Test
    fun `an unknown key is not found`() {
        table.store(42L, 3, 1.5, EXACT, null)

        assertThat(table[43L]).isNull()
    }

    @Test
    fun `a new entry for the same key replaces the old one`() {
        table.store(42L, 3, 1.5, EXACT, null)
        table.store(42L, 5, -2.0, LOWER, null)

        assertThat(table[42L]?.depth).isEqualTo(5)
        assertThat(table[42L]?.bound).isEqualTo(LOWER)
    }
}
