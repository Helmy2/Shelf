package com.shelf.feature.book.impl

import app.cash.turbine.test
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.usecase.AddBook
import com.shelf.feature.book.domain.usecase.ObserveBook
import com.shelf.feature.book.domain.usecase.UpdateBook
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeBookRepository) =
        BookFormViewModel(AddBook(repo), UpdateBook(repo), ObserveBook(repo), BookPresentationMapper())

    @Test
    fun opensWithDefaultsAndTitleFocusRequested() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.Load(null))
        advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.isEdit)
        assertEquals(BookStatusUi.WantToRead, state.status)
        assertEquals("", state.title)
        assertTrue(state.titleFocusRequests > 0)
    }

    @Test
    fun blankTitleShowsValidationErrorWithoutSaving() = runTest(dispatcher) {
        val repo = FakeBookRepository()
        val vm = viewModel(repo)
        val focusBefore = vm.state.value.titleFocusRequests

        vm.onAction(BookFormAction.TitleChanged("   "))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        assertTrue(vm.state.value.titleError)
        assertFalse(vm.state.value.isSaving)
        assertEquals(focusBefore + 1, vm.state.value.titleFocusRequests)
        assertEquals(0, repo.addCount)
    }

    @Test
    fun typingNonBlankCharacterClearsTitleError() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()
        assertTrue(vm.state.value.titleError)

        vm.onAction(BookFormAction.TitleChanged(" "))
        advanceUntilIdle()
        assertTrue(vm.state.value.titleError)

        vm.onAction(BookFormAction.TitleChanged("D"))
        advanceUntilIdle()
        assertFalse(vm.state.value.titleError)
    }

    @Test
    fun saveSuccessPersistsDraftAndEmitsSaved() = runTest(dispatcher) {
        val repo = FakeBookRepository()
        val vm = viewModel(repo)

        vm.eventFlow.test {
            vm.onAction(BookFormAction.TitleChanged("  Dune "))
            vm.onAction(BookFormAction.AuthorChanged("Frank Herbert"))
            vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Reading))
            vm.onAction(BookFormAction.Save)
            advanceUntilIdle()

            assertEquals(BookFormEvent.Saved, awaitItem())
        }
        val saved = repo.books.value.single()
        assertEquals("Dune", saved.title)
        assertEquals("Frank Herbert", saved.author)
        assertEquals(BookStatus.Reading, saved.status)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun generalFailureKeepsEnteredDataAndShowsSaveError() = runTest(dispatcher) {
        val repo = FakeBookRepository().apply { addFailure = IllegalStateException("disk") }
        val vm = viewModel(repo)

        vm.onAction(BookFormAction.TitleChanged("Dune"))
        vm.onAction(BookFormAction.AuthorChanged("Frank"))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.saveError)
        assertFalse(state.isSaving)
        assertFalse(state.titleError)
        assertEquals("Dune", state.title)
        assertEquals("Frank", state.author)
    }

    @Test
    fun cancelEmitsCancelledEvent() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.eventFlow.test {
            vm.onAction(BookFormAction.Cancel)
            advanceUntilIdle()
            assertEquals(BookFormEvent.Cancelled, awaitItem())
        }
    }

    // --- reused instance (regression: stale form state) ---

    @Test
    fun addAfterCompletedAddShowsEmptyForm() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.Load(null))
        vm.onAction(BookFormAction.TitleChanged("Dune"))
        vm.onAction(BookFormAction.AuthorChanged("Herbert"))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        vm.onAction(BookFormAction.Load(null))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("", state.title)
        assertEquals("", state.author)
        assertFalse(state.isEdit)
        assertEquals(BookStatusUi.WantToRead, state.status)
        assertTrue(state.titleFocusRequests > 0)
    }

    @Test
    fun addAfterCancelledAddShowsEmptyForm() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.Load(null))
        vm.onAction(BookFormAction.TitleChanged("Dune"))
        vm.onAction(BookFormAction.Cancel)
        advanceUntilIdle()

        vm.onAction(BookFormAction.Load(null))
        advanceUntilIdle()

        assertEquals("", vm.state.value.title)
    }

    @Test
    fun addAfterEditShowsEmptyAddForm() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository(listOf(dune)))
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()
        assertEquals("Dune", vm.state.value.title)

        vm.onAction(BookFormAction.Load(null))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isEdit)
        assertNull(state.bookId)
        assertEquals("", state.title)
        assertEquals("", state.author)
        assertEquals(BookFormLoadState.Ready, state.loadState)
    }

    @Test
    fun editBookAThenEditBookBShowsBValues() = runTest(dispatcher) {
        val other = book(6, "Emma", "Austen", BookStatus.Finished)
        val vm = viewModel(FakeBookRepository(listOf(dune, other)))
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()
        assertEquals("Dune", vm.state.value.title)

        vm.onAction(BookFormAction.Load(6))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(6L, state.bookId)
        assertEquals("Emma", state.title)
        assertEquals("Austen", state.author)
        assertEquals(BookStatusUi.Finished, state.status)
    }

    @Test
    fun reloadingSameAddWithoutFinishingKeepsTypedText() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.Load(null))
        vm.onAction(BookFormAction.TitleChanged("Dun"))
        advanceUntilIdle()

        vm.onAction(BookFormAction.Load(null)) // recomposition or rotation
        advanceUntilIdle()

        assertEquals("Dun", vm.state.value.title)
    }

    // --- edit mode ---

    private val dune get() = book(5, "Dune", "Frank Herbert", BookStatus.Reading)

    @Test
    fun editShowsLoadingThenPrefillsWithFocus() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeBookRepository(listOf(dune)).apply { observeBookGate = gate }
        val vm = viewModel(repo)
        vm.state.test {
            assertEquals(BookFormLoadState.Ready, awaitItem().loadState)
            vm.onAction(BookFormAction.Load(5))
            assertEquals(BookFormLoadState.Loading, awaitItem().loadState)

            gate.complete(Unit)
            val state = awaitItem()
            assertEquals(BookFormLoadState.Ready, state.loadState)
            assertTrue(state.isEdit)
            assertEquals("Dune", state.title)
            assertEquals("Frank Herbert", state.author)
            assertEquals(BookStatusUi.Reading, state.status)
            assertTrue(state.titleFocusRequests > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editPrefillHappensOnceAndLaterEmissionsDoNotOverwriteTyping() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(dune))
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()
        vm.onAction(BookFormAction.TitleChanged("Dune Messiah"))
        advanceUntilIdle()

        repo.books.value = listOf(dune.copy(title = "Changed elsewhere"))
        vm.onAction(BookFormAction.Load(5)) // e.g. recomposition or rotation
        advanceUntilIdle()

        assertEquals("Dune Messiah", vm.state.value.title)
    }

    @Test
    fun editMissingBookShowsNotFoundAndSaveDoesNothing() = runTest(dispatcher) {
        val repo = FakeBookRepository()
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(99))
        advanceUntilIdle()
        assertEquals(BookFormLoadState.NotFound, vm.state.value.loadState)

        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()
        assertTrue(repo.books.value.isEmpty())
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun editLoadFailureShowsErrorAndRetryRecovers() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(dune)).apply { observeBookError = IllegalStateException("db") }
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()
        assertEquals(BookFormLoadState.Error, vm.state.value.loadState)

        repo.observeBookError = null
        vm.onAction(BookFormAction.RetryLoad)
        advanceUntilIdle()
        assertEquals(BookFormLoadState.Ready, vm.state.value.loadState)
        assertEquals("Dune", vm.state.value.title)
    }

    @Test
    fun editSaveUpdatesBookKeepingIdCreationTimeAndEmitsSaved() = runTest(dispatcher) {
        val original = dune.copy(rating = null, notes = "great")
        val repo = FakeBookRepository(listOf(original))
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()

        vm.eventFlow.test {
            vm.onAction(BookFormAction.TitleChanged("  Dune Messiah "))
            vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
            vm.onAction(BookFormAction.Save)
            advanceUntilIdle()
            assertEquals(BookFormEvent.Saved, awaitItem())
        }
        val saved = repo.books.value.single()
        assertEquals(5, saved.id)
        assertEquals(original.createdAtMillis, saved.createdAtMillis)
        assertEquals("Dune Messiah", saved.title)
        assertEquals(BookStatus.Finished, saved.status)
        assertNull(saved.rating) // returning to Finished starts unset; the old rating is not preserved
        assertEquals("great", saved.notes) // notes were prefilled and saved unchanged
        assertEquals(0, repo.addCount)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun editBlankTitleShowsValidationErrorAndDoesNotUpdate() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(dune))
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()
        val focusBefore = vm.state.value.titleFocusRequests

        vm.onAction(BookFormAction.TitleChanged("  "))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        assertTrue(vm.state.value.titleError)
        assertEquals(focusBefore + 1, vm.state.value.titleFocusRequests)
        assertEquals("Dune", repo.books.value.single().title)
    }

    @Test
    fun editGeneralFailureKeepsDataAndShowsSaveError() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(dune)).apply { updateFailure = IllegalStateException("disk") }
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()

        vm.onAction(BookFormAction.TitleChanged("Dune Messiah"))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.saveError)
        assertFalse(state.isSaving)
        assertEquals(BookFormLoadState.Ready, state.loadState)
        assertEquals("Dune Messiah", state.title)
    }

    @Test
    fun editBookDeletedWhileEditingShowsNotFoundOnSave() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(dune))
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(5))
        advanceUntilIdle()

        repo.books.value = emptyList()
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        assertEquals(BookFormLoadState.NotFound, vm.state.value.loadState)
        assertFalse(vm.state.value.isSaving)
    }

    // --- slice 3: rating and notes ---

    @Test
    fun ratingSelectorShownOnlyForFinished() {
        assertFalse(BookFormState(status = BookStatusUi.WantToRead).showRating)
        assertFalse(BookFormState(status = BookStatusUi.Reading).showRating)
        assertTrue(BookFormState(status = BookStatusUi.Finished).showRating)
    }

    @Test
    fun statusChangeAwayFromFinishedClearsRatingAndReturningStartsUnset() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        vm.onAction(BookFormAction.RatingChanged(4))
        advanceUntilIdle()
        assertEquals(4, vm.state.value.rating)

        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Reading))
        advanceUntilIdle()
        assertNull(vm.state.value.rating)

        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        advanceUntilIdle()
        assertNull(vm.state.value.rating)
    }

    @Test
    fun reselectingSameStatusKeepsRating() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        vm.onAction(BookFormAction.RatingChanged(3))
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        advanceUntilIdle()
        assertEquals(3, vm.state.value.rating)
    }

    @Test
    fun clearUnsetsRating() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        vm.onAction(BookFormAction.RatingChanged(2))
        vm.onAction(BookFormAction.RatingChanged(null))
        advanceUntilIdle()
        assertNull(vm.state.value.rating)
    }

    @Test
    fun addPersistsFinishedRatingAndNotes() = runTest(dispatcher) {
        val repo = FakeBookRepository()
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.TitleChanged("Dune"))
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        vm.onAction(BookFormAction.RatingChanged(5))
        vm.onAction(BookFormAction.NotesChanged("Line one\nLine two"))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        val saved = repo.books.value.single()
        assertEquals(5, saved.rating)
        assertEquals("Line one\nLine two", saved.notes)
    }

    @Test
    fun addNonFinishedNeverSavesRating() = runTest(dispatcher) {
        val repo = FakeBookRepository()
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.TitleChanged("Dune"))
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        vm.onAction(BookFormAction.RatingChanged(5))
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Reading))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()
        assertNull(repo.books.value.single().rating)
    }

    @Test
    fun editPrefillsRatingAndNotesAndSaveWritesFormValues() = runTest(dispatcher) {
        val finished = book(8, "Emma", "Austen", BookStatus.Finished, rating = 4, notes = "lovely")
        val repo = FakeBookRepository(listOf(finished))
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(8))
        advanceUntilIdle()
        assertEquals(4, vm.state.value.rating)
        assertEquals("lovely", vm.state.value.notes)

        vm.onAction(BookFormAction.RatingChanged(2))
        vm.onAction(BookFormAction.NotesChanged("changed my mind"))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        val saved = repo.books.value.single()
        assertEquals(2, saved.rating)
        assertEquals("changed my mind", saved.notes)
    }

    @Test
    fun editFinishedToReadingClearsStoredRating() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(8, "Emma", status = BookStatus.Finished, rating = 4)))
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.Load(8))
        advanceUntilIdle()
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Reading))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()
        assertNull(repo.books.value.single().rating)
    }

    @Test
    fun ratingOutOfRangeShowsInlineErrorKeepsDataAndClearsOnChange() = runTest(dispatcher) {
        val repo = FakeBookRepository()
        val vm = viewModel(repo)
        vm.onAction(BookFormAction.TitleChanged("Dune"))
        vm.onAction(BookFormAction.NotesChanged("keep me"))
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        vm.onAction(BookFormAction.RatingChanged(9))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.ratingError)
        assertFalse(state.saveError)
        assertFalse(state.isSaving)
        assertEquals("keep me", state.notes)
        assertEquals(0, repo.addCount)

        vm.onAction(BookFormAction.RatingChanged(3))
        advanceUntilIdle()
        assertFalse(vm.state.value.ratingError)
    }

    @Test
    fun titleErrorWinsOverRatingError() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.onAction(BookFormAction.StatusChanged(BookStatusUi.Finished))
        vm.onAction(BookFormAction.RatingChanged(9))
        vm.onAction(BookFormAction.Save)
        advanceUntilIdle()
        assertTrue(vm.state.value.titleError)
        assertFalse(vm.state.value.ratingError)
    }
}
