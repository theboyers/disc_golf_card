package com.discgolf.card.ui.scorecard

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.discgolf.card.DiscGolfApp
import com.discgolf.card.R
import com.discgolf.card.ui.theme.AppCard
import com.discgolf.card.ui.theme.AppTopBar
import com.discgolf.card.ui.theme.DeltaText
import com.discgolf.card.ui.theme.DiscGolfCardTheme
import com.discgolf.card.ui.theme.EmptyState
import com.discgolf.card.ui.theme.HoleProgress
import com.discgolf.card.ui.theme.LoadingState
import com.discgolf.card.ui.theme.PrimaryButton
import com.discgolf.card.ui.theme.ScoreText
import com.discgolf.card.ui.theme.Space
import com.discgolf.card.ui.theme.StepperButton

@Composable
fun ScorecardScreen(
    roundId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val app = LocalContext.current.applicationContext as DiscGolfApp
    val viewModel: ScorecardViewModel = viewModel(
        factory = ScorecardViewModel.factory(app.repository, roundId)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { onFinished() }
    }

    ScorecardContent(
        state = state,
        onBack = onBack,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onAdjust = viewModel::adjust,
        onFinish = viewModel::finish
    )
}

@Composable
private fun ScorecardContent(
    state: ScorecardUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAdjust: (String, Int) -> Unit,
    onFinish: () -> Unit
) {
    val title = if (state.courseName.isBlank()) {
        stringResource(R.string.app_name)
    } else {
        state.courseName
    }

    Scaffold(
        topBar = { AppTopBar(title = title, onBack = onBack) },
        bottomBar = {
            if (!state.isLoading && !state.notFound) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = Space.lg, vertical = Space.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onPrevious,
                                enabled = state.currentIndex > 0,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(stringResource(R.string.previous))
                            }
                            PrimaryButton(
                                text = if (state.isLastHole) {
                                    stringResource(R.string.finish_round)
                                } else {
                                    stringResource(R.string.next)
                                },
                                onClick = if (state.isLastHole) onFinish else onNext,
                                loading = state.isFinishing,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.notFound -> EmptyState(
                    icon = Icons.Rounded.Warning,
                    title = stringResource(R.string.round_not_found),
                    message = stringResource(R.string.round_not_found_message),
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = Space.lg, vertical = Space.lg),
                            verticalArrangement = Arrangement.spacedBy(Space.lg)
                        ) {
                            if (state.layoutName.isNotBlank()) {
                                Text(
                                    text = state.layoutName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = stringResource(R.string.hole_number, state.holeNumber),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = stringResource(
                                        R.string.card_hole_progress,
                                        state.currentIndex + 1,
                                        state.holeCount
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HoleProgress(
                                progress = (state.currentIndex + 1).toFloat() / state.holeCount
                            )

                            AppCard {
                                state.players.forEachIndexed { index, player ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                    PlayerScoreRowView(
                                        player = player,
                                        onAdjust = onAdjust
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScoreRowView(
    player: PlayerScoreRow,
    onAdjust: (String, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Space.sm)
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    text = stringResource(R.string.total_label, player.runningTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                player.delta?.let { DeltaText(delta = it) }
            }
        }

        StepperButton(
            icon = ImageVector.vectorResource(R.drawable.ic_minus),
            contentDescription = stringResource(R.string.decrease_score, player.name),
            onClick = { onAdjust(player.id, -1) },
            enabled = player.strokes > 1
        )
        ScoreText(
            value = player.strokes,
            modifier = Modifier.widthIn(min = 48.dp)
        )
        StepperButton(
            icon = ImageVector.vectorResource(R.drawable.ic_plus),
            contentDescription = stringResource(R.string.increase_score, player.name),
            onClick = { onAdjust(player.id, 1) },
            enabled = player.strokes < 99
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScorecardPreview() {
    DiscGolfCardTheme {
        ScorecardContent(
            state = ScorecardUiState(
                isLoading = false,
                courseName = "Birch Park",
                layoutName = "Blue",
                holeNumber = 14,
                currentIndex = 4,
                holeCount = 18,
                isLastHole = false,
                players = listOf(
                    PlayerScoreRow("1", "Alex", 3, 17, null),
                    PlayerScoreRow("2", "Sam", 4, 21, null)
                )
            ),
            onBack = {},
            onPrevious = {},
            onNext = {},
            onAdjust = { _, _ -> },
            onFinish = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScorecardDarkPreview() {
    DiscGolfCardTheme(darkTheme = true) {
        ScorecardContent(
            state = ScorecardUiState(
                isLoading = false,
                courseName = "Birch Park",
                layoutName = "Blue",
                holeNumber = 13,
                currentIndex = 17,
                holeCount = 18,
                isLastHole = true,
                players = listOf(
                    PlayerScoreRow("1", "Alex", 3, 54, -1),
                    PlayerScoreRow("2", "Sam", 5, 58, 3)
                )
            ),
            onBack = {},
            onPrevious = {},
            onNext = {},
            onAdjust = { _, _ -> },
            onFinish = {}
        )
    }
}
