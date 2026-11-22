package com.shelf.feature.book.impl

import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookError
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun book(
    id: Long,
    title: String,
    author: String = "",
    status: BookStatus = BookStatus.WantToRead,
    rating: Int? = null,
    notes: String = "",
) = BookItem(id, title, author, status, rating = rating, notes = notes, createdAtMillis = id)

class FakeBookRepository(initial: List<BookItem> = emptyList()) : BookRepository {
    val books = MutableStateFlow(initial)
    var observeError: Throwable? = null
    var addFailure: Throwable? = null
    var updateFailure: Throwable? = null
    var deleteFailure: Throwable? = null
    var observeBookError: Throwable? = null
    /** When set, observeBook suspends until this is completed. */
    var observeBookGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    var addCount = 0

    override fun observeBooks(): Flow<List<BookItem>> {
        val error = observeError
        return if (error != null) flow { throw error } else books
    }

    override fun observeBook(id: Long): Flow<BookItem?> {
        val error = observeBookError
        return if (error != null) flow { throw error } else books.map { list -> list.firstOrNull { it.id == id } }.onStart { observeBookGate?.await() }
    }

    override suspend fun updateBook(book: BookItem): BookResult<BookItem> {
        updateFailure?.let { throw it }
        val existing = books.value.firstOrNull { it.id == book.id }
            ?: return BookResult.Failure(BookError.NotFound)
        val updated = book.copy(createdAtMillis = existing.createdAtMillis)
        books.value = books.value.map { if (it.id == book.id) updated else it }
        return BookResult.Success(updated)
    }

    override suspend fun deleteBook(id: Long): BookResult<Unit> {
        deleteFailure?.let { throw it }
        if (books.value.none { it.id == id }) return BookResult.Failure(BookError.NotFound)
        books.value = books.value.filterNot { it.id == id }
        return BookResult.Success(Unit)
    }

    override suspend fun addBook(draft: BookDraft): BookResult<BookItem> {
        addFailure?.let { throw it }
        addCount++
        val item = BookItem(
            id = addCount.toLong(), title = draft.title, author = draft.author, status = draft.status,
            rating = draft.rating, notes = draft.notes, createdAtMillis = addCount.toLong(),
        )
        books.value = listOf(item) + books.value
        return BookResult.Success(item)
    }
}
