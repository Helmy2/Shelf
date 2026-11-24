package com.shelf.feature.book.domain.usecase

import com.shelf.feature.book.domain.model.BookStats
import com.shelf.feature.book.domain.model.computeBookStats
import com.shelf.feature.book.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveBookStats(private val repository: BookRepository) {
    operator fun invoke(): Flow<BookStats> = repository.observeBooks().map(::computeBookStats)
}
