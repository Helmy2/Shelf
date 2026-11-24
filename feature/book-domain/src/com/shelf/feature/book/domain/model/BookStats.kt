package com.shelf.feature.book.domain.model

/**
 * Read-only reading statistics. [countByStatus] always has an entry for every [BookStatus].
 * [averageRating] covers Finished books with a rating only; null when [ratedCount] is 0.
 */
data class BookStats(
    val countByStatus: Map<BookStatus, Int>,
    val ratedCount: Int,
    val averageRating: Double?,
)

fun computeBookStats(books: List<BookItem>): BookStats {
    val counts = BookStatus.entries.associateWith { status -> books.count { it.status == status } }
    val ratings = books.filter { it.status == BookStatus.Finished }.mapNotNull { it.rating }
    return BookStats(
        countByStatus = counts,
        ratedCount = ratings.size,
        averageRating = if (ratings.isEmpty()) null else ratings.sum().toDouble() / ratings.size,
    )
}
