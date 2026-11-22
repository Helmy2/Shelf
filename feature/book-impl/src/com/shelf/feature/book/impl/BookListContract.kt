package com.shelf.feature.book.impl

/** UI-level status filter of the list (not a domain type). */
enum class BookListFilter {
    All, WantToRead, Reading, Finished;

    fun matches(status: BookStatusUi): Boolean = when (this) {
        All -> true
        WantToRead -> status == BookStatusUi.WantToRead
        Reading -> status == BookStatusUi.Reading
        Finished -> status == BookStatusUi.Finished
    }

    /** The status the filter names, or null for [All]. */
    val status: BookStatusUi?
        get() = when (this) {
            All -> null
            WantToRead -> BookStatusUi.WantToRead
            Reading -> BookStatusUi.Reading
            Finished -> BookStatusUi.Finished
        }
}

data class BookListState(
    val contentState: BookListContentState = BookListContentState.Loading,
    val selectedFilter: BookListFilter = BookListFilter.All,
) {
    /** Filter chips are shown only when at least one book exists (never in Loading, Error or no-books). */
    val isFilterVisible: Boolean
        get() = (contentState as? BookListContentState.Content)?.let { it.unfilteredCount >= 1 } == true
}

sealed interface BookListContentState {
    data object Loading : BookListContentState
    data object Empty : BookListContentState
    /**
     * [items] are the visible rows (observed books filtered by status, newest first);
     * [unfilteredCount] is the total number of books. Empty [items] with a count >= 1 means "no match".
     */
    data class Content(val items: List<BookItemUi>, val unfilteredCount: Int = items.size) : BookListContentState
    data object Error : BookListContentState
}

sealed interface BookListAction {
    data object OpenAdd : BookListAction
    data class OpenBook(val id: Long) : BookListAction
    data object Retry : BookListAction
    data class SelectFilter(val filter: BookListFilter) : BookListAction
}

sealed interface BookListEvent {
    data object NavigateToAdd : BookListEvent
    data class NavigateToDetail(val id: Long) : BookListEvent
}
