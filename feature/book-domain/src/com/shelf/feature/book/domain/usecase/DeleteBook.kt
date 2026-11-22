package com.shelf.feature.book.domain.usecase

import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.repository.BookRepository

class DeleteBook(private val repository: BookRepository) {
    suspend operator fun invoke(id: Long): BookResult<Unit> = repository.deleteBook(id)
}
