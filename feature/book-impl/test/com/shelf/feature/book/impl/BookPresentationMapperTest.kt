package com.shelf.feature.book.impl

import com.shelf.feature.book.domain.model.BookStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookPresentationMapperTest {
    private val mapper = BookPresentationMapper()

    @Test
    fun mapsItemToUiModel() {
        val ui = mapper.toItemUi(book(7, "Dune", "Frank Herbert", BookStatus.Reading))
        assertEquals(BookItemUi(7, "Dune", "Frank Herbert", BookStatusUi.Reading), ui)
    }

    @Test
    fun mapsEveryStatusBothWays() {
        BookStatus.entries.forEach { status ->
            val ui = mapper.toItemUi(book(1, "t", status = status)).status
            val back = mapper.toDraft(BookFormState(title = "t", status = ui)).status
            assertEquals(status, back)
        }
    }

    @Test
    fun draftKeepsRawTextForDomainValidation() {
        val draft = mapper.toDraft(BookFormState(title = "  Dune ", author = "", status = BookStatusUi.Finished))
        assertEquals("  Dune ", draft.title)
        assertEquals("", draft.author)
        assertEquals(BookStatus.Finished, draft.status)
    }

    @Test
    fun mapsItemToDetailUiModel() {
        val ui = mapper.toDetailUi(book(7, "Dune", "", BookStatus.Finished))
        assertEquals(BookDetailUi(7, "Dune", "", BookStatusUi.Finished), ui)
    }

    @Test
    fun mapsItemToPrefilledFormFields() {
        val form = mapper.toFormState(book(7, "Dune", "Frank Herbert", BookStatus.Reading))
        assertEquals("Dune", form.title)
        assertEquals("Frank Herbert", form.author)
        assertEquals(BookStatusUi.Reading, form.status)
        assertEquals(7L, form.bookId)
    }

    @Test
    fun updatedKeepsIdAndCreationTimeAndTakesRatingAndNotesFromForm() {
        val original = book(7, "Dune", "Frank", BookStatus.Reading).copy(createdAtMillis = 99)
        val updated = mapper.toUpdated(
            original,
            BookFormState(
                title = "Dune 2", author = "F. Herbert", status = BookStatusUi.Finished,
                rating = 4, notes = "n", bookId = 7,
            ),
        )
        assertEquals(
            original.copy(
                title = "Dune 2", author = "F. Herbert", status = BookStatus.Finished, rating = 4, notes = "n",
            ),
            updated,
        )
    }

    @Test
    fun draftCarriesRatingAndNotes() {
        val draft = mapper.toDraft(BookFormState(title = "t", status = BookStatusUi.Finished, rating = 3, notes = "x"))
        assertEquals(3, draft.rating)
        assertEquals("x", draft.notes)
    }

    @Test
    fun ratingShownOnlyForFinishedInItemAndDetail() {
        val finished = book(1, "A", status = BookStatus.Finished, rating = 4, notes = "n")
        assertEquals(4, mapper.toItemUi(finished).rating)
        assertEquals(4, mapper.toDetailUi(finished).rating)
        assertEquals("n", mapper.toDetailUi(finished).notes)

        val reading = book(2, "B", status = BookStatus.Reading, rating = 4)
        assertNull(mapper.toItemUi(reading).rating)
        assertNull(mapper.toDetailUi(reading).rating)
        assertNull(mapper.toItemUi(book(3, "C", status = BookStatus.Finished)).rating)
    }

    @Test
    fun formPrefillIncludesRatingAndNotes() {
        val form = mapper.toFormState(book(1, "A", status = BookStatus.Finished, rating = 5, notes = "great"))
        assertEquals(5, form.rating)
        assertEquals("great", form.notes)
    }
}
