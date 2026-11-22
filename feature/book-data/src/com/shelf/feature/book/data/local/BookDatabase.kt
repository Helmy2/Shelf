package com.shelf.feature.book.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.shelf.shared.core.data.buildBundledRoomDatabase

@Database(
    entities = [BookItemEntity::class],
    version = 1,
    exportSchema = true
)
@ConstructedBy(BookDatabaseConstructor::class)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookItemDao(): BookItemDao
}

@Suppress("KotlinNoActualForExpect")
expect object BookDatabaseConstructor : RoomDatabaseConstructor<BookDatabase> {
    override fun initialize(): BookDatabase
}

interface BookDatabaseFactory {
    fun createBuilder(): RoomDatabase.Builder<BookDatabase>
}

fun createBookDatabase(
    factory: BookDatabaseFactory,
    allowDestructiveMigration: Boolean = false,
): BookDatabase = factory
    .createBuilder()
    .buildBundledRoomDatabase(allowDestructiveMigration = allowDestructiveMigration)
