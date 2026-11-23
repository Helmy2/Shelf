package com.shelf.feature.book.domain.model

import com.shelf.feature.book.domain.repository.BookRepository
import com.shelf.feature.book.domain.usecase.ObserveBookStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun book(id: Long, status: BookStatus, rating: Int? = null) =
    BookItem(id, "T$id", "", status, rating, "", 0L)

private class FakeRepository(initial: List<BookItem>) : BookRepository {
    val books = MutableStateFlow(initial)
    override fun observeBooks() = books
    override fun observeBook(id: Long) = books.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun addBook(draft: BookDraft): BookResult<BookItem> = error("unused")
    override suspend fun updateBook(book: BookItem): BookResult<BookItem> = error("unused")
    override suspend fun deleteBook(id: Long): BookResult<Unit> = error("unused")
}

class BookStatsTest {
    @Test
    fun emptyListHasAllStatusesAtZeroAndNoAverage() {
        val s = computeBookStats(emptyList())
        assertEquals(BookStatus.entries.toSet(), s.countByStatus.keys)
        assertEquals(true, s.countByStatus.values.all { it == 0 })
        assertEquals(0, s.ratedCount)
        assertNull(s.averageRating)
    }

    @Test
    fun mixedStatusesAreCounted() {
        val s = computeBookStats(
            listOf(
                book(1, BookStatus.WantToRead), book(2, BookStatus.WantToRead),
                book(3, BookStatus.Reading), book(4, BookStatus.Finished, 3),
            ),
        )
        assertEquals(2, s.countByStatus[BookStatus.WantToRead])
        assertEquals(1, s.countByStatus[BookStatus.Reading])
        assertEquals(1, s.countByStatus[BookStatus.Finished])
    }

    @Test
    fun finishedUnratedIsExcludedFromAverage() {
        val s = computeBookStats(listOf(book(1, BookStatus.Finished), book(2, BookStatus.Finished, 4)))
        assertEquals(2, s.countByStatus[BookStatus.Finished])
        assertEquals(1, s.ratedCount)
        assertEquals(4.0, s.averageRating)
    }

    @Test
    fun finishedAllUnratedHasNullAverage() {
        val s = computeBookStats(listOf(book(1, BookStatus.Finished)))
        assertEquals(0, s.ratedCount)
        assertNull(s.averageRating)
    }

    @Test
    fun ratingOnNonFinishedIsIgnored() {
        val s = computeBookStats(listOf(book(1, BookStatus.Reading, 5), book(2, BookStatus.WantToRead, 1)))
        assertEquals(0, s.ratedCount)
        assertNull(s.averageRating)
    }

    @Test
    fun averageValueIsCorrect() {
        val s = computeBookStats(listOf(book(1, BookStatus.Finished, 4), book(2, BookStatus.Finished, 5)))
        assertEquals(2, s.ratedCount)
        assertEquals(4.5, s.averageRating)
    }

    @Test
    fun observeBookStatsEmitsAndReactsToChanges() = runTest {
        val repo = FakeRepository(emptyList())
        val useCase = ObserveBookStats(repo)
        assertEquals(0, useCase().first().ratedCount)
        repo.books.value = listOf(book(1, BookStatus.Finished, 2))
        val s = useCase().first()
        assertEquals(1, s.countByStatus[BookStatus.Finished])
        assertEquals(2.0, s.averageRating)
    }
}
