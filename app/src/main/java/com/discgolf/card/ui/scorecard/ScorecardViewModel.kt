package com.discgolf.card.ui.scorecard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.discgolf.card.data.DEFAULT_STROKES
import com.discgolf.card.data.RoundRepository
import com.discgolf.card.data.RoundWithPlayers
import com.discgolf.card.data.holeSequence
import com.discgolf.card.data.runningTotal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerScoreRow(
    val id: String,
    val name: String,
    val strokes: Int,
    val runningTotal: Int,
    val delta: Int?
)

data class ScorecardUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val courseName: String = "",
    val layoutName: String = "",
    val holeNumber: Int = 0,
    val currentIndex: Int = 0,
    val holeCount: Int = 0,
    val isLastHole: Boolean = false,
    val players: List<PlayerScoreRow> = emptyList(),
    val isFinishing: Boolean = false
)

class ScorecardViewModel(
    private val repository: RoundRepository,
    private val roundId: String
) : ViewModel() {

    /** Null until the persisted draft position has been read; avoids a hole-1 flash. */
    private val index = MutableStateFlow<Int?>(null)
    private val finishing = MutableStateFlow(false)
    private val _events = Channel<Unit>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<ScorecardUiState> = combine(
        repository.observeRound(roundId),
        index,
        finishing
    ) { round, indexValue, isFinishing ->
        when {
            round == null -> ScorecardUiState(isLoading = false, notFound = true)
            indexValue == null -> ScorecardUiState(isLoading = true)
            else -> buildState(round, indexValue, isFinishing)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScorecardUiState())

    init {
        viewModelScope.launch {
            val round = repository.observeRound(roundId).first() ?: return@launch
            val maxIndex = (round.round.holeCount - 1).coerceAtLeast(0)
            index.value = round.round.lastHoleIndex.coerceIn(0, maxIndex)
        }
        viewModelScope.launch {
            index.filterNotNull().drop(1).collect { value ->
                repository.updateLastHole(roundId, value)
            }
        }
    }

    fun previous() {
        val current = index.value ?: return
        index.value = (current - 1).coerceAtLeast(0)
    }

    fun next() {
        val current = state.value
        if (current.isLastHole) return
        index.value = (current.currentIndex + 1).coerceAtMost(current.holeCount - 1)
    }

    fun adjust(playerId: String, delta: Int) {
        val hole = state.value.holeNumber
        if (hole <= 0) return
        viewModelScope.launch {
            repository.adjustStrokes(roundId, playerId, hole, delta)
        }
    }

    fun finish() {
        if (finishing.value) return
        viewModelScope.launch {
            finishing.value = true
            try {
                repository.finishRound(roundId)
                _events.send(Unit)
            } finally {
                finishing.value = false
            }
        }
    }

    private fun buildState(
        round: RoundWithPlayers,
        requestedIndex: Int,
        isFinishing: Boolean
    ): ScorecardUiState {
        val entity = round.round
        val holeCount = entity.holeCount.coerceAtLeast(1)
        val index = requestedIndex.coerceIn(0, holeCount - 1)
        val sequence = holeSequence(entity.startingHole, holeCount)
        val hole = sequence[index]
        val isLast = index == holeCount - 1

        val rows = round.players
            .sortedBy { it.player.position }
            .map { player ->
                val total = runningTotal(player.scores, sequence, index)
                PlayerScoreRow(
                    id = player.player.id,
                    name = player.player.name,
                    strokes = player.scores.firstOrNull { it.holeNumber == hole }?.strokes
                        ?: DEFAULT_STROKES,
                    runningTotal = total,
                    delta = if (isLast) entity.overallPar?.let { total - it } else null
                )
            }

        return ScorecardUiState(
            isLoading = false,
            courseName = entity.courseName,
            layoutName = entity.layoutName,
            holeNumber = hole,
            currentIndex = index,
            holeCount = holeCount,
            isLastHole = isLast,
            players = rows,
            isFinishing = isFinishing
        )
    }

    companion object {
        fun factory(repository: RoundRepository, roundId: String) = viewModelFactory {
            initializer { ScorecardViewModel(repository, roundId) }
        }
    }
}
