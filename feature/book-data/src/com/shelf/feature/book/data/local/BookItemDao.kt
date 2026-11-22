package com.shelf.feature.book.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookItemDao {
    @Query("SELECT * FROM book_items ORDER BY createdAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<BookItemEntity>>

    @Query("SELECT * FROM book_items WHERE id = :id")
    fun observeById(id: Long): Flow<BookItemEntity?>

    @Query("SELECT * FROM book_items WHERE id = :id")
    suspend fun getById(id: Long): BookItemEntity?

    /** Returns the number of rows updated (0 when the id does not exist). */
    @Update
    suspend fun update(item: BookItemEntity): Int

    /** Returns the number of rows deleted (0 when the id does not exist). */
    @Query("DELETE FROM book_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Insert
    suspend fun insert(item: BookItemEntity): Long
}
