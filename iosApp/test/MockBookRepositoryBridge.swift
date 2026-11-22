import Foundation
@testable import app

final class MockBookRepositoryBridge: BookRepositoryBridge {
    var booksToReturn: [BookItemSwift] = []
    var loadBooksError: Error?
    var addBookError: Error?
    var addCallCount = 0
    var loadBookError: Error?
    var updateBookError: Error?
    var loadBookCallCount = 0
    var deleteBookError: Error?
    var deleteCallCount = 0
    var deleteGate: (() async -> Void)?
    var updateCallCount = 0
    var lastUpdated: (id: Int64, title: String, author: String, status: BookStatus, rating: Int?, notes: String)?
    var lastAdded: (title: String, author: String, status: BookStatus, rating: Int?, notes: String)?

    func loadBooks() async throws -> [BookItemSwift] {
        if let loadBooksError { throw loadBooksError }
        return booksToReturn
    }

    func addBook(title: String, author: String, status: BookStatus, rating: Int?, notes: String) async throws -> BookItemSwift {
        if let addBookError { throw addBookError }
        addCallCount += 1
        lastAdded = (title, author, status, rating, notes)
        let book = makeBook(id: Int64(addCallCount), title: title, author: author, status: status, rating: rating, notes: notes)
        booksToReturn.insert(book, at: 0)
        return book
    }

    func loadBook(id: Int64) async throws -> BookItemSwift? {
        loadBookCallCount += 1
        if let loadBookError { throw loadBookError }
        return booksToReturn.first { $0.id == id }
    }

    func updateBook(id: Int64, title: String, author: String, status: BookStatus, rating: Int?, notes: String) async throws -> BookItemSwift {
        if let updateBookError { throw updateBookError }
        updateCallCount += 1
        lastUpdated = (id, title, author, status, rating, notes)
        let updated = makeBook(id: id, title: title, author: author, status: status, rating: rating, notes: notes)
        if let index = booksToReturn.firstIndex(where: { $0.id == id }) {
            booksToReturn[index] = updated
        }
        return updated
    }

    func deleteBook(id: Int64) async throws {
        deleteCallCount += 1
        if let deleteGate { await deleteGate() }
        if let deleteBookError { throw deleteBookError }
        booksToReturn.removeAll { $0.id == id }
    }
}

struct MockBridgeError: LocalizedError {
    let errorDescription: String?
}

func makeBook(
    id: Int64 = 1,
    title: String = "Dune",
    author: String = "Frank Herbert",
    status: BookStatus = .wantToRead,
    rating: Int? = nil,
    notes: String = ""
) -> BookItemSwift {
    BookItemSwift(
        id: id,
        title: title,
        author: author,
        status: status,
        rating: rating,
        notes: notes,
        createdAtMillis: 100
    )
}
