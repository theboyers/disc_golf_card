package com.discgolf.card.ui.result

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.discgolf.card.R
import com.discgolf.card.data.RoundEntity
import com.discgolf.card.ui.formatDate
import com.discgolf.card.ui.theme.AppCard
import com.discgolf.card.ui.theme.DeltaText
import com.discgolf.card.ui.theme.DiscGolfCardTheme
import com.discgolf.card.ui.theme.ScoreTableTextStyle
import com.discgolf.card.ui.theme.ScoreTotalTextStyle
import com.discgolf.card.ui.theme.Space

@Composable
fun ShareRoundAction(shareText: String) {
    if (shareText.isBlank()) return
    val context = LocalContext.current
    IconButton(
        onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(send, null))
        }
    ) {
        Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = stringResource(R.string.share_round)
        )
    }
}

@Composable
fun RoundResultContent(
    state: ResultUiState,
    showHoleTable: Boolean,
    footer: (@Composable () -> Unit)? = null
) {
    val round = state.round ?: return

    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg, vertical = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        val dateText = formatDate(round.startedAt)
        val holesText = pluralStringResource(
            R.plurals.holes_meta,
            round.holeCount,
            round.holeCount
        )
        val parText = if (round.overallPar != null) {
            stringResource(R.string.par_label, round.overallPar)
        } else {
            null
        }
        val meta = listOfNotNull(dateText, holesText, parText).joinToString(" · ")

        AppCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    text = round.courseName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (round.layoutName.isNotBlank()) {
                    Text(
                        text = round.layoutName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (round.startingHole != 1) {
                    Text(
                        text = stringResource(R.string.started_hole, round.startingHole),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AppCard {
            state.players.forEachIndexed { index, player ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.lg, vertical = Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = player.rank.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(32.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Space.sm),
                        verticalArrangement = Arrangement.spacedBy(Space.xs)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (player.rank == 1) {
                            WinnerBadge()
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = player.total.toString(),
                            style = ScoreTotalTextStyle,
                            maxLines = 1
                        )
                        player.delta?.let { DeltaText(delta = it) }
                    }
                }
            }
        }

        if (showHoleTable) {
            AppCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Space.sm)
                ) {
                    Text(
                        text = stringResource(R.string.hole_by_hole),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm)
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Space.lg)
                    ) {
                        Column {
                            Row {
                                TableCell(stringResource(R.string.hole_table_hole), 56.dp, header = true)
                                state.columns.forEach { name ->
                                    TableCell(name, 72.dp, header = true)
                                }
                            }
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            state.holes.forEach { hole ->
                                Row {
                                    TableCell(hole.hole.toString(), 56.dp)
                                    hole.strokes.forEach { strokes ->
                                        TableCell(strokes.toString(), 72.dp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Space.sm))
                }
            }
        }

        footer?.invoke()
    }
}

@Composable
private fun WinnerBadge() {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = stringResource(R.string.winner),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs)
        )
    }
}

@Composable
private fun TableCell(text: String, width: Dp, header: Boolean = false) {
    Text(
        text = text,
        style = if (header) MaterialTheme.typography.labelSmall else ScoreTableTextStyle,
        color = if (header) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(width)
            .padding(vertical = Space.sm)
    )
}

private val previewState = ResultUiState(
    isLoading = false,
    round = RoundEntity(
        id = "preview",
        courseName = "Birch Park",
        layoutName = "Blue",
        startedAt = 1_760_000_000_000,
        finishedAt = 1_760_003_600_000,
        overallPar = 54,
        holeCount = 18,
        startingHole = 14
    ),
    players = listOf(
        ResultPlayerRow("Alex", 52, 1, -2),
        ResultPlayerRow("Sam", 57, 2, 3)
    ),
    columns = listOf("Alex", "Sam"),
    holes = listOf(
        ResultHoleRow(14, listOf(3, 3)),
        ResultHoleRow(15, listOf(2, 4)),
        ResultHoleRow(16, listOf(3, 3)),
        ResultHoleRow(17, listOf(4, 3)),
        ResultHoleRow(18, listOf(3, 3))
    ),
    shareText = "preview"
)

@Preview(showBackground = true)
@Composable
private fun ResultPreview() {
    DiscGolfCardTheme {
        RoundResultContent(state = previewState, showHoleTable = true)
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResultDarkPreview() {
    DiscGolfCardTheme(darkTheme = true) {
        RoundResultContent(state = previewState, showHoleTable = false)
    }
}
