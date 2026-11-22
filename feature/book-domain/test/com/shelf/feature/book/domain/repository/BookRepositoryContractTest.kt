package com.shelf.feature.book.domain.repository

import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookError
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.usecase.AddBook
import com.shelf.feature.book.domain.usecase.DeleteBook
import com.shelf.feature.book.domain.usecase.ObserveBook
import com.shelf.feature.book.domain.usecase.ObserveBooks
import com.shelf.feature.book.domain.usecase.UpdateBook
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class InMemoryBookRepository : BookRepository {
    val books = MutableStateFlow<List<BookItem>>(emptyList())
    var addCalls = 0
    var updateCalls = 0

    override fun observeBooks() = books

    override fun observeBook(id: Long) = books.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun updateBook(book: BookItem): BookResult<BookItem> {
        updateCalls++
        val existing = books.value.firstOrNull { it.id == book.id }
            ?: return BookResult.Failure(BookError.NotFound)
        val updated = book.copy(createdAtMillis = existing.createdAtMillis)
        books.value = books.value.map { if (it.id == book.id) updated else it }
        return BookResult.Success(updated)
    }

    override suspend fun deleteBook(id: Long): BookResult<Unit> {
        if (books.value.none { it.id == id }) return BookResult.Failure(BookError.NotFound)
        books.value = books.value.filterNot { it.id == id }
        return BookResult.Success(Unit)
    }

    override suspend fun addBook(draft: BookDraft): BookResult<BookItem> {
        addCalls++
        val item = BookItem(
            id = addCalls.toLong(),
            title = draft.title,
            author = draft.author,
            status = draft.status,
            rating = draft.rating,
            notes = draft.notes,
            createdAtMillis = 100L,
        )
        books.value = listOf(item) + books.value
        return BookResult.Success(item)
    }
}

class BookRepositoryContractTest {
    @Test
    fun addBookUseCaseRejectsBlankTitleWithoutTouchingRepository() = runTest {
        val repository = InMemoryBookRepository()

        val result = AddBook(repository)(BookDraft(title = " "))

        assertEquals(BookError.TitleRequired, assertIs<BookResult.Failure>(result).error)
        assertEquals(0, repository.addCalls)
    }

    @Test
    fun addedBookAppearsInObserveBooks() = runTest {
        val repository = InMemoryBookRepository()

        val added = assertIs<BookResult.Success<BookItem>>(
            AddBook(repository)(BookDraft(title = " Dune ", status = BookStatus.Reading))
        ).value

        val listed = ObserveBooks(repository)().first()
        assertEquals(listOf(added), listed)
        assertEquals("Dune", added.title)
    }

    private suspend fun seeded(): Pair<InMemoryBookRepository, BookItem> {
        val repository = InMemoryBookRepository()
        val added = assertIs<BookResult.Success<BookItem>>(AddBook(repository)(BookDraft("Dune"))).value
        return repository to added
    }

    @Test
    fun updateBookUseCaseRejectsBlankTitleWithoutTouchingRepository() = runTest {
        val (repository, added) = seeded()

        val result = UpdateBook(repository)(added.copy(title = "  "))

        assertEquals(BookError.TitleRequired, assertIs<BookResult.Failure>(result).error)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun updateBookTrimsKeepsIdAndCreatedAt() = runTest {
        val (repository, added) = seeded()

        val updated = assertIs<BookResult.Success<BookItem>>(
            UpdateBook(repository)(added.copy(title = " Dune Messiah ", status = BookStatus.Finished, createdAtMillis = 1L))
        ).value

        assertEquals("Dune Messiah", updated.title)
        assertEquals(added.id, updated.id)
        assertEquals(added.createdAtMillis, updated.createdAtMillis)
        assertEquals(updated, ObserveBook(repository)(added.id).first())
    }

    @Test
    fun updateMissingBookReturnsNotFound() = runTest {
        val (repository, added) = seeded()

        val result = UpdateBook(repository)(added.copy(id = 99L))

        assertEquals(BookError.NotFound, assertIs<BookResult.Failure>(result).error)
    }

    @Test
    fun observeBookEmitsNullForMissingId() = runTest {
        val (repository, _) = seeded()

        assertNull(ObserveBook(repository)(99L).first())
    }

    @Test
    fun deleteBookRemovesItFromObserveBooksAndObserveBook() = runTest {
        val (repository, added) = seeded()
        val other = assertIs<BookResult.Success<BookItem>>(AddBook(repository)(BookDraft("Emma"))).value

        assertIs<BookResult.Success<Unit>>(DeleteBook(repository)(added.id))

        assertEquals(listOf(other), ObserveBooks(repository)().first())
        assertNull(ObserveBook(repository)(added.id).first())
    }

    @Test
    fun deleteMissingBookReturnsNotFound() = runTest {
        val (repository, _) = seeded()

        val result = DeleteBook(repository)(99L)

        assertEquals(BookError.NotFound, assertIs<BookResult.Failure>(result).error)
    }

    @Test
    fun deletingTheLastBookYieldsEmptyList() = runTest {
        val (repository, added) = seeded()

        DeleteBook(repository)(added.id)

        assertEquals(emptyList(), ObserveBooks(repository)().first())
    }
}
