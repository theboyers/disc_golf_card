package com.discgolf.card.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey val id: String,
    val courseName: String,
    val layoutName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val overallPar: Int?,
    val holeCount: Int,
    val startingHole: Int,
    val lastHoleIndex: Int = 0
)

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roundId")]
)
data class PlayerEntity(
    @PrimaryKey val id: String,
    val roundId: String,
    val name: String,
    val position: Int
)

@Entity(
    tableName = "hole_scores",
    primaryKeys = ["roundId", "playerId", "holeNumber"],
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playerId")]
)
data class HoleScoreEntity(
    val roundId: String,
    val playerId: String,
    val holeNumber: Int,
    val strokes: Int
)

data class PlayerWithScores(
    @Embedded val player: PlayerEntity,
    @Relation(
        entity = HoleScoreEntity::class,
        parentColumn = "id",
        entityColumn = "playerId"
    )
    val scores: List<HoleScoreEntity>
)

data class RoundWithPlayers(
    @Embedded val round: RoundEntity,
    @Relation(
        entity = PlayerEntity::class,
        parentColumn = "id",
        entityColumn = "roundId"
    )
    val players: List<PlayerWithScores>
)
