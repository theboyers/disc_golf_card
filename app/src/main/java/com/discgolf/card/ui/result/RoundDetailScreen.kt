package com.discgolf.card.ui.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.discgolf.card.DiscGolfApp
import com.discgolf.card.R
import com.discgolf.card.ui.theme.AppTopBar
import com.discgolf.card.ui.theme.EmptyState
import com.discgolf.card.ui.theme.LoadingState

@Composable
fun RoundDetailScreen(
    roundId: String,
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
                title = stringResource(R.string.round_title),
                onBack = onBack,
                actions = { ShareRoundAction(state.shareText) }
            )
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
                    RoundResultContent(state = state, showHoleTable = true)
                }
            }
        }
    }
}
