package de.praxisit.mule.server

import de.praxisit.mule.Black
import de.praxisit.mule.Color
import de.praxisit.mule.FieldIndex
import de.praxisit.mule.GameResult.Ongoing
import de.praxisit.mule.GameResult.Remis
import de.praxisit.mule.GameResult.Win
import de.praxisit.mule.GameState
import de.praxisit.mule.GameState.Companion.MOVES_WITHOUT_CAPTURE_FOR_REMIS
import de.praxisit.mule.JumpMove
import de.praxisit.mule.Move
import de.praxisit.mule.Phase
import de.praxisit.mule.Player
import de.praxisit.mule.PushMove
import de.praxisit.mule.SetMove
import de.praxisit.mule.White
import kotlinx.serialization.Serializable

@Serializable
enum class ColorDto { WHITE, BLACK }

@Serializable
enum class MoveType { SET, PUSH, JUMP }

@Serializable
enum class ResultStatus { ONGOING, WIN, REMIS }

@Serializable
enum class ResultReason { TWO_STONES, BLOCKED, REPETITION, NO_CAPTURE }

@Serializable
data class NewGameRequest(val humanColor: ColorDto = ColorDto.WHITE, val strength: Strength = Strength.MEDIUM)

/** A move; the fields are numbered 0 to 23 like the board of the engine. */
@Serializable
data class MoveDto(
    val type: MoveType,
    val color: ColorDto,
    val to: Int,
    val from: Int? = null,
    val capture: Int? = null
)

@Serializable
data class PlayerDto(
    val color: ColorDto,
    val phase: Phase,
    val stonesInHand: Int,
    val stonesOnBoard: Int,
    val stonesLost: Int
)

@Serializable
data class ResultDto(val status: ResultStatus, val winner: ColorDto? = null, val reason: ResultReason? = null)

@Serializable
data class GameDto(
    val id: String,
    val humanColor: ColorDto,
    val strength: Strength,
    /** The 24 fields, null for an empty field. */
    val board: List<ColorDto?>,
    val activeColor: ColorDto,
    val white: PlayerDto,
    val black: PlayerDto,
    /** The legal moves of the active player, empty when the game is over. */
    val legalMoves: List<MoveDto>,
    val moves: List<MoveDto>,
    val movesWithoutCapture: Int,
    val result: ResultDto
)

fun Color.toDto() = if (this == White) ColorDto.WHITE else ColorDto.BLACK

fun ColorDto.toColor() = if (this == ColorDto.WHITE) White else Black

fun Move.toDto() = when (this) {
    is SetMove  -> MoveDto(MoveType.SET, color.toDto(), toField.index, capture = capturedField?.index)
    is PushMove -> MoveDto(MoveType.PUSH, color.toDto(), toField.index, fromField.index, capturedField?.index)
    is JumpMove -> MoveDto(MoveType.JUMP, color.toDto(), toField.index, fromField.index, capturedField?.index)
}

fun MoveDto.toMove(): Move {
    val color = color.toColor()
    val to = FieldIndex(to)
    val capture = capture?.let { FieldIndex(it) }
    return when (type) {
        MoveType.SET  -> SetMove(color, to, capture)
        MoveType.PUSH -> PushMove(color, FieldIndex(requireNotNull(from) { "A push needs a from field" }), to, capture)
        MoveType.JUMP -> JumpMove(color, FieldIndex(requireNotNull(from) { "A jump needs a from field" }), to, capture)
    }
}

fun GameState.toDto(id: String, humanColor: Color, strength: Strength, moves: List<Move>) = GameDto(
    id = id,
    humanColor = humanColor.toDto(),
    strength = strength,
    board = FieldIndex.INDEXES.map { field -> (board.getStone(field) as? Color)?.toDto() },
    activeColor = activeColor.toDto(),
    white = playerDto(position.white),
    black = playerDto(position.black),
    legalMoves = if (result == Ongoing) legalMoves.map { it.toDto() } else emptyList(),
    moves = moves.map { it.toDto() },
    movesWithoutCapture = movesWithoutCapture,
    result = resultDto()
)

private fun GameState.playerDto(player: Player): PlayerDto {
    val stonesOnBoard = Integer.bitCount(board.stones(player.color))
    return PlayerDto(
        color = player.color.toDto(),
        phase = player.phase,
        stonesInHand = player.remainingStones,
        stonesOnBoard = stonesOnBoard,
        stonesLost = Player.STONES - player.stones
    )
}

private fun GameState.resultDto() = when (val result = result) {
    Ongoing -> ResultDto(ResultStatus.ONGOING)
    Remis   -> {
        val reason = if (movesWithoutCapture >= MOVES_WITHOUT_CAPTURE_FOR_REMIS) {
            ResultReason.NO_CAPTURE
        } else {
            ResultReason.REPETITION
        }
        ResultDto(ResultStatus.REMIS, reason = reason)
    }
    is Win  -> {
        val loser = position.player(result.winner.opposite)
        val reason = if (loser.phase == Phase.LOST) ResultReason.TWO_STONES else ResultReason.BLOCKED
        ResultDto(ResultStatus.WIN, result.winner.toDto(), reason)
    }
}
