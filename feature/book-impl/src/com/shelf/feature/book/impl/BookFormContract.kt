package com.shelf.feature.book.impl

enum class BookFormLoadState { Ready, Loading, NotFound, Error }

data class BookFormState(
    val title: String = "",
    val author: String = "",
    val status: BookStatusUi = BookStatusUi.WantToRead,
    /** 1..5 or null; only meaningful (and shown) while [status] is Finished. */
    val rating: Int? = null,
    val notes: String = "",
    val ratingError: Boolean = false,
    val titleError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    /** Incremented to ask the view to focus the title field (initial open, prefill and after a validation error). */
    val titleFocusRequests: Int = 0,
    /** Non-null in edit mode. */
    val bookId: Long? = null,
    val loadState: BookFormLoadState = BookFormLoadState.Ready,
) {
    val isEdit: Boolean get() = bookId != null
    val showRating: Boolean get() = status == BookStatusUi.Finished
}

sealed interface BookFormAction {
    /** Starts the form: [id] null is add mode, otherwise the book to edit is loaded and prefilled once. */
    data class Load(val id: Long?) : BookFormAction
    data object RetryLoad : BookFormAction
    data class TitleChanged(val value: String) : BookFormAction
    data class AuthorChanged(val value: String) : BookFormAction
    data class StatusChanged(val value: BookStatusUi) : BookFormAction
    /** [value] null clears the rating. */
    data class RatingChanged(val value: Int?) : BookFormAction
    data class NotesChanged(val value: String) : BookFormAction
    data object Save : BookFormAction
    data object Cancel : BookFormAction
}

sealed interface BookFormEvent {
    data object Saved : BookFormEvent
    data object Cancelled : BookFormEvent
}
