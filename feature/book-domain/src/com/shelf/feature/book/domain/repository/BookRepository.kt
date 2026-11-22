package com.shelf.feature.book.domain.repository

import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    /** All books, newest first. */
    fun observeBooks(): Flow<List<BookItem>>

    /** Emits the book with [id], or null when it does not exist (or is removed later). */
    fun observeBook(id: Long): Flow<BookItem?>

    /**
     * Validates and persists [book], keeping its stored `createdAtMillis`.
     * Returns `NotFound` when the id does not exist, `TitleRequired` for a blank title.
     */
    suspend fun updateBook(book: BookItem): BookResult<BookItem>

    /** Deletes book [id]. Returns `NotFound` when the id does not exist. */
    suspend fun deleteBook(id: Long): BookResult<Unit>

    /** Validates the draft, stamps `createdAtMillis` and persists it. */
    suspend fun addBook(draft: BookDraft): BookResult<BookItem>
}
