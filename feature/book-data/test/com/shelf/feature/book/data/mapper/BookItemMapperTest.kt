package com.shelf.feature.book.data.mapper

import com.shelf.feature.book.data.local.BookItemEntity
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookItemMapperTest {
    @Test
    fun mapsEntityToDomain() {
        val item = BookItemEntity(1, "Dune", "Frank Herbert", "Finished", 5, "great", 100L).toDomain()

        assertEquals(BookItem(1, "Dune", "Frank Herbert", BookStatus.Finished, 5, "great", 100L), item)
    }

    @Test
    fun unknownStatusFallsBackToWantToRead() {
        val item = BookItemEntity(2, "Old", "", "Legacy", null, "", 1L).toDomain()

        assertEquals(BookStatus.WantToRead, item.status)
        assertNull(item.rating)
    }

    @Test
    fun roundTripPreservesIdAndCreatedAt() {
        val item = BookItem(9, "Emma", "Austen", BookStatus.Finished, 4, "n", 777L)

        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun mapsDomainToEntity() {
        val entity = BookItem(3, "Emma", "Austen", BookStatus.Reading, null, "n", 300L).toEntity()

        assertEquals(BookItemEntity(3, "Emma", "Austen", "Reading", null, "n", 300L), entity)
    }
}
