package com.shelf.feature.book.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shelf.shared.core.data.database.androidDatabaseDirectory

class AndroidBookDatabaseFactory(
    private val context: Context
) : BookDatabaseFactory {
    override fun createBuilder(): RoomDatabase.Builder<BookDatabase> {
        val appContext = context.applicationContext
        val databasePath = "${androidDatabaseDirectory(appContext)}/shelf_book.db"

        return Room.databaseBuilder<BookDatabase>(
            context = appContext,
            name = databasePath
        )
    }
}
