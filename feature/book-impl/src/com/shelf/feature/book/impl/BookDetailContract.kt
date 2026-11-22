package com.shelf.feature.book.impl

data class BookDetailState(
    val contentState: BookDetailContentState = BookDetailContentState.Loading,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val hasDeleteError: Boolean = false,
)

sealed interface BookDetailContentState {
    data object Loading : BookDetailContentState
    data object NotFound : BookDetailContentState
    data class Content(val book: BookDetailUi) : BookDetailContentState
    data object Error : BookDetailContentState
}

sealed interface BookDetailAction {
    data class Load(val id: Long) : BookDetailAction
    data object Retry : BookDetailAction
    data object Edit : BookDetailAction
    data object Back : BookDetailAction
    data object RequestDelete : BookDetailAction
    data object ConfirmDelete : BookDetailAction
    data object CancelDelete : BookDetailAction
}

sealed interface BookDetailEvent {
    data class NavigateToEdit(val id: Long) : BookDetailEvent
    data object NavigateBack : BookDetailEvent

    /** Delete succeeded (or the book was already gone): pop back to the list. */
    data object NavigateBackAfterDelete : BookDetailEvent
}
