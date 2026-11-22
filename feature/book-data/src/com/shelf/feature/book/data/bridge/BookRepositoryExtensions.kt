package com.shelf.feature.book.data.bridge

import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookError
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.feature.book.domain.repository.BookRepository
import kotlinx.coroutines.flow.first

/** Thrown by `*OrThrow` helpers; [kind] lets Swift distinguish validation errors. */
class BookBridgeException(val kind: BookError) : RuntimeException(kind.toString())

suspend fun BookRepository.loadBooksOrThrow(): List<BookItem> = observeBooks().first()

suspend fun BookRepository.addBookOrThrow(
    title: String,
    author: String,
    status: BookStatus,
    rating: Int?,
    notes: String,
): BookItem = when (
    val result = addBook(BookDraft(title = title, author = author, status = status, rating = rating, notes = notes))
) {
    is BookResult.Success -> result.value
    is BookResult.Failure -> throw BookBridgeException(result.error)
}

/** Returns the book with [id], or null when it does not exist. */
suspend fun BookRepository.loadBookOrThrow(id: Long): BookItem? = observeBook(id).first()

/** Updates title, author, status, rating and notes of book [id]; throws [BookBridgeException] on failure (e.g. NotFound). */
suspend fun BookRepository.updateBookOrThrow(
    id: Long,
    title: String,
    author: String,
    status: BookStatus,
    rating: Int?,
    notes: String,
): BookItem {
    val existing = observeBook(id).first() ?: throw BookBridgeException(BookError.NotFound)
    return when (val result = updateBook(existing.copy(title = title, author = author, status = status, rating = rating, notes = notes))) {
        is BookResult.Success -> result.value
        is BookResult.Failure -> throw BookBridgeException(result.error)
    }
}

/** Deletes book [id]; throws [BookBridgeException] on failure (kind == NotFound when it does not exist). */
suspend fun BookRepository.deleteBookOrThrow(id: Long) {
    when (val result = deleteBook(id)) {
        is BookResult.Success -> Unit
        is BookResult.Failure -> throw BookBridgeException(result.error)
    }
}
