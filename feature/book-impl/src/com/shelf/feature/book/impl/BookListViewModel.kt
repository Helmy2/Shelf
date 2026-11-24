package com.shelf.feature.book.impl

import androidx.lifecycle.viewModelScope
import com.shelf.feature.book.domain.usecase.ObserveBooks
import com.shelf.shared.core.presentation.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BookListViewModel(
    private val observeBooks: ObserveBooks,
    private val mapper: BookPresentationMapper,
    private val announcer: BookDeleteAnnouncer,
) : MviViewModel<BookListState, BookListAction, BookListEvent>(BookListState()) {
    /** Title of a book just deleted from the detail; the screen announces it once, then consumes it. */
    val pendingDeleteTitle: StateFlow<String?> = announcer.pendingTitle

    fun consumeDeleteAnnouncement(title: String) = announcer.consume(title)

    private var observeJob: Job? = null
    private var allItems: List<BookItemUi> = emptyList()

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        updateState { it.copy(contentState = BookListContentState.Loading) }
        observeJob = viewModelScope.launch {
            observeBooks()
                .onEach { books ->
                    allItems = books.map(mapper::toItemUi)
                    updateState { current ->
                        // An empty shelf resets the filter so a later added book is never hidden by it.
                        val filter = if (allItems.isEmpty()) BookListFilter.All else current.selectedFilter
                        current.copy(contentState = contentFor(filter), selectedFilter = filter)
                    }
                }
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    updateState { it.copy(contentState = BookListContentState.Error) }
                }
                .collect {}
        }
    }

    private fun contentFor(filter: BookListFilter): BookListContentState =
        if (allItems.isEmpty()) {
            BookListContentState.Empty
        } else {
            BookListContentState.Content(
                items = allItems.filter { filter.matches(it.status) },
                unfilteredCount = allItems.size,
            )
        }

    override suspend fun handleAction(action: BookListAction) {
        when (action) {
            BookListAction.OpenAdd -> sendEvent(BookListEvent.NavigateToAdd)
            BookListAction.OpenStats -> sendEvent(BookListEvent.NavigateToStats)
            is BookListAction.OpenBook -> sendEvent(BookListEvent.NavigateToDetail(action.id))
            BookListAction.Retry -> startObserving()
            is BookListAction.SelectFilter -> updateState { current ->
                if (current.selectedFilter == action.filter || current.contentState !is BookListContentState.Content) {
                    current
                } else {
                    current.copy(contentState = contentFor(action.filter), selectedFilter = action.filter)
                }
            }
        }
    }
}
