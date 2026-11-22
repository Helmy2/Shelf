package com.shelf.feature.book.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import com.shelf.shared.core.data.database.iosDatabaseDirectory

class IosBookDatabaseFactory : BookDatabaseFactory {
    override fun createBuilder(): RoomDatabase.Builder<BookDatabase> {
        val databasePath = "${iosDatabaseDirectory()}/shelf_book.db"

        return Room.databaseBuilder<BookDatabase>(
            name = databasePath
        )
    }
}
