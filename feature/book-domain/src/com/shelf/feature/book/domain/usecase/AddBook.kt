package com.shelf.feature.book.domain.usecase

import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.repository.BookRepository

class AddBook(private val repository: BookRepository) {
    suspend operator fun invoke(draft: BookDraft): BookResult<BookItem> = when (val valid = draft.validated()) {
        is BookResult.Failure -> valid
        is BookResult.Success -> repository.addBook(valid.value)
    }
}
