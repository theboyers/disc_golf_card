package com.discgolf.card

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.discgolf.card.data.AppDatabase
import com.discgolf.card.data.DEFAULT_STROKES
import com.discgolf.card.data.HoleScoreDto
import com.discgolf.card.data.PlayerDto
import com.discgolf.card.data.RoundConfig
import com.discgolf.card.data.RoundDao
import com.discgolf.card.data.RoundDto
import com.discgolf.card.data.RoundRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoundDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RoundDao
    private lateinit var repository: RoundRepository

    private val config = RoundConfig(
        courseName = "Birch Park",
        layoutName = "Blue",
        players = listOf("Alex", "Sam"),
        overallPar = 6,
        holeCount = 2,
        startingHole = 2
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.roundDao()
        repository = RoundRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startRoundSeedsEveryHoleWithDefaultStrokes() = runBlocking {
        val id = repository.startRound(config)
        val loaded = dao.observeRound(id).first()!!

        assertEquals(2, loaded.players.size)
        assertTrue(
            loaded.players.all { player ->
                player.scores.size == 2 && player.scores.all { it.strokes == DEFAULT_STROKES }
            }
        )
        assertEquals(2, loaded.round.startingHole)
        assertEquals("Birch Park", loaded.round.courseName)
    }

    @Test
    fun strokesStayInsideMinAndMax() = runBlocking {
        val id = repository.startRound(config)
        val playerId = dao.observeRound(id).first()!!.players.first().player.id

        repository.adjustStrokes(id, playerId, 2, -10)
        assertEquals(1, strokesFor(id, playerId, 2))

        repository.adjustStrokes(id, playerId, 2, 500)
        assertEquals(99, strokesFor(id, playerId, 2))

        repository.adjustStrokes(id, playerId, 2, -1)
        assertEquals(98, strokesFor(id, playerId, 2))
    }

    @Test
    fun importIsIdempotentAndKeepsLocalData() = runBlocking {
        val first = repository.import(listOf(exportedRound()))
        assertEquals(1, first.imported)
        assertEquals(0, first.skippedDuplicate)

        val editedPlayer = dao.observeRound("fixed-round").first()!!.players.first()
        repository.adjustStrokes("fixed-round", editedPlayer.player.id, 1, 2)

        val second = repository.import(listOf(exportedRound()))
        assertEquals(0, second.imported)
        assertEquals(1, second.skippedDuplicate)
        assertEquals(1, dao.observeHistory().first().size)
        assertEquals(
            5,
            dao.observeRound("fixed-round").first()!!
                .players.first { it.player.id == editedPlayer.player.id }
                .scores.first { it.holeNumber == 1 }
                .strokes
        )
    }

    @Test
    fun invalidRoundsAreSkipped() = runBlocking {
        val result = repository.import(listOf(exportedRound().copy(holeCount = 0)))
        assertEquals(0, result.imported)
        assertEquals(1, result.skippedInvalid)
        assertTrue(dao.observeHistory().first().isEmpty())
    }

    @Test
    fun deleteCascadesPlayersAndScores() = runBlocking {
        val id = repository.startRound(config)
        repository.finishRound(id)
        assertEquals(1, dao.observeHistory().first().size)

        repository.deleteRound(id)

        assertTrue(dao.observeHistory().first().isEmpty())
        assertEquals(0, count("players"))
        assertEquals(0, count("hole_scores"))
        assertEquals(0, count("rounds"))
    }

    private suspend fun strokesFor(roundId: String, playerId: String, hole: Int): Int =
        dao.observeRound(roundId).first()!!
            .players.first { it.player.id == playerId }
            .scores.first { it.holeNumber == hole }
            .strokes

    private fun exportedRound() = RoundDto(
        id = "fixed-round",
        courseName = "Oak Ridge",
        layoutName = "",
        startedAt = 100L,
        finishedAt = 200L,
        overallPar = null,
        holeCount = 2,
        startingHole = 1,
        players = listOf(
            PlayerDto(
                id = "file-player",
                name = "Alex",
                position = 0,
                scores = listOf(HoleScoreDto(1, 3), HoleScoreDto(2, 4))
            )
        )
    )

    private fun count(table: String): Int = db.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM $table")
        .use { cursor: Cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
