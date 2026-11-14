package com.discgolf.card.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

const val SCHEMA_VERSION = 1

@Serializable
data class BackupFile(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: Long = 0L,
    val rounds: List<RoundDto> = emptyList()
)

@Serializable
data class RoundDto(
    val id: String,
    val courseName: String,
    val layoutName: String = "",
    val startedAt: Long,
    val finishedAt: Long? = null,
    val overallPar: Int? = null,
    val holeCount: Int,
    val startingHole: Int = 1,
    val players: List<PlayerDto> = emptyList()
)

@Serializable
data class PlayerDto(
    val id: String,
    val name: String,
    val position: Int = 0,
    val scores: List<HoleScoreDto> = emptyList()
)

@Serializable
data class HoleScoreDto(
    val hole: Int,
    val strokes: Int
)

object BackupJson {
    private val format = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        isLenient = false
    }

    fun encode(rounds: List<RoundDto>, exportedAt: Long = System.currentTimeMillis()): String =
        format.encodeToString(BackupFile(exportedAt = exportedAt, rounds = rounds))

    fun decode(text: String): BackupFile {
        val file = format.decodeFromString<BackupFile>(text)
        require(file.schemaVersion == SCHEMA_VERSION) {
            "Unsupported backup version ${file.schemaVersion}"
        }
        return file
    }
}

fun RoundWithPlayers.toDto(): RoundDto = RoundDto(
    id = round.id,
    courseName = round.courseName,
    layoutName = round.layoutName,
    startedAt = round.startedAt,
    finishedAt = round.finishedAt,
    overallPar = round.overallPar,
    holeCount = round.holeCount,
    startingHole = round.startingHole,
    players = players.map { playerWithScores ->
        PlayerDto(
            id = playerWithScores.player.id,
            name = playerWithScores.player.name,
            position = playerWithScores.player.position,
            scores = playerWithScores.scores
                .sortedBy { it.holeNumber }
                .map { HoleScoreDto(hole = it.holeNumber, strokes = it.strokes) }
        )
    }
)

/** Missing finish timestamps from older exports become completed rounds at start time. */
fun RoundDto.normalizedForImport(): RoundDto = copy(
    courseName = courseName.trim(),
    layoutName = layoutName.trim(),
    finishedAt = finishedAt ?: startedAt
)

fun RoundDto.isValid(): Boolean {
    if (id.isBlank()) return false
    if (courseName.isBlank()) return false
    if (holeCount !in 1..MAX_HOLES) return false
    if (startingHole !in 1..holeCount) return false
    if (players.size !in 1..MAX_PLAYERS) return false
    if (overallPar != null && overallPar !in 1..999) return false
    if (startedAt < 0L) return false
    if (players.map { it.id }.toSet().size != players.size) return false

    return players.all { player ->
        player.id.isNotBlank() &&
            player.name.isNotBlank() &&
            player.scores.size == holeCount &&
            player.scores.map { it.hole }.toSet().size == holeCount &&
            player.scores.all { it.hole in 1..holeCount && it.strokes in MIN_STROKES..MAX_STROKES }
    }
}

/**
 * Player IDs are local-only (dedupe is by round ID), so they are regenerated on import to make
 * malformed or merged files harmless. The round ID is preserved.
 */
fun RoundDto.toEntities(): Triple<RoundEntity, List<PlayerEntity>, List<HoleScoreEntity>> {
    val round = RoundEntity(
        id = id,
        courseName = courseName,
        layoutName = layoutName,
        startedAt = startedAt,
        finishedAt = finishedAt,
        overallPar = overallPar,
        holeCount = holeCount,
        startingHole = startingHole
    )
    val playerEntities = players.mapIndexed { index, player ->
        PlayerEntity(
            id = UUID.randomUUID().toString(),
            roundId = round.id,
            name = player.name,
            position = index
        )
    }
    val scores = players.flatMapIndexed { index, player ->
        val playerId = playerEntities[index].id
        player.scores.map { score ->
            HoleScoreEntity(
                roundId = round.id,
                playerId = playerId,
                holeNumber = score.hole,
                strokes = score.strokes
            )
        }
    }
    return Triple(round, playerEntities, scores)
}
