package com.shelf.feature.book.impl

/** Presentation model of the stats; [averageText] is already formatted with one decimal, null when nothing is rated. */
data class BookStatsUi(
    val wantToReadCount: Int,
    val readingCount: Int,
    val finishedCount: Int,
    val ratedCount: Int,
    val averageText: String?,
)

sealed interface BookStatsContentState {
    data object Loading : BookStatsContentState
    data object Empty : BookStatsContentState
    data class Content(val stats: BookStatsUi) : BookStatsContentState
    data object Error : BookStatsContentState
}

data class BookStatsState(
    val contentState: BookStatsContentState = BookStatsContentState.Loading,
)

sealed interface BookStatsAction {
    data object Retry : BookStatsAction
    data object Back : BookStatsAction
    data object AddBook : BookStatsAction
}

sealed interface BookStatsEvent {
    data object NavigateBack : BookStatsEvent
    data object NavigateToAdd : BookStatsEvent
}
