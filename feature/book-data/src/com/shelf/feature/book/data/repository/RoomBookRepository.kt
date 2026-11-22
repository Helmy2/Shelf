package com.shelf.feature.book.data.repository

import com.shelf.feature.book.data.local.BookItemDao
import com.shelf.feature.book.data.mapper.toDomain
import com.shelf.feature.book.data.mapper.toEntity
import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookError
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.model.validated
import com.shelf.feature.book.domain.repository.BookRepository
import com.shelf.shared.core.domain.TimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomBookRepository(
    private val dao: BookItemDao,
    private val timeProvider: TimeProvider,
) : BookRepository {
    override fun observeBooks(): Flow<List<BookItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeBook(id: Long): Flow<BookItem?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun updateBook(book: BookItem): BookResult<BookItem> {
        val valid = when (val result = book.validated()) {
            is BookResult.Failure -> return result
            is BookResult.Success -> result.value
        }
        return try {
            val existing = dao.getById(valid.id) ?: return BookResult.Failure(BookError.NotFound)
            val updated = valid.copy(createdAtMillis = existing.createdAtMillis)
            if (dao.update(updated.toEntity()) == 0) {
                BookResult.Failure(BookError.NotFound)
            } else {
                BookResult.Success(updated)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            BookResult.Failure(BookError.Unknown)
        }
    }

    override suspend fun deleteBook(id: Long): BookResult<Unit> = try {
        if (dao.deleteById(id) == 0) BookResult.Failure(BookError.NotFound) else BookResult.Success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        BookResult.Failure(BookError.Unknown)
    }

    override suspend fun addBook(draft: BookDraft): BookResult<BookItem> {
        val valid = when (val result = draft.validated()) {
            is BookResult.Failure -> return result
            is BookResult.Success -> result.value
        }
        return try {
            val item = BookItem(
                id = 0,
                title = valid.title,
                author = valid.author,
                status = valid.status,
                rating = valid.rating,
                notes = valid.notes,
                createdAtMillis = timeProvider.nowMillis(),
            )
            val id = dao.insert(item.toEntity())
            BookResult.Success(item.copy(id = id))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            BookResult.Failure(BookError.Unknown)
        }
    }
}
