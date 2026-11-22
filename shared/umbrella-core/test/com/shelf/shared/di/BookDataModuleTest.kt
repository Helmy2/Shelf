package com.shelf.shared.di

import androidx.room.RoomDatabase
import com.shelf.feature.book.data.di.bookDataModule
import com.shelf.feature.book.data.local.BookDatabase
import com.shelf.feature.book.data.local.BookDatabaseFactory
import com.shelf.shared.core.domain.TimeProvider
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

class BookDataModuleTest {
    @Test
    fun resolvesTimeProviderAndBookDatabaseFactory() {
        val testFactory = object : BookDatabaseFactory {
            override fun createBuilder(): RoomDatabase.Builder<BookDatabase> {
                error("The DI graph test should not eagerly create the Room database")
            }
        }

        val koin = koinApplication {
            modules(coreModule(), bookDataModule(testFactory))
        }.koin

        assertNotNull(koin.get<TimeProvider>())
        assertNotNull(koin.get<BookDatabaseFactory>())
    }
}
