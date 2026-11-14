package com.discgolf.card.ui.setup

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.discgolf.card.DiscGolfApp
import com.discgolf.card.R
import com.discgolf.card.data.RoundEntity
import com.discgolf.card.ui.theme.AppTopBar
import com.discgolf.card.ui.theme.ConfirmDialog
import com.discgolf.card.ui.theme.DiscGolfCardTheme
import com.discgolf.card.ui.theme.NumberField
import com.discgolf.card.ui.theme.PrimaryButton
import com.discgolf.card.ui.theme.ResumeBanner
import com.discgolf.card.ui.theme.SectionLabel
import com.discgolf.card.ui.theme.Space

class SetupActions(
    val onCourseChange: (String) -> Unit = {},
    val onLayoutChange: (String) -> Unit = {},
    val onHoleCountChange: (String) -> Unit = {},
    val onHolePreset: (Int) -> Unit = {},
    val onStartingHoleChange: (String) -> Unit = {},
    val onOverallParChange: (String) -> Unit = {},
    val onPlayerNameChange: (String, String) -> Unit = { _, _ -> },
    val onAddPlayer: () -> Unit = {},
    val onRemovePlayer: (String) -> Unit = {},
    val onStart: () -> Unit = {},
    val onResumeDraft: (String) -> Unit = {},
    val onDiscardDraft: () -> Unit = {},
    val onDismissDraftWarning: () -> Unit = {},
    val onConfirmDiscardAndStart: () -> Unit = {},
    val onOpenHistory: () -> Unit = {}
)

@Composable
fun SetupScreen(
    onOpenScorecard: (String) -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenHistory: () -> Unit
) {
    val app = LocalContext.current.applicationContext as DiscGolfApp
    val viewModel: SetupViewModel = viewModel(factory = SetupViewModel.factory(app.repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SetupEvent.OpenScorecard -> onOpenScorecard(event.roundId)
            }
        }
    }

    SetupContent(
        state = state,
        draft = draft,
        actions = SetupActions(
            onCourseChange = viewModel::onCourseChange,
            onLayoutChange = viewModel::onLayoutChange,
            onHoleCountChange = viewModel::onHoleCountChange,
            onHolePreset = viewModel::onHolePreset,
            onStartingHoleChange = viewModel::onStartingHoleChange,
            onOverallParChange = viewModel::onOverallParChange,
            onPlayerNameChange = viewModel::onPlayerNameChange,
            onAddPlayer = viewModel::addPlayer,
            onRemovePlayer = viewModel::removePlayer,
            onStart = viewModel::onStartClicked,
            onResumeDraft = onOpenDraft,
            onDiscardDraft = viewModel::discardDraft,
            onDismissDraftWarning = viewModel::dismissDraftWarning,
            onConfirmDiscardAndStart = viewModel::confirmDiscardDraftAndStart,
            onOpenHistory = onOpenHistory
        )
    )
}

@Composable
private fun SetupContent(
    state: SetupUiState,
    draft: RoundEntity?,
    actions: SetupActions
) {
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(draft) {
        if (draft == null) showDiscardConfirm = false
    }

    val courseError = if (state.errors.courseRequired) stringResource(R.string.course_required) else null
    val holesError = if (state.errors.holeCountInvalid) stringResource(R.string.holes_range_error) else null
    val startingHoleError = if (state.errors.startingHoleInvalid) {
        stringResource(R.string.starting_hole_range_error, state.holeCount ?: 36)
    } else {
        null
    }
    val parError = if (state.errors.overallParInvalid) stringResource(R.string.par_range_error) else null

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.setup_title),
                actions = {
                    TextButton(
                        onClick = actions.onOpenHistory,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.history))
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Space.lg, vertical = Space.md),
                    contentAlignment = Alignment.Center
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.start_round),
                        onClick = actions.onStart,
                        loading = state.isStarting,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Space.lg, vertical = Space.lg),
                    verticalArrangement = Arrangement.spacedBy(Space.lg)
                ) {
                    draft?.let { unfinished ->
                        ResumeBanner(
                            courseName = unfinished.courseName,
                            progressText = pluralStringResource(
                                R.plurals.holes_meta,
                                unfinished.holeCount,
                                unfinished.holeCount
                            ),
                            onResume = { actions.onResumeDraft(unfinished.id) },
                            onDiscard = { showDiscardConfirm = true }
                        )
                    }

                    OutlinedTextField(
                        value = state.courseName,
                        onValueChange = actions.onCourseChange,
                        label = { Text(stringResource(R.string.course)) },
                        placeholder = { Text(stringResource(R.string.course_placeholder)) },
                        singleLine = true,
                        isError = state.errors.courseRequired,
                        supportingText = courseError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.layoutName,
                        onValueChange = actions.onLayoutChange,
                        label = { Text(stringResource(R.string.layout)) },
                        placeholder = { Text(stringResource(R.string.layout_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        SectionLabel(stringResource(R.string.holes))
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                            HolePresetChip(9, state.holeCountText, actions.onHolePreset)
                            HolePresetChip(18, state.holeCountText, actions.onHolePreset)
                            HolePresetChip(27, state.holeCountText, actions.onHolePreset)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            NumberField(
                                value = state.holeCountText,
                                onValueChange = actions.onHoleCountChange,
                                label = stringResource(R.string.holes),
                                isError = state.errors.holeCountInvalid,
                                supportingText = holesError,
                                modifier = Modifier.weight(1f)
                            )
                            NumberField(
                                value = state.startingHoleText,
                                onValueChange = actions.onStartingHoleChange,
                                label = stringResource(R.string.starting_hole),
                                isError = state.errors.startingHoleInvalid,
                                supportingText = startingHoleError,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    NumberField(
                        value = state.overallParText,
                        onValueChange = actions.onOverallParChange,
                        label = stringResource(R.string.overall_par),
                        isError = state.errors.overallParInvalid,
                        supportingText = parError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        SectionLabel(stringResource(R.string.players))
                        state.players.forEachIndexed { index, player ->
                            val playerError = if (player.id in state.errors.blankPlayerIds) {
                                stringResource(R.string.player_name_error)
                            } else {
                                null
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                OutlinedTextField(
                                    value = player.name,
                                    onValueChange = { actions.onPlayerNameChange(player.id, it) },
                                    label = { Text(stringResource(R.string.player_n, index + 1)) },
                                    singleLine = true,
                                    isError = playerError != null,
                                    supportingText = playerError?.let { { Text(it) } },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { actions.onRemovePlayer(player.id) },
                                    enabled = state.players.size > 1,
                                    modifier = Modifier.padding(top = Space.sm)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.remove_player, index + 1)
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = actions.onAddPlayer,
                            enabled = state.canAddPlayer,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = Space.sm)
                            )
                            Text(stringResource(R.string.add_player))
                        }
                    }
                }
            }
        }
    }

    if (showDiscardConfirm && draft != null) {
        ConfirmDialog(
            title = stringResource(R.string.discard_round_title),
            message = stringResource(R.string.discard_round_message, draft.courseName),
            confirmLabel = stringResource(R.string.discard),
            destructive = true,
            onConfirm = {
                actions.onDiscardDraft()
                showDiscardConfirm = false
            },
            onDismiss = { showDiscardConfirm = false }
        )
    }

    if (state.showDraftWarning) {
        ConfirmDialog(
            title = stringResource(R.string.draft_warning_title),
            message = stringResource(
                R.string.draft_warning_message,
                draft?.courseName ?: ""
            ),
            confirmLabel = stringResource(R.string.draft_warning_confirm),
            destructive = true,
            onConfirm = actions.onConfirmDiscardAndStart,
            onDismiss = actions.onDismissDraftWarning
        )
    }

}

@Composable
private fun HolePresetChip(value: Int, current: String, onSelect: (Int) -> Unit) {
    FilterChip(
        selected = current.toIntOrNull() == value,
        onClick = { onSelect(value) },
        label = { Text(value.toString()) }
    )
}

@Preview(showBackground = true)
@Composable
private fun SetupPreview() {
    DiscGolfCardTheme {
        SetupContent(
            state = SetupUiState(
                courseName = "Birch Park",
                layoutName = "Blue",
                players = listOf(PlayerInput(name = "Alex"), PlayerInput(name = "Sam")),
                startingHoleText = "14",
                overallParText = "54"
            ),
            draft = null,
            actions = SetupActions()
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetupDarkPreview() {
    DiscGolfCardTheme(darkTheme = true) {
        SetupContent(
            state = SetupUiState(
                courseName = "Birch Park",
                players = listOf(PlayerInput(name = "Alex"), PlayerInput(name = "Sam")),
                startingHoleText = "14",
                overallParText = "54"
            ),
            draft = null,
            actions = SetupActions()
        )
    }
}
