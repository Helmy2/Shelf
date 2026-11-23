package com.shelf.feature.book.impl

import androidx.lifecycle.viewModelScope
import com.shelf.feature.book.domain.model.BookStats
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.usecase.ObserveBookStats
import com.shelf.shared.core.presentation.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class BookStatsViewModel(
    private val observeBookStats: ObserveBookStats,
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
                .onEach { stats -> updateState { it.copy(contentState = stats.toContentState()) } }
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

    private fun BookStats.toContentState(): BookStatsContentState {
        val wantToRead = countByStatus[BookStatus.WantToRead] ?: 0
        val reading = countByStatus[BookStatus.Reading] ?: 0
        val finished = countByStatus[BookStatus.Finished] ?: 0
        if (wantToRead == 0 && reading == 0 && finished == 0) return BookStatsContentState.Empty
        return BookStatsContentState.Content(
            BookStatsUi(
                wantToReadCount = wantToRead,
                readingCount = reading,
                finishedCount = finished,
                ratedCount = ratedCount,
                averageText = averageRating?.let(::formatOneDecimal),
            ),
        )
    }

    /** Locale independent one-decimal formatting (e.g. 4.2), rounding half up. */
    private fun formatOneDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        return "${tenths / 10}.${tenths % 10}"
    }
}
