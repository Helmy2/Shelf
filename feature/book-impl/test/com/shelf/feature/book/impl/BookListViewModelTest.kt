package com.shelf.feature.book.impl

import app.cash.turbine.test
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.usecase.ObserveBooks
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val announcer = BookDeleteAnnouncer()

    private fun viewModel(repo: FakeBookRepository) =
        BookListViewModel(ObserveBooks(repo), BookPresentationMapper(), announcer)

    @Test
    fun startsInLoadingBeforeFirstEmission() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        assertIs<BookListContentState.Loading>(vm.state.value.contentState)
        advanceUntilIdle()
    }

    @Test
    fun showsEmptyWhenNoBooks() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        advanceUntilIdle()
        assertIs<BookListContentState.Empty>(vm.state.value.contentState)
    }

    @Test
    fun showsContentAsUiModelsAndUpdatesOnChange() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(2, "Dune", "Frank Herbert"), book(1, "Emma")))
        val vm = viewModel(repo)
        advanceUntilIdle()

        val content = assertIs<BookListContentState.Content>(vm.state.value.contentState)
        assertEquals(listOf("Dune", "Emma"), content.items.map { it.title })

        repo.books.value = listOf(book(3, "Ulysses")) + repo.books.value
        advanceUntilIdle()
        val updated = assertIs<BookListContentState.Content>(vm.state.value.contentState)
        assertEquals("Ulysses", updated.items.first().title)
    }

    @Test
    fun showsErrorWhenObservationFailsAndRetryRecovers() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "Emma")))
        repo.observeError = IllegalStateException("db")
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertIs<BookListContentState.Error>(vm.state.value.contentState)

        repo.observeError = null
        vm.onAction(BookListAction.Retry)
        advanceUntilIdle()
        assertIs<BookListContentState.Content>(vm.state.value.contentState)
    }

    @Test
    fun openAddEmitsNavigateEvent() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.eventFlow.test {
            vm.onAction(BookListAction.OpenAdd)
            advanceUntilIdle()
            assertEquals(BookListEvent.NavigateToAdd, awaitItem())
        }
    }

    @Test
    fun openBookEmitsNavigateToDetailWithId() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository(listOf(book(3, "Emma"))))
        vm.eventFlow.test {
            vm.onAction(BookListAction.OpenBook(3))
            advanceUntilIdle()
            assertEquals(BookListEvent.NavigateToDetail(3), awaitItem())
        }
    }

    @Test
    fun rowsShowRatingOnlyForFinishedBooksWithRating() = runTest(dispatcher) {
        val repo = FakeBookRepository(
            listOf(
                book(1, "Rated", status = BookStatus.Finished, rating = 4),
                book(2, "Unrated", status = BookStatus.Finished),
                book(3, "Reading", status = BookStatus.Reading),
            ),
        )
        val vm = viewModel(repo)
        advanceUntilIdle()
        val items = assertIs<BookListContentState.Content>(vm.state.value.contentState).items
        assertEquals(listOf(4, null, null), items.map { it.rating })
    }

    private fun mixedRepo() = FakeBookRepository(
        listOf(
            book(5, "E", status = BookStatus.Reading),
            book(4, "D", status = BookStatus.Finished),
            book(3, "C", status = BookStatus.Reading),
            book(2, "B", status = BookStatus.WantToRead),
            book(1, "A", status = BookStatus.Reading),
        ),
    )

    private fun BookListViewModel.titles() =
        assertIs<BookListContentState.Content>(state.value.contentState).items.map { it.title }

    @Test
    fun defaultFilterIsAllWithAllRowsAndChipsVisible() = runTest(dispatcher) {
        val vm = viewModel(mixedRepo())
        advanceUntilIdle()
        assertEquals(BookListFilter.All, vm.state.value.selectedFilter)
        assertEquals(listOf("E", "D", "C", "B", "A"), vm.titles())
        assertEquals(5, assertIs<BookListContentState.Content>(vm.state.value.contentState).unfilteredCount)
        assertTrue(vm.state.value.isFilterVisible)
    }

    @Test
    fun selectFilterShowsOnlyMatchingStatusKeepingOrder() = runTest(dispatcher) {
        val vm = viewModel(mixedRepo())
        advanceUntilIdle()
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Reading))
        advanceUntilIdle()
        assertEquals(BookListFilter.Reading, vm.state.value.selectedFilter)
        assertEquals(listOf("E", "C", "A"), vm.titles())
        assertEquals(5, assertIs<BookListContentState.Content>(vm.state.value.contentState).unfilteredCount)

        vm.onAction(BookListAction.SelectFilter(BookListFilter.WantToRead))
        advanceUntilIdle()
        assertEquals(listOf("B"), vm.titles())
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Finished))
        advanceUntilIdle()
        assertEquals(listOf("D"), vm.titles())
        vm.onAction(BookListAction.SelectFilter(BookListFilter.All))
        advanceUntilIdle()
        assertEquals(listOf("E", "D", "C", "B", "A"), vm.titles())
    }

    @Test
    fun filterWithNoMatchKeepsChipsAndSelection() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository(listOf(book(1, "A", status = BookStatus.Reading))))
        advanceUntilIdle()
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Finished))
        advanceUntilIdle()
        val content = assertIs<BookListContentState.Content>(vm.state.value.contentState)
        assertTrue(content.items.isEmpty())
        assertEquals(1, content.unfilteredCount)
        assertEquals(BookListFilter.Finished, vm.state.value.selectedFilter)
        assertTrue(vm.state.value.isFilterVisible)
    }

    @Test
    fun noBooksAtAllIsEmptyWithChipsHidden() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        advanceUntilIdle()
        assertIs<BookListContentState.Empty>(vm.state.value.contentState)
        assertFalse(vm.state.value.isFilterVisible)
    }

    @Test
    fun selectingCurrentFilterIsNoOp() = runTest(dispatcher) {
        val vm = viewModel(mixedRepo())
        advanceUntilIdle()
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Reading))
        advanceUntilIdle()
        val before = vm.state.value
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Reading))
        advanceUntilIdle()
        assertEquals(before, vm.state.value)
    }

    @Test
    fun selectionKeptWhenBooksChange() = runTest(dispatcher) {
        val repo = mixedRepo()
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Reading))
        advanceUntilIdle()

        // Added book that does not match is not shown; no auto reset.
        repo.books.value = listOf(book(6, "F", status = BookStatus.Finished)) + repo.books.value
        advanceUntilIdle()
        assertEquals(BookListFilter.Reading, vm.state.value.selectedFilter)
        assertEquals(listOf("E", "C", "A"), vm.titles())

        // Status edit that no longer matches makes the row disappear.
        repo.books.value = repo.books.value.map { if (it.id == 3L) it.copy(status = BookStatus.Finished) else it }
        advanceUntilIdle()
        assertEquals(listOf("E", "A"), vm.titles())
        assertEquals(6, assertIs<BookListContentState.Content>(vm.state.value.contentState).unfilteredCount)
    }

    @Test
    fun selectionResetsToAllWhenShelfBecomesEmpty() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "A", status = BookStatus.Reading)))
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Reading))
        advanceUntilIdle()
        repo.books.value = emptyList()
        advanceUntilIdle()
        assertIs<BookListContentState.Empty>(vm.state.value.contentState)
        assertEquals(BookListFilter.All, vm.state.value.selectedFilter)

        repo.books.value = listOf(book(2, "B", status = BookStatus.Finished))
        advanceUntilIdle()
        assertEquals(listOf("B"), vm.titles())
    }

    @Test
    fun loadingAndErrorHideChips() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "A")))
        repo.observeError = IllegalStateException("db")
        val vm = viewModel(repo)
        assertFalse(vm.state.value.isFilterVisible)
        advanceUntilIdle()
        assertIs<BookListContentState.Error>(vm.state.value.contentState)
        assertFalse(vm.state.value.isFilterVisible)
    }

    @Test
    fun deletingLastBookEmptiesListAndResetsFilterViaRepository() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "A", status = BookStatus.Reading)))
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Reading))
        advanceUntilIdle()
        repo.deleteBook(1)
        advanceUntilIdle()
        assertIs<BookListContentState.Empty>(vm.state.value.contentState)
        assertEquals(BookListFilter.All, vm.state.value.selectedFilter)
        assertFalse(vm.state.value.isFilterVisible)
    }

    @Test
    fun deleteUnderActiveFilterKeepsFilterAndDropsRow() = runTest(dispatcher) {
        val repo = FakeBookRepository(
            listOf(book(2, "B", status = BookStatus.Reading), book(1, "A", status = BookStatus.Reading), book(3, "C")),
        )
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onAction(BookListAction.SelectFilter(BookListFilter.Reading))
        advanceUntilIdle()
        repo.deleteBook(2)
        advanceUntilIdle()
        assertEquals(BookListFilter.Reading, vm.state.value.selectedFilter)
        assertEquals(listOf("A"), vm.titles())
        val content = assertIs<BookListContentState.Content>(vm.state.value.contentState)
        assertEquals(2, content.unfilteredCount)
    }

    @Test
    fun deleteAnnouncementIsExposedOnceAndConsumed() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        assertEquals(null, vm.pendingDeleteTitle.value)
        announcer.post("Dune")
        assertEquals("Dune", vm.pendingDeleteTitle.value)
        vm.consumeDeleteAnnouncement("Dune")
        assertEquals(null, vm.pendingDeleteTitle.value)
    }
}
