package com.discgolf.card.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

data class RoundConfig(
    val courseName: String,
    val layoutName: String,
    val players: List<String>,
    val overallPar: Int?,
    val holeCount: Int,
    val startingHole: Int
)

data class ImportResult(
    val imported: Int = 0,
    val skippedDuplicate: Int = 0,
    val skippedInvalid: Int = 0
)

class RoundRepository(private val dao: RoundDao) {

    fun observeRound(id: String): Flow<RoundWithPlayers?> = dao.observeRound(id)

    fun observeHistory(): Flow<List<RoundWithPlayers>> = dao.observeHistory()

    fun observeDraft(): Flow<RoundEntity?> = dao.observeDraft()

    suspend fun startRound(config: RoundConfig): String = withContext(Dispatchers.IO) {
        require(config.players.isNotEmpty()) { "At least one player is required" }
        require(config.players.size <= MAX_PLAYERS) { "At most $MAX_PLAYERS players" }
        require(config.holeCount in 1..MAX_HOLES) { "Invalid hole count" }

        val roundId = UUID.randomUUID().toString()
        val startingHole = config.startingHole.coerceIn(1, config.holeCount)
        val round = RoundEntity(
            id = roundId,
            courseName = config.courseName.trim(),
            layoutName = config.layoutName.trim(),
            startedAt = System.currentTimeMillis(),
            finishedAt = null,
            overallPar = config.overallPar,
            holeCount = config.holeCount,
            startingHole = startingHole
        )
        val players = config.players.mapIndexed { index, name ->
            PlayerEntity(
                id = UUID.randomUUID().toString(),
                roundId = roundId,
                name = name.trim(),
                position = index
            )
        }
        val scores = players.flatMap { player ->
            (1..config.holeCount).map { hole ->
                HoleScoreEntity(
                    roundId = roundId,
                    playerId = player.id,
                    holeNumber = hole,
                    strokes = DEFAULT_STROKES
                )
            }
        }
        dao.createRound(round, players, scores)
        roundId
    }

    suspend fun adjustStrokes(roundId: String, playerId: String, hole: Int, delta: Int) =
        withContext(Dispatchers.IO) {
            dao.adjustStrokes(roundId, playerId, hole, delta)
        }

    suspend fun finishRound(roundId: String, finishedAt: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            dao.markFinished(roundId, finishedAt)
        }

    suspend fun updateLastHole(roundId: String, index: Int) = withContext(Dispatchers.IO) {
        dao.updateLastHole(roundId, index)
    }

    suspend fun discardDraft(roundId: String) = withContext(Dispatchers.IO) {
        dao.deleteRound(roundId)
    }

    suspend fun deleteRound(roundId: String) = withContext(Dispatchers.IO) {
        dao.deleteRound(roundId)
    }

    suspend fun exportAll(): List<RoundDto> = withContext(Dispatchers.IO) {
        dao.getFinishedRounds().map { it.toDto() }
    }

    suspend fun import(rounds: List<RoundDto>): ImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var duplicates = 0
        var invalid = 0

        rounds.forEach { original ->
            val dto = original.normalizedForImport()
            if (!dto.isValid()) {
                invalid++
                return@forEach
            }
            val (round, players, scores) = dto.toEntities()
            if (dao.importRound(round, players, scores)) imported++ else duplicates++
        }
        ImportResult(imported, duplicates, invalid)
    }
}
