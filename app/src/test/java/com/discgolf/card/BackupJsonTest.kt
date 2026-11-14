package com.discgolf.card

import com.discgolf.card.data.BackupJson
import com.discgolf.card.data.HoleScoreDto
import com.discgolf.card.data.PlayerDto
import com.discgolf.card.data.RoundDto
import com.discgolf.card.data.isValid
import com.discgolf.card.data.normalizedForImport
import com.discgolf.card.data.toEntities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupJsonTest {

    private fun round(
        id: String = "round-1",
        holeCount: Int = 2,
        startingHole: Int = 1,
        players: List<PlayerDto> = listOf(
            PlayerDto("p1", "Alex", 0, listOf(HoleScoreDto(1, 3), HoleScoreDto(2, 4))),
            PlayerDto("p2", "Sam", 1, listOf(HoleScoreDto(1, 3), HoleScoreDto(2, 3)))
        )
    ) = RoundDto(
        id = id,
        courseName = "Birch Park",
        layoutName = "Blue",
        startedAt = 1_000L,
        finishedAt = 2_000L,
        overallPar = 6,
        holeCount = holeCount,
        startingHole = startingHole,
        players = players
    )

    @Test
    fun `round trip preserves every field`() {
        val original = listOf(round())
        val decoded = BackupJson.decode(BackupJson.encode(original, exportedAt = 5L))

        assertEquals(1, decoded.schemaVersion)
        assertEquals(5L, decoded.exportedAt)
        assertEquals(1, decoded.rounds.size)
        val restored = decoded.rounds.single()
        assertEquals(original.single(), restored)
        assertTrue(restored.isValid())
    }

    @Test
    fun `unknown keys are ignored and missing optional fields default`() {
        val json = """
            {
              "schemaVersion": 1,
              "exportedAt": 1,
              "futureField": "ignored",
              "rounds": [
                {
                  "id": "r1",
                  "courseName": "Oak Ridge",
                  "startedAt": 10,
                  "holeCount": 1,
                  "players": [
                    {"id": "p", "name": "Alex", "scores": [{"hole": 1, "strokes": 3}]}
                  ]
                }
              ]
            }
        """.trimIndent()

        val decoded = BackupJson.decode(json)
        val restored = decoded.rounds.single()
        assertEquals("", restored.layoutName)
        assertEquals(1, restored.startingHole)
        assertEquals(null, restored.overallPar)
        assertTrue(restored.isValid())
    }

    @Test
    fun `future schema version is rejected`() {
        val json = """{"schemaVersion": 2, "exportedAt": 1, "rounds": []}"""
        try {
            BackupJson.decode(json)
            fail("Expected unsupported version failure")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Unsupported backup version"))
        }
    }

    @Test
    fun `validation rejects malformed rounds`() {
        assertFalse(round(holeCount = 0).isValid())
        assertFalse(round(startingHole = 3).isValid())
        assertFalse(round(players = emptyList()).isValid())
        assertFalse(
            round(
                players = listOf(
                    PlayerDto("p", "Alex", 0, listOf(HoleScoreDto(1, 3)))
                )
            ).isValid()
        )
        assertFalse(
            round(
                players = listOf(
                    PlayerDto("p", "Alex", 0, listOf(HoleScoreDto(1, 0), HoleScoreDto(2, 3)))
                )
            ).isValid()
        )
        assertFalse(
            round(
                players = listOf(
                    PlayerDto("p", "Alex", 0, listOf(HoleScoreDto(1, 3), HoleScoreDto(1, 3)))
                )
            ).isValid()
        )
        assertFalse(
            round(
                players = listOf(
                    PlayerDto("p", "Alex", 0, listOf(HoleScoreDto(1, 3), HoleScoreDto(2, 100)))
                )
            ).isValid()
        )
    }

    @Test
    fun `draft exports become completed rounds using start time`() {
        val draft = round().copy(finishedAt = null)
        assertEquals(1_000L, draft.normalizedForImport().finishedAt)
    }

    @Test
    fun `entities keep round id and generate local player ids`() {
        val (entity, players, scores) = round().toEntities()

        assertEquals("round-1", entity.id)
        assertEquals(2, players.size)
        assertEquals(4, scores.size)
        assertEquals(players.map { it.id }.toSet(), scores.map { it.playerId }.toSet())
        assertTrue(players.all { it.roundId == entity.id })
        assertNotNull(players.first().id)
    }
}
