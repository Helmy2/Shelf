package com.shelf.feature.book.impl

import app.cash.turbine.test
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.usecase.ObserveBookStats
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
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BookStatsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeBookRepository) = BookStatsViewModel(ObserveBookStats(repo), BookPresentationMapper())

    @Test
    fun startsInLoading() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        assertIs<BookStatsContentState.Loading>(vm.state.value.contentState)
        advanceUntilIdle()
    }

    @Test
    fun emptyWhenNoBooks() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        advanceUntilIdle()
        assertIs<BookStatsContentState.Empty>(vm.state.value.contentState)
    }

    @Test
    fun contentShowsCountsAndOneDecimalAverage() = runTest(dispatcher) {
        val repo = FakeBookRepository(
            listOf(
                book(1, "A", status = BookStatus.WantToRead),
                book(2, "B", status = BookStatus.Reading),
                book(3, "C", status = BookStatus.Reading),
                book(4, "D", status = BookStatus.Finished, rating = 4),
                book(5, "E", status = BookStatus.Finished, rating = 4),
                book(6, "F", status = BookStatus.Finished, rating = 5),
                book(7, "G", status = BookStatus.Finished),
            ),
        )
        val vm = viewModel(repo)
        advanceUntilIdle()
        val stats = assertIs<BookStatsContentState.Content>(vm.state.value.contentState).stats
        assertEquals(1, stats.wantToReadCount)
        assertEquals(2, stats.readingCount)
        assertEquals(4, stats.finishedCount)
        assertEquals(3, stats.ratedCount)
        assertEquals("4.3", stats.averageText)
    }

    // Parity fixture: keep in sync with iosApp/test/BookStatsTests.swift (testAverageTextParityFixture).
    // Ties (4.25) must round half up to "4.3" on both platforms.
    @Test
    fun averageTextParityFixtureRoundsHalfUp() = runTest(dispatcher) {
        val fixture = listOf(
            listOf(4, 5) to "4.5",
            listOf(4, 4, 4, 5) to "4.3",
            listOf(3, 4, 4) to "3.7",
            listOf(5) to "5.0",
        )
        for ((ratings, expected) in fixture) {
            val books = ratings.mapIndexed { i, r ->
                book(i + 1L, "B$i", status = BookStatus.Finished, rating = r)
            }
            val vm = viewModel(FakeBookRepository(books))
            advanceUntilIdle()
            val stats = assertIs<BookStatsContentState.Content>(vm.state.value.contentState).stats
            assertEquals(expected, stats.averageText, "ratings=$ratings")
        }
    }

    @Test
    fun noRatingsHasNullAverage() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository(listOf(book(1, "A", status = BookStatus.Finished))))
        advanceUntilIdle()
        val stats = assertIs<BookStatsContentState.Content>(vm.state.value.contentState).stats
        assertEquals(0, stats.ratedCount)
        assertNull(stats.averageText)
    }

    @Test
    fun updatesWhenBooksChange() = runTest(dispatcher) {
        val repo = FakeBookRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()
        repo.books.value = listOf(book(1, "A", status = BookStatus.Reading))
        advanceUntilIdle()
        val stats = assertIs<BookStatsContentState.Content>(vm.state.value.contentState).stats
        assertEquals(1, stats.readingCount)
    }

    @Test
    fun errorThenRetryRecovers() = runTest(dispatcher) {
        val repo = FakeBookRepository(listOf(book(1, "A")))
        repo.observeError = IllegalStateException("db")
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertIs<BookStatsContentState.Error>(vm.state.value.contentState)

        repo.observeError = null
        vm.onAction(BookStatsAction.Retry)
        advanceUntilIdle()
        assertIs<BookStatsContentState.Content>(vm.state.value.contentState)
    }

    @Test
    fun backAndAddEmitEvents() = runTest(dispatcher) {
        val vm = viewModel(FakeBookRepository())
        vm.eventFlow.test {
            vm.onAction(BookStatsAction.Back)
            advanceUntilIdle()
            assertEquals(BookStatsEvent.NavigateBack, awaitItem())
            vm.onAction(BookStatsAction.AddBook)
            advanceUntilIdle()
            assertEquals(BookStatsEvent.NavigateToAdd, awaitItem())
        }
    }
}
