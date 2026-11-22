package com.shelf.feature.book.data.bridge

import com.shelf.feature.book.data.local.IosBookDatabaseFactory
import com.shelf.feature.book.data.local.createBookDatabase
import com.shelf.feature.book.data.repository.RoomBookRepository
import com.shelf.feature.book.domain.repository.BookRepository
import com.shelf.shared.core.domain.SystemTimeProvider

fun iosBookRepository(): BookRepository {
    val database = createBookDatabase(IosBookDatabaseFactory())
    return RoomBookRepository(
        dao = database.bookItemDao(),
        timeProvider = SystemTimeProvider,
    )
}
