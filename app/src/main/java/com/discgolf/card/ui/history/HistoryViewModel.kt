package com.discgolf.card.ui.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.discgolf.card.data.FileTransfer
import com.discgolf.card.data.ImportResult
import com.discgolf.card.data.RoundRepository
import com.discgolf.card.data.RoundWithPlayers
import com.discgolf.card.data.standings
import com.discgolf.card.ui.backupFileName
import com.discgolf.card.ui.formatDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class HistoryRow(
    val id: String,
    val courseName: String,
    val layoutName: String,
    val dateText: String,
    val winnerNames: List<String>,
    val total: Int?,
    val delta: Int?
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val rows: List<HistoryRow> = emptyList(),
    val isTransferring: Boolean = false
)

sealed interface HistoryMessage {
    data object ExportEmpty : HistoryMessage
    data object ShareFailed : HistoryMessage
    data class ImportCompleted(val result: ImportResult) : HistoryMessage
    data object ImportFailed : HistoryMessage
    data object Deleted : HistoryMessage
}

class HistoryViewModel(
    private val repository: RoundRepository,
    private val fileTransfer: FileTransfer
) : ViewModel() {

    private val transferring = MutableStateFlow(false)
    private val _messages = Channel<HistoryMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()
    private val _shareFiles = Channel<File>(Channel.BUFFERED)
    val shareFiles = _shareFiles.receiveAsFlow()

    val state: StateFlow<HistoryUiState> = combine(
        repository.observeHistory(),
        transferring
    ) { rounds, isTransferring ->
        HistoryUiState(
            isLoading = false,
            rows = rounds.map { it.toRow() },
            isTransferring = isTransferring
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun delete(roundId: String) {
        viewModelScope.launch {
            repository.deleteRound(roundId)
            _messages.trySend(HistoryMessage.Deleted)
        }
    }

    fun shareBackup() {
        viewModelScope.launch {
            transferring.value = true
            val result = runCatching { fileTransfer.prepareShareFile(backupFileName()) }
                .getOrElse { Result.failure(it) }
            transferring.value = false
            result.onSuccess { share ->
                if (share.roundCount == 0) {
                    _messages.trySend(HistoryMessage.ExportEmpty)
                } else {
                    _shareFiles.trySend(share.file)
                }
            }.onFailure {
                _messages.trySend(HistoryMessage.ShareFailed)
            }
        }
    }

    /** Imports rounds from a user-selected backup document URI. */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            transferring.value = true
            val result = fileTransfer.import(uri)
            transferring.value = false
            result.onSuccess {
                _messages.trySend(HistoryMessage.ImportCompleted(it))
            }.onFailure {
                _messages.trySend(HistoryMessage.ImportFailed)
            }
        }
    }

    private fun RoundWithPlayers.toRow(): HistoryRow {
        val ranked = standings(players, round.overallPar)
        val top = ranked.firstOrNull()
        return HistoryRow(
            id = round.id,
            courseName = round.courseName,
            layoutName = round.layoutName,
            dateText = formatDate(round.startedAt),
            winnerNames = ranked.filter { it.rank == 1 }.map { it.player.player.name },
            total = top?.total,
            delta = top?.delta
        )
    }

    companion object {
        fun factory(repository: RoundRepository, fileTransfer: FileTransfer) = viewModelFactory {
            initializer { HistoryViewModel(repository, fileTransfer) }
        }
    }
}
