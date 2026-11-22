package com.shelf.feature.book.impl

import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookStatus

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
