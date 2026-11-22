package com.shelf.feature.book.impl

import com.shelf.feature.book.domain.model.BookError
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.usecase.AddBook
import com.shelf.feature.book.domain.usecase.ObserveBook
import com.shelf.feature.book.domain.usecase.UpdateBook
import com.shelf.shared.core.presentation.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class BookFormViewModel(
    private val addBook: AddBook,
    private val updateBook: UpdateBook,
    private val observeBook: ObserveBook,
    private val mapper: BookPresentationMapper,
) : MviViewModel<BookFormState, BookFormAction, BookFormEvent>(BookFormState()) {
    /** The form is prefilled once; later repository emissions never overwrite what the user types. */
    private var prefilled = false
    private var started = false

    override suspend fun handleAction(action: BookFormAction) {
        when (action) {
            is BookFormAction.Load -> load(action.id)
            BookFormAction.RetryLoad -> state.value.bookId?.let { loadForEdit(it) }
            is BookFormAction.TitleChanged -> updateState {
                it.copy(
                    title = action.value,
                    titleError = if (action.value.isNotBlank()) false else it.titleError,
                )
            }

            is BookFormAction.AuthorChanged -> updateState { it.copy(author = action.value) }
            is BookFormAction.StatusChanged -> updateState {
                // Leaving Finished drops the rating; returning to Finished starts unset.
                if (it.status == action.value) it else it.copy(status = action.value, rating = null, ratingError = false)
            }

            is BookFormAction.RatingChanged -> updateState { it.copy(rating = action.value, ratingError = false) }
            is BookFormAction.NotesChanged -> updateState { it.copy(notes = action.value) }
            BookFormAction.Save -> save()
            BookFormAction.Cancel -> finish(BookFormEvent.Cancelled)
        }
    }

    /** True once Saved/Cancelled was emitted: the next Load starts a brand new session. */
    private var finished = false

    private suspend fun finish(event: BookFormEvent) {
        finished = true
        sendEvent(event)
    }

    private suspend fun load(id: Long?) {
        // Defense in depth when an instance is reused: restart from a clean state for a different
        // book or after a completed session. Same id without finishing (recomposition, rotation)
        // keeps whatever the user typed.
        if (finished || (started && state.value.bookId != id)) {
            prefilled = false
            updateState { BookFormState() }
        }
        finished = false
        started = true
        if (id == null) {
            if (state.value.titleFocusRequests == 0) {
                updateState { it.copy(titleFocusRequests = 1) }
            }
            return
        }
        if (prefilled && state.value.bookId == id) return
        loadForEdit(id)
    }

    private suspend fun loadForEdit(id: Long) {
        updateState { it.copy(bookId = id, loadState = BookFormLoadState.Loading) }
        val book: BookItem? = try {
            observeBook(id).first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateState { it.copy(loadState = BookFormLoadState.Error) }
            return
        }
        if (book == null) {
            updateState { it.copy(loadState = BookFormLoadState.NotFound) }
            return
        }
        prefilled = true
        val fields = mapper.toFormState(book)
        updateState {
            it.copy(
                title = fields.title,
                author = fields.author,
                status = fields.status,
                rating = fields.rating,
                notes = fields.notes,
                loadState = BookFormLoadState.Ready,
                titleFocusRequests = it.titleFocusRequests + 1,
            )
        }
    }

    private suspend fun save() {
        val current = state.value
        if (current.isSaving || current.loadState != BookFormLoadState.Ready) return
        updateState { it.copy(isSaving = true, saveError = false, ratingError = false) }

        val result = try {
            val id = current.bookId
            if (id == null) {
                addBook(mapper.toDraft(current))
            } else {
                val existing = observeBook(id).first()
                if (existing == null) {
                    BookResult.Failure(BookError.NotFound)
                } else {
                    updateBook(mapper.toUpdated(existing, current))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BookResult.Failure(BookError.Unknown)
        }

        when (result) {
            is BookResult.Success -> {
                updateState { it.copy(isSaving = false) }
                finish(BookFormEvent.Saved)
            }

            is BookResult.Failure -> when (result.error) {
                BookError.TitleRequired -> updateState {
                    it.copy(
                        isSaving = false,
                        titleError = true,
                        titleFocusRequests = it.titleFocusRequests + 1,
                    )
                }

                BookError.RatingOutOfRange -> updateState { it.copy(isSaving = false, ratingError = true) }

                BookError.NotFound -> updateState {
                    it.copy(isSaving = false, loadState = BookFormLoadState.NotFound)
                }

                else -> updateState { it.copy(isSaving = false, saveError = true) }
            }
        }
    }
}
