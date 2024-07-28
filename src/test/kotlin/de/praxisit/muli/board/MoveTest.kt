package de.praxisit.muli.board

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MoveTest {
    @Test
    fun `print set moves`() {
        assertThat(SetMove(White, 1).toString()).isEqualTo("SetMove(White, 1)")
        assertThat(SetMove(Black, 23, 5).toString()).isEqualTo("SetMove(Black, 23, 5)")
    }

    @Test
    fun `print push moves`() {
        assertThat(PushMove(White, 1, 2).toString()).isEqualTo("PushMove(White, 1 -> 2)")
        assertThat(PushMove(Black, 22, 23, 2).toString()).isEqualTo("PushMove(Black, 22 -> 23, 2)")
    }

    @Test
    fun `print jump moves`() {
        assertThat(JumpMove(White, 1, 20).toString()).isEqualTo("JumpMove(White, 1 -> 20)")
        assertThat(JumpMove(Black, 22, 2, 3).toString()).isEqualTo("JumpMove(Black, 22 -> 2, 3)")
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "SET,White,,1,1",
            "SET,Black,,-1,",
            "SET,Black,,24,",
            "SET,Black,,1,-1",
            "SET,Black,,1,-24",
            "PUSH,Black,1,1,",
            "PUSH,White,1,2,1",
            "PUSH,Black,1,2,2",
            "PUSH,White,6,17,",
            "PUSH,Black,-1,0,10",
            "PUSH,Black,24,0,10",
            "PUSH,Black,1,-1,10",
            "PUSH,Black,1,24,10",
            "PUSH,Black,1,2,-1",
            "PUSH,Black,1,2,24",
            "JUMP,Black,1,1,",
            "JUMP,White,1,2,1",
            "JUMP,Black,1,2,2",
            "JUMP,Black,-1,2,3",
            "JUMP,Black,24,2,3",
            "JUMP,Black,1,-1,3",
            "JUMP,Black,1,24,3",
            "JUMP,Black,1,2,-1",
            "JUMP,Black,1,2,24",
        ]
    )
    fun `create illegal moves`(method: String, colorName: String, from: Int?, to: Int, captured: Int?) {
        val color = if (colorName == "White") White else Black
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
            val move = SetMove(White, 1)

            val newMove = move.addCaptureField(2)

            assertThat(newMove).isEqualTo(SetMove(White, 1, 2))
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
            assertThatThrownBy { SetMove(White, 1).addCaptureField(captureField) }
                .isInstanceOf(IllegalMoveException::class.java)
        }

        @Test
        fun `add capture field to PushMove`() {
            val move = PushMove(Black, 1, 2)

            val newMove = move.addCaptureField(3)

            assertThat(newMove).isEqualTo(PushMove(Black, 1, 2, 3))
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
            assertThatThrownBy { PushMove(White, 1, 2).addCaptureField(captureField) }
                .isInstanceOf(IllegalMoveException::class.java)
        }

        @Test
        fun `add capture field to JumpMove`() {
            val move = JumpMove(White, 1, 2)

            val newMove = move.addCaptureField(3)

            assertThat(newMove).isEqualTo(JumpMove(White, 1, 2, 3))
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
            assertThatThrownBy { JumpMove(White, 1, 2).addCaptureField(captureField) }
                .isInstanceOf(IllegalMoveException::class.java)
        }

    }
}
