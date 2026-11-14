package com.discgolf.card.data

const val DEFAULT_STROKES = 3
const val MIN_STROKES = 1
const val MAX_STROKES = 99
const val MAX_PLAYERS = 8
const val MAX_HOLES = 36

/**
 * Actual hole numbers in play order.
 * 18 holes starting at 14 -> [14, 15, 16, 17, 18, 1, ..., 13].
 */
fun holeSequence(startingHole: Int, holeCount: Int): List<Int> {
    require(holeCount > 0) { "holeCount must be positive" }
    val start = startingHole.coerceIn(1, holeCount)
    return List(holeCount) { index -> (start - 1 + index) % holeCount + 1 }
}

fun totalStrokes(scores: List<HoleScoreEntity>): Int = scores.sumOf { it.strokes }

/** Sum of the holes already reached in the play order (wrap-aware). */
fun runningTotal(scores: List<HoleScoreEntity>, sequence: List<Int>, throughIndex: Int): Int {
    if (throughIndex < 0) return 0
    val played = sequence.take(throughIndex + 1).toHashSet()
    return scores.filter { it.holeNumber in played }.sumOf { it.strokes }
}

data class Standing(
    val player: PlayerWithScores,
    val total: Int,
    val rank: Int,
    val delta: Int?
)

/** Lowest total first; ties share a rank; position breaks the tie for display order. */
fun standings(players: List<PlayerWithScores>, overallPar: Int?): List<Standing> {
    val sorted = players.sortedWith(compareBy({ totalStrokes(it.scores) }, { it.player.position }))
    var previousTotal: Int? = null
    var previousRank = 0
    return sorted.mapIndexed { index, player ->
        val total = totalStrokes(player.scores)
        val rank = if (total == previousTotal) previousRank else index + 1
        previousTotal = total
        previousRank = rank
        Standing(
            player = player,
            total = total,
            rank = rank,
            delta = overallPar?.let { total - it }
        )
    }
}
