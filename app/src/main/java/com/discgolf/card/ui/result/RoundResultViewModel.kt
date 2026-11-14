package com.discgolf.card.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.discgolf.card.data.RoundEntity
import com.discgolf.card.data.RoundRepository
import com.discgolf.card.data.RoundWithPlayers
import com.discgolf.card.data.holeSequence
import com.discgolf.card.data.standings
import com.discgolf.card.ui.shareText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ResultPlayerRow(
    val name: String,
    val total: Int,
    val rank: Int,
    val delta: Int?
)

data class ResultHoleRow(
    val hole: Int,
    val strokes: List<Int>
)

data class ResultUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val round: RoundEntity? = null,
    val players: List<ResultPlayerRow> = emptyList(),
    val columns: List<String> = emptyList(),
    val holes: List<ResultHoleRow> = emptyList(),
    val shareText: String = ""
)

class RoundResultViewModel(repository: RoundRepository, roundId: String) : ViewModel() {

    val state: StateFlow<ResultUiState> = repository.observeRound(roundId)
        .map { round ->
            if (round == null) {
                ResultUiState(isLoading = false, notFound = true)
            } else {
                build(round)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())

    private fun build(round: RoundWithPlayers): ResultUiState {
        val entity = round.round
        val ranked = standings(round.players, entity.overallPar)
        val orderedPlayers = round.players.sortedBy { it.player.position }
        val sequence = holeSequence(entity.startingHole, entity.holeCount)

        val holes = sequence.map { hole ->
            ResultHoleRow(
                hole = hole,
                strokes = orderedPlayers.map { player ->
                    player.scores.firstOrNull { it.holeNumber == hole }?.strokes ?: 0
                }
            )
        }

        return ResultUiState(
            isLoading = false,
            round = entity,
            players = ranked.map {
                ResultPlayerRow(
                    name = it.player.player.name,
                    total = it.total,
                    rank = it.rank,
                    delta = it.delta
                )
            },
            columns = orderedPlayers.map { it.player.name },
            holes = holes,
            shareText = shareText(entity, ranked)
        )
    }

    companion object {
        fun factory(repository: RoundRepository, roundId: String) = viewModelFactory {
            initializer { RoundResultViewModel(repository, roundId) }
        }
    }
}
