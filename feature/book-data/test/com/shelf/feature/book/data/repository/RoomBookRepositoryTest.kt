package com.shelf.feature.book.data.repository

import com.shelf.feature.book.data.local.BookItemDao
import com.shelf.feature.book.data.local.BookItemEntity
import com.shelf.feature.book.domain.model.BookDraft
import com.shelf.feature.book.domain.model.BookError
import com.shelf.feature.book.domain.model.BookItem
import com.shelf.feature.book.domain.model.BookResult
import com.shelf.feature.book.domain.model.BookStatus
import com.shelf.shared.core.testing.FakeTimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomBookRepositoryTest {
    @Test
    fun addBookStampsCurrentTimeAndTrims() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider(current = 500L))

        val created = assertIs<BookResult.Success<BookItem>>(
            repository.addBook(BookDraft(" Dune ", " Herbert ", BookStatus.Reading))
        ).value

        assertEquals(1L, created.id)
        assertEquals("Dune", created.title)
        assertEquals("Herbert", created.author)
        assertEquals(BookStatus.Reading, created.status)
        assertEquals(500L, created.createdAtMillis)
        assertEquals("Reading", dao.rows.value.single().status)
    }

    @Test
    fun blankTitleIsRejectedAndNotPersisted() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())

        val result = repository.addBook(BookDraft("   "))

        assertEquals(BookError.TitleRequired, assertIs<BookResult.Failure>(result).error)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun observeBooksEmitsNewestFirst() = runTest {
        val time = FakeTimeProvider(current = 100L)
        val repository = RoomBookRepository(FakeBookItemDao(), time)

        repository.addBook(BookDraft("First"))
        time.setNowMillis(200L)
        repository.addBook(BookDraft("Second"))

        assertEquals(listOf("Second", "First"), repository.observeBooks().first().map { it.title })
    }

    @Test
    fun daoFailureBecomesUnknownError() = runTest {
        val dao = FakeBookItemDao(failInsert = true)
        val repository = RoomBookRepository(dao, FakeTimeProvider())

        val result = repository.addBook(BookDraft("Dune"))

        assertEquals(BookError.Unknown, assertIs<BookResult.Failure>(result).error)
    }

    private suspend fun RoomBookRepository.seed(): BookItem =
        assertIs<BookResult.Success<BookItem>>(addBook(BookDraft("Dune", "Herbert"))).value

    @Test
    fun updateBookPersistsChangesAndPreservesIdAndCreatedAt() = runTest {
        val time = FakeTimeProvider(current = 100L)
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, time)
        val created = repository.seed()
        time.setNowMillis(900L)

        val updated = assertIs<BookResult.Success<BookItem>>(
            repository.updateBook(created.copy(title = " Dune Messiah ", status = BookStatus.Finished, createdAtMillis = 5L))
        ).value

        assertEquals(created.id, updated.id)
        assertEquals("Dune Messiah", updated.title)
        assertEquals(100L, updated.createdAtMillis)
        assertEquals(updated, repository.observeBook(created.id).first())
        assertEquals("Finished", dao.rows.value.single().status)
        assertEquals(100L, dao.rows.value.single().createdAtMillis)
    }

    @Test
    fun updateBookWithBlankTitleIsRejectedAndNotPersisted() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())
        val created = repository.seed()

        val result = repository.updateBook(created.copy(title = "  "))

        assertEquals(BookError.TitleRequired, assertIs<BookResult.Failure>(result).error)
        assertEquals("Dune", dao.rows.value.single().title)
    }

    @Test
    fun updateBookWithMissingIdReturnsNotFound() = runTest {
        val repository = RoomBookRepository(FakeBookItemDao(), FakeTimeProvider())
        val created = repository.seed()

        val result = repository.updateBook(created.copy(id = 42L))

        assertEquals(BookError.NotFound, assertIs<BookResult.Failure>(result).error)
    }

    @Test
    fun addBookPersistsRatingAndNotes() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())

        val created = assertIs<BookResult.Success<BookItem>>(
            repository.addBook(BookDraft("Dune", status = BookStatus.Finished, rating = 4, notes = "classic"))
        ).value

        assertEquals(4, created.rating)
        assertEquals(4, dao.rows.value.single().rating)
        assertEquals("classic", dao.rows.value.single().notes)
    }

    @Test
    fun addBookWithInvalidRatingIsRejectedAndNotPersisted() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())

        val result = repository.addBook(BookDraft("Dune", status = BookStatus.Finished, rating = 6))

        assertEquals(BookError.RatingOutOfRange, assertIs<BookResult.Failure>(result).error)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun updateBookPersistsRatingAndNotesAndClearsRatingWhenNotFinished() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())
        val created = repository.seed()

        repository.updateBook(created.copy(status = BookStatus.Finished, rating = 5, notes = "loved it"))
        assertEquals(5, dao.rows.value.single().rating)
        assertEquals("loved it", dao.rows.value.single().notes)

        val back = assertIs<BookResult.Success<BookItem>>(
            repository.updateBook(created.copy(status = BookStatus.Reading, rating = 5, notes = "again"))
        ).value
        assertNull(back.rating)
        assertNull(dao.rows.value.single().rating)
        assertEquals("again", dao.rows.value.single().notes)
    }

    @Test
    fun updateBookWithInvalidRatingIsRejectedAndNotPersisted() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())
        val created = repository.seed()

        val result = repository.updateBook(created.copy(status = BookStatus.Finished, rating = 0))

        assertEquals(BookError.RatingOutOfRange, assertIs<BookResult.Failure>(result).error)
        assertEquals("WantToRead", dao.rows.value.single().status)
    }

    @Test
    fun deleteBookRemovesRowAndEmitsNull() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())
        val created = repository.seed()
        val other = assertIs<BookResult.Success<BookItem>>(repository.addBook(BookDraft("Emma"))).value

        assertIs<BookResult.Success<Unit>>(repository.deleteBook(created.id))

        assertEquals(listOf(other.id), dao.rows.value.map { it.id })
        assertNull(repository.observeBook(created.id).first())
        assertEquals(listOf(other), repository.observeBooks().first())
    }

    @Test
    fun deletingLastBookYieldsEmptyList() = runTest {
        val repository = RoomBookRepository(FakeBookItemDao(), FakeTimeProvider())
        val created = repository.seed()

        repository.deleteBook(created.id)

        assertTrue(repository.observeBooks().first().isEmpty())
    }

    @Test
    fun deleteBookWithMissingIdReturnsNotFound() = runTest {
        val dao = FakeBookItemDao()
        val repository = RoomBookRepository(dao, FakeTimeProvider())
        repository.seed()

        val result = repository.deleteBook(42L)

        assertEquals(BookError.NotFound, assertIs<BookResult.Failure>(result).error)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun deleteBookDaoFailureBecomesUnknownError() = runTest {
        val dao = FakeBookItemDao(failDelete = true)
        val repository = RoomBookRepository(dao, FakeTimeProvider())
        val created = repository.seed()

        val result = repository.deleteBook(created.id)

        assertEquals(BookError.Unknown, assertIs<BookResult.Failure>(result).error)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun deleteBookRethrowsCancellation() = runTest {
        val repository = RoomBookRepository(FakeBookItemDao(cancelDelete = true), FakeTimeProvider())

        assertFailsWith<CancellationException> { repository.deleteBook(1L) }
    }

    @Test
    fun observeBookEmitsNullForMissingId() = runTest {
        val repository = RoomBookRepository(FakeBookItemDao(), FakeTimeProvider())

        assertNull(repository.observeBook(7L).first())
    }
}

private class FakeBookItemDao(
    private val failInsert: Boolean = false,
    private val failDelete: Boolean = false,
    private val cancelDelete: Boolean = false,
) : BookItemDao {
    val rows = MutableStateFlow<List<BookItemEntity>>(emptyList())

    override fun observeAll(): Flow<List<BookItemEntity>> = rows.map { list ->
        list.sortedWith(compareByDescending<BookItemEntity> { it.createdAtMillis }.thenByDescending { it.id })
    }

    override fun observeById(id: Long): Flow<BookItemEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): BookItemEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun update(item: BookItemEntity): Int {
        if (rows.value.none { it.id == item.id }) return 0
        rows.value = rows.value.map { if (it.id == item.id) item else it }
        return 1
    }

    override suspend fun deleteById(id: Long): Int {
        if (cancelDelete) throw CancellationException("cancelled")
        if (failDelete) error("delete failed")
        val before = rows.value.size
        rows.value = rows.value.filterNot { it.id == id }
        return before - rows.value.size
    }

    override suspend fun insert(item: BookItemEntity): Long {
        if (failInsert) error("insert failed")
        val id = rows.value.size + 1L
        rows.value = rows.value + item.copy(id = id)
        return id
    }
}
