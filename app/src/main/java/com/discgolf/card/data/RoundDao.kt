package com.discgolf.card.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RoundDao {

    @Transaction
    @Query("SELECT * FROM rounds WHERE id = :id")
    abstract fun observeRound(id: String): Flow<RoundWithPlayers?>

    @Transaction
    @Query("SELECT * FROM rounds WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    abstract fun observeHistory(): Flow<List<RoundWithPlayers>>

    @Transaction
    @Query("SELECT * FROM rounds WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    abstract suspend fun getFinishedRounds(): List<RoundWithPlayers>

    @Query("SELECT * FROM rounds WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    abstract fun observeDraft(): Flow<RoundEntity?>

    @Insert
    abstract suspend fun insertRound(round: RoundEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRoundIgnore(round: RoundEntity): Long

    @Insert
    abstract suspend fun insertPlayers(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertScoresIgnore(scores: List<HoleScoreEntity>)

    @Query("UPDATE rounds SET finishedAt = :finishedAt WHERE id = :id AND finishedAt IS NULL")
    abstract suspend fun markFinished(id: String, finishedAt: Long)

    @Query("UPDATE rounds SET lastHoleIndex = :index WHERE id = :id")
    abstract suspend fun updateLastHole(id: String, index: Int)

    @Query(
        """
        UPDATE hole_scores
        SET strokes = max(1, min(99, strokes + :delta))
        WHERE roundId = :roundId AND playerId = :playerId AND holeNumber = :hole
        """
    )
    abstract suspend fun adjustStrokes(roundId: String, playerId: String, hole: Int, delta: Int)

    @Query("DELETE FROM rounds WHERE id = :id")
    abstract suspend fun deleteRound(id: String)

    @Query("DELETE FROM rounds WHERE finishedAt IS NULL")
    abstract suspend fun deleteDrafts()

    @Transaction
    open suspend fun createRound(
        round: RoundEntity,
        players: List<PlayerEntity>,
        scores: List<HoleScoreEntity>
    ) {
        deleteDrafts()
        insertRound(round)
        insertPlayers(players)
        insertScoresIgnore(scores)
    }

    @Transaction
    open suspend fun importRound(
        round: RoundEntity,
        players: List<PlayerEntity>,
        scores: List<HoleScoreEntity>
    ): Boolean {
        if (insertRoundIgnore(round) == -1L) return false
        insertPlayers(players)
        insertScoresIgnore(scores)
        return true
    }
}
