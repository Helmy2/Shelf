package com.shelf.feature.book.impl

import androidx.lifecycle.viewModelScope
import com.shelf.feature.book.domain.model.BookError
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.usecase.DeleteBook
import com.shelf.feature.book.domain.usecase.ObserveBook
import com.shelf.shared.core.presentation.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val observeBook: ObserveBook,
    private val mapper: BookPresentationMapper,
    private val deleteBook: DeleteBook,
    private val announcer: BookDeleteAnnouncer,
) : MviViewModel<BookDetailState, BookDetailAction, BookDetailEvent>(BookDetailState()) {
    private var bookId: Long? = null
    private var observeJob: Job? = null

    /** Set once a delete succeeded (or hit NotFound): the observer must not flash Not found any more. */
    private var deleted = false

    private fun startObserving(id: Long) {
        observeJob?.cancel()
        bookId = id
        updateState { it.copy(contentState = BookDetailContentState.Loading) }
        observeJob = viewModelScope.launch {
            observeBook(id)
                .onEach { book ->
                    // The book vanishing because of our own delete is not "Not found": navigation wins.
                    if (deleted || (book == null && currentState().isDeleting)) return@onEach
                    updateState {
                        it.copy(
                            contentState = if (book == null) {
                                BookDetailContentState.NotFound
                            } else {
                                BookDetailContentState.Content(mapper.toDetailUi(book))
                            },
                        )
                    }
                }
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    updateState { it.copy(contentState = BookDetailContentState.Error) }
                }
                .collect {}
        }
    }

    override suspend fun handleAction(action: BookDetailAction) {
        when (action) {
            is BookDetailAction.Load -> if (bookId != action.id) startObserving(action.id)
            BookDetailAction.Retry -> bookId?.let(::startObserving)
            BookDetailAction.Edit -> bookId?.let { sendEvent(BookDetailEvent.NavigateToEdit(it)) }
            BookDetailAction.Back -> sendEvent(BookDetailEvent.NavigateBack)
            BookDetailAction.RequestDelete -> {
                val s = currentState()
                if (s.contentState is BookDetailContentState.Content && !s.isDeleting && !deleted) {
                    updateState { it.copy(isDeleteDialogVisible = true, hasDeleteError = false) }
                }
            }

            BookDetailAction.CancelDelete -> updateState {
                if (it.isDeleting) it else it.copy(isDeleteDialogVisible = false)
            }

            BookDetailAction.ConfirmDelete -> confirmDelete()
        }
    }

    private fun currentState() = state.value

    private suspend fun confirmDelete() {
        val s = currentState()
        val content = s.contentState as? BookDetailContentState.Content ?: return
        if (!s.isDeleteDialogVisible || s.isDeleting || deleted) return
        val id = content.book.id
        val title = content.book.title
        updateState { it.copy(isDeleting = true, hasDeleteError = false) }
        val succeeded = try {
            when (val result = deleteBook(id)) {
                is BookResult.Success -> true
                is BookResult.Failure -> result.error == BookError.NotFound
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            false
        }
        if (succeeded) {
            deleted = true
            observeJob?.cancel()
            // Dialog closes; isDeleting stays true so Edit/Delete stay disabled while the screen pops.
            updateState { it.copy(isDeleteDialogVisible = false) }
            announcer.post(title)
            sendEvent(BookDetailEvent.NavigateBackAfterDelete)
        } else {
            updateState { it.copy(isDeleting = false, isDeleteDialogVisible = false, hasDeleteError = true) }
        }
    }
}
