package com.shelf.feature.book.domain.usecase

import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow

class ObserveBook(private val repository: BookRepository) {
    operator fun invoke(id: Long): Flow<BookItem?> = repository.observeBook(id)
}
