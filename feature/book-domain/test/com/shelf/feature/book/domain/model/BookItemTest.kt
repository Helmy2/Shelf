package com.shelf.feature.book.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BookItemTest {
    @Test
    fun blankTitleIsRejected() {
        val result = BookDraft(title = "   ").validated()

        assertEquals(BookError.TitleRequired, assertIs<BookResult.Failure>(result).error)
    }

    @Test
    fun emptyTitleIsRejected() {
        assertIs<BookResult.Failure>(BookDraft(title = "").validated())
    }

    @Test
    fun validDraftIsTrimmed() {
        val result = BookDraft(
            title = "  Dune ",
            author = " Frank Herbert ",
            status = BookStatus.Reading,
            notes = " great ",
        ).validated()

        val draft = assertIs<BookResult.Success<BookDraft>>(result).value
        assertEquals("Dune", draft.title)
        assertEquals("Frank Herbert", draft.author)
        assertEquals(" great ", draft.notes)
        assertEquals(BookStatus.Reading, draft.status)
    }

    @Test
    fun defaultsToWantToReadWithoutRating() {
        val draft = BookDraft(title = "Dune")

        assertEquals(BookStatus.WantToRead, draft.status)
        assertEquals(null, draft.rating)
    }

    private fun item(status: BookStatus, rating: Int?, title: String = "Dune") =
        BookItem(1, title, "", status, rating, "n", 10L)

    private fun BookResult<BookItem>.ok() = assertIs<BookResult.Success<BookItem>>(this).value
    private fun BookResult<*>.err() = assertIs<BookResult.Failure>(this).error

    @Test
    fun nonFinishedStatusClearsRating() {
        assertNull(item(BookStatus.Reading, 4).validated().ok().rating)
        assertNull(item(BookStatus.WantToRead, 99).validated().ok().rating)
        assertNull(BookDraft("Dune", status = BookStatus.Reading, rating = 3).validated().draft().rating)
    }

    private fun BookResult<BookDraft>.draft() = assertIs<BookResult.Success<BookDraft>>(this).value

    @Test
    fun finishedRatingOutOfRangeIsRejected() {
        assertEquals(BookError.RatingOutOfRange, item(BookStatus.Finished, 0).validated().err())
        assertEquals(BookError.RatingOutOfRange, item(BookStatus.Finished, 6).validated().err())
        assertEquals(
            BookError.RatingOutOfRange,
            BookDraft("Dune", status = BookStatus.Finished, rating = 6).validated().err(),
        )
    }

    @Test
    fun finishedRatingBoundsAndNullAreAccepted() {
        assertEquals(1, item(BookStatus.Finished, 1).validated().ok().rating)
        assertEquals(5, item(BookStatus.Finished, 5).validated().ok().rating)
        assertNull(item(BookStatus.Finished, null).validated().ok().rating)
        assertEquals(5, BookDraft("Dune", status = BookStatus.Finished, rating = 5).validated().draft().rating)
    }

    @Test
    fun titleErrorWinsOverRatingError() {
        assertEquals(BookError.TitleRequired, item(BookStatus.Finished, 9, title = " ").validated().err())
        assertEquals(
            BookError.TitleRequired,
            BookDraft(" ", status = BookStatus.Finished, rating = 9).validated().err(),
        )
    }
}
