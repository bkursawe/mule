package de.praxisit.mule.console

import de.praxisit.mule.*
import de.praxisit.mule.GameResult.Ongoing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameTest {
    @Test
    fun `a game between two players ends with a result`() {
        val result = Game(white = SimpleChoosingStrategy(), black = SimpleChoosingStrategy()).play()

        assertThat(result).isNotEqualTo(Ongoing)
    }
}
