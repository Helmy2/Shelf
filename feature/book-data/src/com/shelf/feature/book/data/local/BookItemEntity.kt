package com.shelf.feature.book.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_items")
data class BookItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val status: String,
    val rating: Int?,
    val notes: String,
    val createdAtMillis: Long,
)
