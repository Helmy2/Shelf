package com.shelf.feature.book.data.mapper

import com.shelf.feature.book.data.local.BookItemEntity
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookStatus

fun BookItemEntity.toDomain(): BookItem = BookItem(
    id = id,
    title = title,
    author = author,
    status = status.toBookStatus(),
    rating = rating,
    notes = notes,
    createdAtMillis = createdAtMillis,
)

fun BookItem.toEntity(): BookItemEntity = BookItemEntity(
    id = id,
    title = title,
    author = author,
    status = status.name,
    rating = rating,
    notes = notes,
    createdAtMillis = createdAtMillis,
)

private fun String.toBookStatus(): BookStatus = BookStatus.entries.firstOrNull { it.name == this }
    ?: BookStatus.WantToRead
