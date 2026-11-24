package com.shelf.feature.book.impl

import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookStats
import com.shelf.feature.book.domain.model.BookStatus
import kotlin.math.roundToInt

class BookPresentationMapper {
    fun toItemUi(item: BookItem): BookItemUi = BookItemUi(
        id = item.id,
        title = item.title,
        author = item.author,
        status = item.status.toUi(),
        rating = item.visibleRating(),
    )

    fun toDetailUi(item: BookItem): BookDetailUi = BookDetailUi(
        id = item.id,
        title = item.title,
        author = item.author,
        status = item.status.toUi(),
        rating = item.visibleRating(),
        notes = item.notes,
    )

    /** Form fields for editing [item]; the caller merges them into the live form state. */
    fun toFormState(item: BookItem): BookFormState = BookFormState(
        title = item.title,
        author = item.author,
        status = item.status.toUi(),
        rating = item.visibleRating(),
        notes = item.notes,
        bookId = item.id,
    )

    /** Applies the form fields to [item], keeping id and creation time; rating and notes come from the form. */
    fun toUpdated(item: BookItem, form: BookFormState): BookItem = item.copy(
        title = form.title,
        author = form.author,
        status = form.status.toDomain(),
        rating = form.rating,
        notes = form.notes,
    )

    fun toDraft(form: BookFormState): BookDraft = BookDraft(
        title = form.title,
        author = form.author,
        status = form.status.toDomain(),
        rating = form.rating,
        notes = form.notes,
    )

    fun toStatsContentState(stats: BookStats): BookStatsContentState {
        val wantToRead = stats.countByStatus[BookStatus.WantToRead] ?: 0
        val reading = stats.countByStatus[BookStatus.Reading] ?: 0
        val finished = stats.countByStatus[BookStatus.Finished] ?: 0
        if (wantToRead == 0 && reading == 0 && finished == 0) return BookStatsContentState.Empty
        return BookStatsContentState.Content(
            BookStatsUi(
                wantToReadCount = wantToRead,
                readingCount = reading,
                finishedCount = finished,
                ratedCount = stats.ratedCount,
                averageText = stats.averageRating?.let(::formatOneDecimal),
            ),
        )
    }

    /**
     * Locale independent one-decimal formatting (e.g. 4.2), rounding half up, always with "." as the
     * decimal separator (common code cannot use String.format/java.text).
     */
    private fun formatOneDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        return "${tenths / 10}.${tenths % 10}"
    }

    /** Only a Finished book shows its rating. */
    private fun BookItem.visibleRating(): Int? = if (status == BookStatus.Finished) rating else null

    private fun BookStatus.toUi(): BookStatusUi = when (this) {
        BookStatus.WantToRead -> BookStatusUi.WantToRead
        BookStatus.Reading -> BookStatusUi.Reading
        BookStatus.Finished -> BookStatusUi.Finished
    }

    private fun BookStatusUi.toDomain(): BookStatus = when (this) {
        BookStatusUi.WantToRead -> BookStatus.WantToRead
        BookStatusUi.Reading -> BookStatus.Reading
        BookStatusUi.Finished -> BookStatus.Finished
    }
}
