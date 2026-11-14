package com.discgolf.card

import com.discgolf.card.data.HoleScoreEntity
import com.discgolf.card.data.PlayerEntity
import com.discgolf.card.data.PlayerWithScores
import com.discgolf.card.data.holeSequence
import com.discgolf.card.data.runningTotal
import com.discgolf.card.data.standings
import com.discgolf.card.data.totalStrokes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoringTest {

    private fun scores(playerId: String, values: List<Int>): List<HoleScoreEntity> =
        values.mapIndexed { index, strokes ->
            HoleScoreEntity("round", playerId, index + 1, strokes)
        }

    private fun player(
        id: String,
        name: String,
        position: Int,
        values: List<Int>
    ): PlayerWithScores = PlayerWithScores(
        player = PlayerEntity(id, "round", name, position),
        scores = scores(id, values)
    )

    @Test
    fun `starting at 14 wraps through 18 then 1`() {
        assertEquals(
            listOf(14, 15, 16, 17, 18, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
            holeSequence(14, 18)
        )
    }

    @Test
    fun `starting hole defaults and clamps`() {
        assertEquals((1..18).toList(), holeSequence(1, 18))
        assertEquals(listOf(18) + (1..17).toList(), holeSequence(18, 18))
        assertEquals(listOf(3, 1, 2), holeSequence(18, 3))
        assertEquals(listOf(1), holeSequence(0, 1))
        assertEquals(listOf(5, 1, 2, 3, 4), holeSequence(99, 5))
    }

    @Test
    fun `running total counts only reached holes in play order`() {
        val sequence = holeSequence(14, 18)
        val holes = (1..18).map { HoleScoreEntity("round", "p", it, 3) }

        assertEquals(3, runningTotal(holes, sequence, 0))
        assertEquals(15, runningTotal(holes, sequence, 4))
        assertEquals(18, runningTotal(holes, sequence, 5))
        assertEquals(54, runningTotal(holes, sequence, 17))
        assertEquals(0, runningTotal(holes, sequence, -1))
    }

    @Test
    fun `standings sort ascending and share ranks on ties`() {
        val result = standings(
            listOf(
                player("a", "Alex", 0, List(18) { 3 }),      // 54
                player("b", "Sam", 1, List(18) { 3 }),       // 54
                player("c", "Jo", 2, List(18) { 4 })         // 72
            ),
            overallPar = 54
        )

        assertEquals(listOf("Alex", "Sam", "Jo"), result.map { it.player.player.name })
        assertEquals(listOf(1, 1, 3), result.map { it.rank })
        assertEquals(listOf(0, 0, 18), result.map { it.delta })
    }

    @Test
    fun `no par means no delta`() {
        val result = standings(listOf(player("a", "Alex", 0, List(9) { 3 })), overallPar = null)
        assertNull(result.single().delta)
        assertEquals(27, result.single().total)
    }

    @Test
    fun `total strokes sums every hole`() {
        assertEquals(27, totalStrokes(scores("p", List(9) { 3 })))
    }
}
