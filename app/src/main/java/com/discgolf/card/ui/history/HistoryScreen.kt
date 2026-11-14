package com.discgolf.card.ui.history

import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.discgolf.card.DiscGolfApp
import com.discgolf.card.R
import com.discgolf.card.ui.backupFileName
import com.discgolf.card.ui.formatDelta
import com.discgolf.card.ui.theme.AppCard
import com.discgolf.card.ui.theme.AppTopBar
import com.discgolf.card.ui.theme.ConfirmDialog
import com.discgolf.card.ui.theme.DiscGolfCardTheme
import com.discgolf.card.ui.theme.EmptyState
import com.discgolf.card.ui.theme.LoadingState
import com.discgolf.card.ui.theme.Space

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onRoundClick: (String) -> Unit,
    onStartRound: () -> Unit
) {
    val app = LocalContext.current.applicationContext as DiscGolfApp
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(app.repository, app.fileTransfer)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::import) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(messageText(context, message))
        }
    }

    var pendingDelete by remember { mutableStateOf<HistoryRow?>(null) }

    HistoryContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRoundClick = onRoundClick,
        onStartRound = onStartRound,
        onDeleteClick = { pendingDelete = it },
        onExportClick = { exportLauncher.launch(backupFileName()) },
        onImportClick = {
            importLauncher.launch(
                arrayOf("application/json", "text/plain", "application/octet-stream")
            )
        }
    )

    pendingDelete?.let { row ->
        ConfirmDialog(
            title = stringResource(R.string.delete_round_title),
            message = stringResource(R.string.delete_round_message, row.courseName),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.delete(row.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRoundClick: (String) -> Unit,
    onStartRound: () -> Unit,
    onDeleteClick: (HistoryRow) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.history_title),
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.more_actions),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_to_drive)) },
                                enabled = !state.isTransferring,
                                onClick = {
                                    menuOpen = false
                                    onExportClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_from_drive)) },
                                enabled = !state.isTransferring,
                                onClick = {
                                    menuOpen = false
                                    onImportClick()
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.rows.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Rounded.List,
                    title = stringResource(R.string.history_empty_title),
                    message = stringResource(R.string.history_empty_message),
                    actionLabel = stringResource(R.string.start_a_round),
                    onAction = onStartRound,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(Space.lg),
                        verticalArrangement = Arrangement.spacedBy(Space.md)
                    ) {
                        items(state.rows, key = { it.id }) { row ->
                            HistoryRowCard(
                                row = row,
                                onClick = { onRoundClick(row.id) },
                                onDeleteClick = { onDeleteClick(row) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRowCard(
    row: HistoryRow,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val winnerLabel = when {
        row.winnerNames.size > 1 -> stringResource(
            R.string.history_tied,
            row.winnerNames.joinToString(", ")
        )

        row.winnerNames.size == 1 -> stringResource(
            R.string.history_won_by,
            row.winnerNames.first()
        )

        else -> ""
    }
    val total = row.total
    val delta = row.delta
    val scoreLabel = if (total == null) {
        ""
    } else if (delta != null) {
        pluralStringResource(R.plurals.history_score_par, total, total, formatDelta(delta))
    } else {
        pluralStringResource(R.plurals.history_score, total, total)
    }
    val resultLabel = listOf(winnerLabel, scoreLabel)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val subtitle = listOfNotNull(row.layoutName.takeIf { it.isNotBlank() }, row.dateText)
        .joinToString(" · ")

    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = Space.lg, top = Space.md, bottom = Space.md, end = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    text = row.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = resultLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(
                        R.string.delete_round_description,
                        row.courseName
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun messageText(context: Context, message: HistoryMessage): String = when (message) {
    is HistoryMessage.Exported -> context.resources.getQuantityString(
        R.plurals.exported_rounds,
        message.count,
        message.count
    )

    HistoryMessage.ExportEmpty -> context.getString(R.string.export_empty)
    HistoryMessage.ExportFailed -> context.getString(R.string.export_failed)
    is HistoryMessage.Imported -> buildString {
        append(
            context.resources.getQuantityString(
                R.plurals.imported_rounds,
                message.duplicates,
                message.imported,
                message.duplicates
            )
        )
        if (message.invalid > 0) {
            append(" · ")
            append(
                context.resources.getQuantityString(
                    R.plurals.skipped_invalid,
                    message.invalid,
                    message.invalid
                )
            )
        }
    }

    HistoryMessage.ImportFailed -> context.getString(R.string.import_failed)
    HistoryMessage.Deleted -> context.getString(R.string.round_deleted)
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    DiscGolfCardTheme {
        HistoryContent(
            state = HistoryUiState(
                isLoading = false,
                rows = listOf(
                    HistoryRow("1", "Birch Park", "Blue", "Nov 12, 2026", listOf("Alex"), 52, -2),
                    HistoryRow("2", "Oak Ridge", "", "Nov 4, 2026", listOf("Sam", "Alex"), 57, 3)
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRoundClick = {},
            onStartRound = {},
            onDeleteClick = {},
            onExportClick = {},
            onImportClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryDarkPreview() {
    DiscGolfCardTheme(darkTheme = true) {
        HistoryContent(
            state = HistoryUiState(isLoading = false, rows = emptyList()),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRoundClick = {},
            onStartRound = {},
            onDeleteClick = {},
            onExportClick = {},
            onImportClick = {}
        )
    }
}
