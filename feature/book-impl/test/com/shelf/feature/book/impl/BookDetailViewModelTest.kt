package com.shelf.feature.book.impl

import app.cash.turbine.test
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.usecase.DeleteBook
import com.shelf.feature.book.domain.usecase.ObserveBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val announcer = BookDeleteAnnouncer()

    private fun viewModel(repo: FakeBookRepository) =
        BookDetailViewModel(ObserveBook(repo), BookPresentationMapper(), DeleteBook(repo), announcer)

    @Test
    fun startsInLoadingBeforeFirstEmission() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository(listOf(book(1, "Dune"))))
        assertIs<BookDetailContentState.Loading>(vm.state.value.contentState)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()
    }

    @Test
    fun showsContentAsUiModelAndUpdatesWhenBookChanges() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune", "Frank Herbert", BookStatus.Reading)))
        val vm = viewModel(repo)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()

        val content = assertIs<BookDetailContentState.Content>(vm.state.value.contentState)
        assertEquals(BookDetailUi(1, "Dune", "Frank Herbert", BookStatusUi.Reading), content.book)

        repo.books.value = listOf(book(1, "Dune Messiah"))
        advanceUntilIdle()
        val updated = assertIs<BookDetailContentState.Content>(vm.state.value.contentState)
        assertEquals("Dune Messiah", updated.book.title)
    }

    @Test
    fun showsNotFoundWhenBookMissingAndWhenItDisappears() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune")))
        val missing = viewModel(repo)
        missing.onAction(BookDetailAction.Load(42))
        advanceUntilIdle()
        assertIs<BookDetailContentState.NotFound>(missing.state.value.contentState)

        val vm = viewModel(repo)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()
        assertIs<BookDetailContentState.Content>(vm.state.value.contentState)
        repo.books.value = emptyList()
        advanceUntilIdle()
        assertIs<BookDetailContentState.NotFound>(vm.state.value.contentState)
    }

    @Test
    fun showsErrorWhenObservationFailsAndRetryRecovers() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune")))
        repo.observeBookError = IllegalStateException("db")
        val vm = viewModel(repo)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()
        assertIs<BookDetailContentState.Error>(vm.state.value.contentState)

        repo.observeBookError = null
        vm.onAction(BookDetailAction.Retry)
        advanceUntilIdle()
        assertIs<BookDetailContentState.Content>(vm.state.value.contentState)
    }

    @Test
    fun editEmitsNavigateToEditWithBookId() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository(listOf(book(7, "Dune"))))
        vm.onAction(BookDetailAction.Load(7))
        advanceUntilIdle()
        vm.eventFlow.test {
            vm.onAction(BookDetailAction.Edit)
            advanceUntilIdle()
            assertEquals(BookDetailEvent.NavigateToEdit(7), awaitItem())
        }
    }

    @Test
    fun backEmitsNavigateBack() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.eventFlow.test {
            vm.onAction(BookDetailAction.Back)
            advanceUntilIdle()
            assertEquals(BookDetailEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun detailCarriesRatingAndNotesForFinishedBook() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune", status = BookStatus.Finished, rating = 5, notes = "  \n ")))
        val vm = viewModel(repo)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()
        val ui = assertIs<BookDetailContentState.Content>(vm.state.value.contentState).book
        assertEquals(5, ui.rating)
        // Whitespace-only notes are passed through; the view hides the section when blank.
        assertEquals(true, ui.notes.isBlank())

        repo.books.value = listOf(book(1, "Dune", status = BookStatus.Reading, rating = 5, notes = "good"))
        advanceUntilIdle()
        val reading = assertIs<BookDetailContentState.Content>(vm.state.value.contentState).book
        assertEquals(null, reading.rating)
        assertEquals("good", reading.notes)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loaded(repo: FakeBookRepository): BookDetailViewModel {
        val vm = viewModel(repo)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()
        return vm
    }

    @Test
    fun requestShowsDialogAndCancelClosesItWithBookUnchanged() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune")))
        val vm = loaded(repo)
        vm.onAction(BookDetailAction.RequestDelete)
        advanceUntilIdle()
        assertTrue(vm.state.value.isDeleteDialogVisible)
        vm.onAction(BookDetailAction.CancelDelete)
        advanceUntilIdle()
        assertFalse(vm.state.value.isDeleteDialogVisible)
        assertEquals(1, repo.books.value.size)
        assertIs<BookDetailContentState.Content>(vm.state.value.contentState)
    }

    @Test
    fun confirmWithoutRequestDoesNothing() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune")))
        val vm = loaded(repo)
        vm.onAction(BookDetailAction.ConfirmDelete)
        advanceUntilIdle()
        assertEquals(1, repo.books.value.size)
    }

    @Test
    fun confirmDeletesNavigatesBackAndNeverFlashesNotFound() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune")))
        val vm = loaded(repo)
        vm.onAction(BookDetailAction.RequestDelete)
        advanceUntilIdle()
        vm.eventFlow.test {
            vm.onAction(BookDetailAction.ConfirmDelete)
            advanceUntilIdle()
            assertEquals(BookDetailEvent.NavigateBackAfterDelete, awaitItem())
        }
        assertTrue(repo.books.value.isEmpty())
        assertIs<BookDetailContentState.Content>(vm.state.value.contentState)
        assertFalse(vm.state.value.isDeleteDialogVisible)
        assertFalse(vm.state.value.hasDeleteError)
        assertEquals("Dune", announcer.pendingTitle.value)
    }

    @Test
    fun notFoundIsTreatedAsSuccess() = runTest(dispatcher) {
        // The book is already gone in storage, but the detail still shows it (stale observer): delete -> NotFound.
        val stale = FakeBookRepository(listOf(book(1, "Dune")))
        val repo = object : com.shelf.feature.book.domain.repository.BookRepository by stale {
            override suspend fun deleteBook(id: Long) =
                com.shelf.feature.book.domain.model.BookResult.Failure(com.shelf.feature.book.domain.model.BookError.NotFound)
        }
        val vm = BookDetailViewModel(ObserveBook(repo), BookPresentationMapper(), DeleteBook(repo), announcer)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()
        vm.onAction(BookDetailAction.RequestDelete)
        advanceUntilIdle()
        vm.eventFlow.test {
            vm.onAction(BookDetailAction.ConfirmDelete)
            advanceUntilIdle()
            assertEquals(BookDetailEvent.NavigateBackAfterDelete, awaitItem())
        }
        assertFalse(vm.state.value.hasDeleteError)
        assertEquals("Dune", announcer.pendingTitle.value)
    }

    @Test
    fun failureKeepsDetailOpenWithErrorThatClearsOnNextRequest() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Dune")))
        repo.deleteFailure = IllegalStateException("db")
        val vm = loaded(repo)
        vm.onAction(BookDetailAction.RequestDelete)
        vm.onAction(BookDetailAction.ConfirmDelete)
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue(s.hasDeleteError)
        assertFalse(s.isDeleting)
        assertFalse(s.isDeleteDialogVisible)
        assertIs<BookDetailContentState.Content>(s.contentState)
        assertEquals(1, repo.books.value.size)
        assertEquals(null, announcer.pendingTitle.value)

        repo.deleteFailure = null
        vm.onAction(BookDetailAction.RequestDelete)
        advanceUntilIdle()
        assertFalse(vm.state.value.hasDeleteError)
        assertTrue(vm.state.value.isDeleteDialogVisible)
        vm.onAction(BookDetailAction.ConfirmDelete)
        advanceUntilIdle()
        assertTrue(repo.books.value.isEmpty())
    }

    @Test
    fun doubleConfirmDeletesOnceAndEditDisabledFlagWhileDeleting() = runTest(dispatcher) {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        var calls = 0
        val base = FakeBookRepository(listOf(book(1, "Dune")))
        val repo = object : com.shelf.feature.book.domain.repository.BookRepository by base {
            override suspend fun deleteBook(id: Long): com.shelf.feature.book.domain.model.BookResult<Unit> {
                calls++
                gate.await()
                return base.deleteBook(id)
            }
        }
        val vm = BookDetailViewModel(ObserveBook(repo), BookPresentationMapper(), DeleteBook(repo), announcer)
        vm.onAction(BookDetailAction.Load(1))
        advanceUntilIdle()
        vm.onAction(BookDetailAction.RequestDelete)
        vm.onAction(BookDetailAction.ConfirmDelete)
        vm.onAction(BookDetailAction.ConfirmDelete)
        vm.onAction(BookDetailAction.CancelDelete)
        advanceUntilIdle()
        assertTrue(vm.state.value.isDeleting)
        assertTrue(vm.state.value.isDeleteDialogVisible)
        assertEquals(1, calls)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, calls)
        assertTrue(base.books.value.isEmpty())
    }

    @Test
    fun deleteIsIgnoredOutsideContentState() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookDetailAction.Load(9))
        advanceUntilIdle()
        vm.onAction(BookDetailAction.RequestDelete)
        advanceUntilIdle()
        assertFalse(vm.state.value.isDeleteDialogVisible)
    }
}
