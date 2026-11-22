package com.shelf.feature.book.domain.model

data class BookItem(
    val id: Long,
    val title: String,
    val author: String,
    val status: BookStatus,
    val rating: Int?,
    val notes: String,
    val createdAtMillis: Long,
)

/**
 * Validates a book for add or update. Keeps id and createdAtMillis.
 * Rules (title is checked first, so [BookError.TitleRequired] wins over [BookError.RatingOutOfRange]):
 * title is trimmed and must be non-blank; author is trimmed; notes are stored as given;
 * rating is cleared to null unless status is Finished, and a Finished rating must be 1..5.
 */
fun BookItem.validated(): BookResult<BookItem> =
    when (val n = normalize(title, author, status, rating)) {
        is BookResult.Failure -> n
        is BookResult.Success -> BookResult.Success(copy(title = n.value.title, author = n.value.author, rating = n.value.rating))
    }

private const val MIN_RATING = 1
private const val MAX_RATING = 5

private data class Normalized(val title: String, val author: String, val rating: Int?)

/** Shared by [BookItem.validated] and [BookDraft.validated]: title is checked first, then the rating rule. */
private fun normalize(title: String, author: String, status: BookStatus, rating: Int?): BookResult<Normalized> {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) return BookResult.Failure(BookError.TitleRequired)
    val cleanRating = when {
        status != BookStatus.Finished -> null
        rating == null || rating in MIN_RATING..MAX_RATING -> rating
        else -> return BookResult.Failure(BookError.RatingOutOfRange)
    }
    return BookResult.Success(Normalized(cleanTitle, author.trim(), cleanRating))
}

enum class BookStatus {
    WantToRead,
    Reading,
    Finished,
}

/** User input for a new book, before it has an id or creation time. */
data class BookDraft(
    val title: String,
    val author: String = "",
    val status: BookStatus = BookStatus.WantToRead,
    val rating: Int? = null,
    val notes: String = "",
) {
    /** Same rules as [BookItem.validated]: title first, then rating normalization and range. */
    fun validated(): BookResult<BookDraft> =
        when (val n = normalize(title, author, status, rating)) {
            is BookResult.Failure -> n
            is BookResult.Success -> BookResult.Success(copy(title = n.value.title, author = n.value.author, rating = n.value.rating))
        }
}
