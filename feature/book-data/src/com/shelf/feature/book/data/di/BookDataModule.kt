package com.shelf.feature.book.data.di

import com.shelf.feature.book.data.local.BookDatabase
import com.shelf.feature.book.data.local.BookDatabaseFactory
import com.shelf.feature.book.data.local.BookItemDao
import com.shelf.feature.book.data.local.createBookDatabase
import com.shelf.feature.book.data.repository.RoomBookRepository
import com.shelf.feature.book.domain.repository.BookRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Data-layer wiring owned by the book feature. Needs `TimeProvider` from the shared core module.
 */
fun bookDataModule(bookDatabaseFactory: BookDatabaseFactory) = module {
    single { bookDatabaseFactory }
    single<BookDatabase> { createBookDatabase(get()) }
    single<BookItemDao> { get<BookDatabase>().bookItemDao() }
    singleOf(::RoomBookRepository).bind<BookRepository>()
}
