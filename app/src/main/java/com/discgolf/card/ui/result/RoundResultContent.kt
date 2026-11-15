package com.discgolf.card.ui.result

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.discgolf.card.R
import com.discgolf.card.data.RoundEntity
import com.discgolf.card.ui.formatDate
import com.discgolf.card.ui.theme.AppCard
import com.discgolf.card.ui.theme.DeltaText
import com.discgolf.card.ui.theme.DiscGolfCardTheme
import com.discgolf.card.ui.theme.ScoreTableTextStyle
import com.discgolf.card.ui.theme.ScoreTotalSmallTextStyle
import com.discgolf.card.ui.theme.ScoreTotalTextStyle
import com.discgolf.card.ui.theme.Space
import kotlin.math.roundToInt

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
            ScorecardCard(state = state, startingHole = round.startingHole)
        }

        footer?.invoke()
    }
}

private val ScorecardNameWidth = 104.dp
private val ScorecardCellWidth = 44.dp
private val ScorecardTotalWidth = 76.dp

@Composable
private fun ScorecardCard(state: ResultUiState, startingHole: Int) {
    val scrollState = rememberScrollState()
    val holeNumbers = state.holes.map { it.hole }
    val totals = state.columns.indices.map { playerIndex ->
        state.holes.sumOf { it.strokes.getOrElse(playerIndex) { 0 } }
    }

    AppCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Space.lg,
                        end = Space.lg,
                        top = Space.md,
                        bottom = Space.sm
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.scorecard),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (startingHole != 1) {
                    Text(
                        text = stringResource(R.string.scorecard_in_play_order),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            ScorecardHeader(holeNumbers = holeNumbers, scrollState = scrollState)
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            state.columns.forEachIndexed { playerIndex, name ->
                ScorecardPlayerRow(
                    name = name,
                    values = state.holes.map { it.strokes.getOrElse(playerIndex) { 0 } },
                    total = totals[playerIndex],
                    scrollState = scrollState
                )
                if (playerIndex != state.columns.lastIndex) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            if (scrollState.maxValue > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.sm, bottom = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(ScorecardNameWidth))
                    ScorecardScrollbar(
                        scrollState = scrollState,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(ScorecardTotalWidth))
                }
            }
        }
    }
}

@Composable
private fun ScorecardScrollbar(scrollState: ScrollState, modifier: Modifier = Modifier) {
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val barHeight = 4.dp

    Box(
        modifier = modifier
            .height(barHeight)
            .onSizeChanged { trackWidthPx = it.width }
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (trackWidthPx > 0 && scrollState.maxValue > 0) {
            val contentWidthPx = trackWidthPx + scrollState.maxValue
            val thumbWidthPx = (trackWidthPx.toFloat() * trackWidthPx / contentWidthPx)
                .coerceAtLeast(with(density) { 24.dp.toPx() })
            val maxOffsetPx = (trackWidthPx - thumbWidthPx).coerceAtLeast(0f)
            val fraction = scrollState.value.toFloat() / scrollState.maxValue
            Box(
                modifier = Modifier
                    .offset { IntOffset((maxOffsetPx * fraction).roundToInt(), 0) }
                    .width(with(density) { thumbWidthPx.toDp() })
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun ScorecardHeader(holeNumbers: List<Int>, scrollState: ScrollState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScorecardLabel(
            text = stringResource(R.string.scorecard_player),
            width = ScorecardNameWidth,
            startPadding = Space.lg,
            endPadding = Space.sm,
            contentAlignment = Alignment.CenterStart
        )
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                holeNumbers.forEach { hole ->
                    Box(
                        modifier = Modifier
                            .width(ScorecardCellWidth)
                            .padding(vertical = Space.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hole.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        ScorecardLabel(
            text = stringResource(R.string.scorecard_total),
            width = ScorecardTotalWidth,
            endPadding = Space.lg,
            contentAlignment = Alignment.CenterEnd
        )
    }
}

@Composable
private fun ScorecardPlayerRow(
    name: String,
    values: List<Int>,
    total: Int,
    scrollState: ScrollState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(ScorecardNameWidth)
                .padding(
                    start = Space.lg,
                    end = Space.sm,
                    top = Space.md,
                    bottom = Space.md
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                values.forEach { value ->
                    Box(
                        modifier = Modifier
                            .width(ScorecardCellWidth)
                            .padding(vertical = Space.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value.toString(),
                            style = ScoreTableTextStyle,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .width(ScorecardTotalWidth)
                .padding(
                    end = Space.lg,
                    top = Space.md,
                    bottom = Space.md
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = total.toString(),
                style = ScoreTotalSmallTextStyle,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScorecardLabel(
    text: String,
    width: Dp,
    contentAlignment: Alignment,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(
                start = startPadding,
                end = endPadding,
                top = Space.sm,
                bottom = Space.sm
            ),
        contentAlignment = contentAlignment
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

private fun previewHoles(): List<ResultHoleRow> {
    val alex = mapOf(15 to 2, 17 to 2)
    val sam = mapOf(14 to 4, 16 to 4, 18 to 4)
    return ((14..18) + (1..13)).map { hole ->
        ResultHoleRow(
            hole = hole,
            strokes = listOf(alex[hole] ?: 3, sam[hole] ?: 3)
        )
    }
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
    holes = previewHoles(),
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
        RoundResultContent(state = previewState, showHoleTable = true)
    }
}
