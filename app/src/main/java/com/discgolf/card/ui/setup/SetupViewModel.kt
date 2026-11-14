package com.discgolf.card.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.discgolf.card.data.MAX_HOLES
import com.discgolf.card.data.MAX_PLAYERS
import com.discgolf.card.data.RoundConfig
import com.discgolf.card.data.RoundEntity
import com.discgolf.card.data.RoundRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PlayerInput(val id: String = UUID.randomUUID().toString(), val name: String = "")

data class SetupErrors(
    val courseRequired: Boolean = false,
    val holeCountInvalid: Boolean = false,
    val startingHoleInvalid: Boolean = false,
    val overallParInvalid: Boolean = false,
    val blankPlayerIds: Set<String> = emptySet()
) {
    val hasErrors: Boolean
        get() = courseRequired || holeCountInvalid || startingHoleInvalid ||
            overallParInvalid || blankPlayerIds.isNotEmpty()
}

data class SetupUiState(
    val courseName: String = "",
    val layoutName: String = "",
    val players: List<PlayerInput> = listOf(PlayerInput()),
    val holeCountText: String = "18",
    val startingHoleText: String = "1",
    val overallParText: String = "",
    val errors: SetupErrors = SetupErrors(),
    val isStarting: Boolean = false,
    val showDraftWarning: Boolean = false
) {
    val holeCount: Int? get() = holeCountText.toIntOrNull()?.takeIf { it in 1..MAX_HOLES }
    val canAddPlayer: Boolean get() = players.size < MAX_PLAYERS
}

sealed interface SetupEvent {
    data class OpenScorecard(val roundId: String) : SetupEvent
}

class SetupViewModel(private val repository: RoundRepository) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    val draft: StateFlow<RoundEntity?> = repository.observeDraft()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<SetupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onCourseChange(value: String) = update {
        copy(courseName = value, errors = errors.copy(courseRequired = false))
    }

    fun onLayoutChange(value: String) = update { copy(layoutName = value) }

    fun onHoleCountChange(value: String) = update {
        copy(
            holeCountText = value.take(2),
            errors = errors.copy(holeCountInvalid = false, startingHoleInvalid = false)
        )
    }

    fun onHolePreset(value: Int) = update {
        copy(
            holeCountText = value.toString(),
            errors = errors.copy(holeCountInvalid = false, startingHoleInvalid = false)
        )
    }

    fun onStartingHoleChange(value: String) = update {
        copy(startingHoleText = value.take(2), errors = errors.copy(startingHoleInvalid = false))
    }

    fun onOverallParChange(value: String) = update {
        copy(overallParText = value.take(3), errors = errors.copy(overallParInvalid = false))
    }

    fun addPlayer() = update {
        if (players.size >= MAX_PLAYERS) {
            this
        } else {
            copy(players = players + PlayerInput())
        }
    }

    fun removePlayer(id: String) = update {
        if (players.size <= 1) {
            this
        } else {
            copy(players = players.filterNot { it.id == id })
        }
    }

    fun onPlayerNameChange(id: String, name: String) = update {
        copy(
            players = players.map { if (it.id == id) it.copy(name = name) else it },
            errors = errors.copy(blankPlayerIds = errors.blankPlayerIds - id)
        )
    }

    fun onStartClicked() {
        val current = _state.value
        val errors = validate(current)
        if (errors.hasErrors) {
            _state.value = current.copy(errors = errors)
            return
        }
        if (draft.value != null) {
            _state.value = current.copy(showDraftWarning = true)
            return
        }
        startRound()
    }

    fun dismissDraftWarning() = update { copy(showDraftWarning = false) }

    fun confirmDiscardDraftAndStart() {
        _state.value = _state.value.copy(showDraftWarning = false)
        startRound(discardDraftFirst = true)
    }

    fun discardDraft() {
        val currentDraft = draft.value ?: return
        viewModelScope.launch { repository.discardDraft(currentDraft.id) }
    }

    fun onDraftResumed() = update { copy(showDraftWarning = false) }

    private fun startRound(discardDraftFirst: Boolean = false) {
        val current = _state.value
        val holeCount = current.holeCount ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isStarting = true)
            if (discardDraftFirst) draft.value?.let { repository.discardDraft(it.id) }
            val roundId = repository.startRound(
                RoundConfig(
                    courseName = current.courseName,
                    layoutName = current.layoutName,
                    players = current.players.map { it.name },
                    overallPar = current.overallParText.toIntOrNull(),
                    holeCount = holeCount,
                    startingHole = current.startingHoleText.toIntOrNull() ?: 1
                )
            )
            _state.value = _state.value.copy(isStarting = false)
            _events.send(SetupEvent.OpenScorecard(roundId))
        }
    }

    private fun update(block: SetupUiState.() -> SetupUiState) {
        _state.value = _state.value.block()
    }

    private fun validate(state: SetupUiState): SetupErrors {
        val holeCount = state.holeCount
        val startingHole = state.startingHoleText.toIntOrNull() ?: 1
        val par = state.overallParText.toIntOrNull()
        val blankIds = state.players.filter { it.name.isBlank() }.map { it.id }.toSet()
        return SetupErrors(
            courseRequired = state.courseName.isBlank(),
            holeCountInvalid = holeCount == null,
            startingHoleInvalid = holeCount != null && startingHole !in 1..holeCount,
            overallParInvalid = state.overallParText.isNotBlank() && (par == null || par !in 1..999),
            blankPlayerIds = blankIds
        )
    }

    companion object {
        fun factory(repository: RoundRepository) = viewModelFactory {
            initializer { SetupViewModel(repository) }
        }
    }
}
