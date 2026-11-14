package com.discgolf.card.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.discgolf.card.DiscGolfApp
import com.discgolf.card.R
import com.discgolf.card.ui.theme.AppTopBar
import com.discgolf.card.ui.theme.EmptyState
import com.discgolf.card.ui.theme.LoadingState
import com.discgolf.card.ui.theme.PrimaryButton
import com.discgolf.card.ui.theme.Space
import com.discgolf.card.ui.theme.appColors

@Composable
fun SummaryScreen(
    roundId: String,
    onDone: () -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as DiscGolfApp
    val viewModel: RoundResultViewModel = viewModel(
        factory = RoundResultViewModel.factory(app.repository, roundId)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.summary_title),
                onBack = onBack,
                actions = { ShareRoundAction(state.shareText) }
            )
        },
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
                            TextButton(
                                onClick = onViewHistory,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(stringResource(R.string.view_history))
                            }
                            PrimaryButton(
                                text = stringResource(R.string.done),
                                onClick = onDone,
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

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    RoundResultContent(
                        state = state,
                        showHoleTable = false,
                        footer = { SavedNotice() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedNotice() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.appColors.success,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.saved_to_history),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
