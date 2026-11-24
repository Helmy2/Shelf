package com.shelf.feature.book.impl

import androidx.lifecycle.viewModelScope
import com.shelf.feature.book.domain.usecase.ObserveBookStats
import com.shelf.shared.core.presentation.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BookStatsViewModel(
    private val observeBookStats: ObserveBookStats,
    private val mapper: BookPresentationMapper,
) : MviViewModel<BookStatsState, BookStatsAction, BookStatsEvent>(BookStatsState()) {
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        updateState { it.copy(contentState = BookStatsContentState.Loading) }
        observeJob = viewModelScope.launch {
            observeBookStats()
                .onEach { stats -> updateState { it.copy(contentState = mapper.toStatsContentState(stats)) } }
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    updateState { it.copy(contentState = BookStatsContentState.Error) }
                }
                .collect {}
        }
    }

    override suspend fun handleAction(action: BookStatsAction) {
        when (action) {
            BookStatsAction.Retry -> startObserving()
            BookStatsAction.Back -> sendEvent(BookStatsEvent.NavigateBack)
            BookStatsAction.AddBook -> sendEvent(BookStatsEvent.NavigateToAdd)
        }
    }
}
