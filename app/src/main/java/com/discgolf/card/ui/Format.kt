package com.discgolf.card.ui

import android.content.res.Resources
import com.discgolf.card.R
import com.discgolf.card.data.ImportResult
import com.discgolf.card.data.RoundEntity
import com.discgolf.card.data.Standing
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatDelta(delta: Int): String = when {
    delta > 0 -> "+$delta"
    delta < 0 -> delta.toString()
    else -> "E"
}

fun backupFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.US).format(LocalDateTime.now())
    return "disc-golf-rounds-$stamp.json"
}

fun shareText(round: RoundEntity, standings: List<Standing>): String = buildString {
    append("Disc Golf — ").append(round.courseName)
    if (round.layoutName.isNotBlank()) append(" (").append(round.layoutName).append(")")
    append('\n')
    append(formatDate(round.startedAt)).append(" · ").append(round.holeCount).append(" holes")
    round.overallPar?.let { append(" · Par ").append(it) }
    append('\n')
    standings.forEach { standing ->
        append(standing.rank).append(". ").append(standing.player.player.name)
        append(" — ").append(standing.total)
        standing.delta?.let { append(" (").append(formatDelta(it)).append(")") }
        append('\n')
    }
    append("Scored with Disc Golf Card")
}

/** Formats a user-facing summary for a completed backup import. */
fun importResultMessage(resources: Resources, result: ImportResult): String {
    if (result.imported == 0 && result.skippedDuplicate == 0) {
        return if (result.skippedInvalid > 0) {
            resources.getQuantityString(
                R.plurals.skipped_invalid,
                result.skippedInvalid,
                result.skippedInvalid
            )
        } else {
            resources.getString(R.string.import_failed)
        }
    }
    return buildString {
        append(
            resources.getQuantityString(
                R.plurals.imported_rounds,
                result.skippedDuplicate,
                result.imported,
                result.skippedDuplicate
            )
        )
        if (result.skippedInvalid > 0) {
            append(" · ")
            append(
                resources.getQuantityString(
                    R.plurals.skipped_invalid,
                    result.skippedInvalid,
                    result.skippedInvalid
                )
            )
        }
    }
}
