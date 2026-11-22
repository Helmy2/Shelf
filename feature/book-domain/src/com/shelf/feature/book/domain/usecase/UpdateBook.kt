package com.shelf.feature.book.domain.usecase

import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.model.validated
import com.shelf.feature.book.domain.repository.BookRepository

class UpdateBook(private val repository: BookRepository) {
    suspend operator fun invoke(book: BookItem): BookResult<BookItem> = when (val valid = book.validated()) {
        is BookResult.Failure -> valid
        is BookResult.Success -> repository.updateBook(valid.value)
    }
}
