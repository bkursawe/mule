package de.praxisit.muli.board

import de.praxisit.muli.board.Color.BLACK
import de.praxisit.muli.board.Color.WHITE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MoveTest {
    @Test
    fun `print set moves`() {
        assertThat(SetMove(WHITE, 1).toString()).isEqualTo("SetMove(WHITE, 1)")
        assertThat(SetMove(BLACK, 23, 5).toString()).isEqualTo("SetMove(BLACK, 23, 5)")
    }

    @Test
    fun `print push moves`() {
        assertThat(PushMove(WHITE, 1, 2).toString()).isEqualTo("PushMove(WHITE, 1 -> 2)")
        assertThat(PushMove(BLACK, 22, 23, 2).toString()).isEqualTo("PushMove(BLACK, 22 -> 23, 2)")
    }

    @Test
    fun `print jump moves`() {
        assertThat(JumpMove(WHITE, 1, 20).toString()).isEqualTo("JumpMove(WHITE, 1 -> 20)")
        assertThat(JumpMove(BLACK, 22, 2, 3).toString()).isEqualTo("JumpMove(BLACK, 22 -> 2, 3)")
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "SET,WHITE,,1,1",
            "SET,BLACK,,-1,",
            "SET,BLACK,,24,",
            "SET,BLACK,,1,-1",
            "SET,BLACK,,1,-24",
            "PUSH,BLACK,1,1,",
            "PUSH,WHITE,1,2,1",
            "PUSH,BLACK,1,2,2",
            "PUSH,WHITE,6,17,",
            "PUSH,BLACK,-1,0,10",
            "PUSH,BLACK,24,0,10",
            "PUSH,BLACK,1,-1,10",
            "PUSH,BLACK,1,24,10",
            "PUSH,BLACK,1,2,-1",
            "PUSH,BLACK,1,2,24",
            "JUMP,BLACK,1,1,",
            "JUMP,WHITE,1,2,1",
            "JUMP,BLACK,1,2,2",
            "JUMP,BLACK,-1,2,3",
            "JUMP,BLACK,24,2,3",
            "JUMP,BLACK,1,-1,3",
            "JUMP,BLACK,1,24,3",
            "JUMP,BLACK,1,2,-1",
            "JUMP,BLACK,1,2,24",
        ]
    )
    fun `create illegal moves`(method: String, color: Color, from: Int?, to: Int, captured: Int?) {
        assertThatThrownBy {
            when (method) {
                "SET" -> SetMove(color, to, captured)
                "PUSH" -> PushMove(color, from!!, to, captured)
                "JUMP" -> JumpMove(color, from!!, to, captured)
                else -> throw IllegalArgumentException("Unexpected method: $method")
            }
        }.isInstanceOf(IllegalMoveException::class.java)
    }

    @Nested
    inner class AddCaptureField {
        @Test
        fun `add capture field to SetMove`() {
            val move = SetMove(WHITE, 1)

            val newMove = move.addCaptureField(2)

            assertThat(newMove).isEqualTo(SetMove(WHITE, 1, 2))
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "1",
                "-1",
                "24"
            ]
        )
        fun `add capture field to SetMove with invalid field`(captureField: Int) {
            assertThatThrownBy { SetMove(WHITE, 1).addCaptureField(captureField) }
                .isInstanceOf(IllegalMoveException::class.java)
        }

        @Test
        fun `add capture field to PushMove`() {
            val move = PushMove(BLACK, 1, 2)

            val newMove = move.addCaptureField(3)

            assertThat(newMove).isEqualTo(PushMove(BLACK, 1, 2, 3))
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "1",
                "2",
                "-1",
                "24"
            ]
        )
        fun `add capture field to PushMove with invalid field`(captureField: Int) {
            assertThatThrownBy { PushMove(WHITE, 1, 2).addCaptureField(captureField) }
                .isInstanceOf(IllegalMoveException::class.java)
        }

        @Test
        fun `add capture field to JumpMove`() {
            val move = JumpMove(WHITE, 1, 2)

            val newMove = move.addCaptureField(3)

            assertThat(newMove).isEqualTo(JumpMove(WHITE, 1, 2, 3))
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "1",
                "2",
                "-1",
                "24"
            ]
        )
        fun `add capture field to JumpMove with invalid field`(captureField: Int) {
            assertThatThrownBy { JumpMove(WHITE, 1, 2).addCaptureField(captureField) }
                .isInstanceOf(IllegalMoveException::class.java)
        }

    }
}
