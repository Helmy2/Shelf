import Foundation
import KotlinModules

/// Failure kinds the form needs to distinguish.
enum BookBridgeFailure: Error, Equatable {
    case titleRequired
    case notFound
    case ratingOutOfRange
    case other
}

protocol BookRepositoryBridge {
    func loadBooks() async throws -> [BookItemSwift]
    func addBook(title: String, author: String, status: BookStatus, rating: Int?, notes: String) async throws -> BookItemSwift
    /// Returns nil when the book does not exist.
    func loadBook(id: Int64) async throws -> BookItemSwift?
    func updateBook(id: Int64, title: String, author: String, status: BookStatus, rating: Int?, notes: String) async throws -> BookItemSwift
    /// Throws BookBridgeFailure.notFound when the book does not exist.
    func deleteBook(id: Int64) async throws
}

final class BookBridge: BookRepositoryBridge {
    let repository: BookRepository

    init() {
        repository = BookRepositoryFactoryKt.iosBookRepository()
    }

    func loadBooks() async throws -> [BookItemSwift] {
        let kotlinBooks = try await BookRepositoryExtensionsKt.loadBooksOrThrow(repository)
        return kotlinBooks.map { mapFromKotlin($0) }
    }

    func addBook(title: String, author: String, status: BookStatus, rating: Int?, notes: String) async throws -> BookItemSwift {
        do {
            let created = try await BookRepositoryExtensionsKt.addBookOrThrow(
                repository,
                title: title,
                author: author,
                status: status.toKotlinStatus(),
                rating: rating.map { KotlinInt(int: Int32($0)) },
                notes: notes
            )
            return mapFromKotlin(created)
        } catch {
            throw Self.translate(error)
        }
    }

    func loadBook(id: Int64) async throws -> BookItemSwift? {
        let book = try await BookRepositoryExtensionsKt.loadBookOrThrow(repository, id: id)
        return book.map { mapFromKotlin($0) }
    }

    func updateBook(id: Int64, title: String, author: String, status: BookStatus, rating: Int?, notes: String) async throws -> BookItemSwift {
        do {
            let updated = try await BookRepositoryExtensionsKt.updateBookOrThrow(
                repository,
                id: id,
                title: title,
                author: author,
                status: status.toKotlinStatus(),
                rating: rating.map { KotlinInt(int: Int32($0)) },
                notes: notes
            )
            return mapFromKotlin(updated)
        } catch {
            throw Self.translate(error)
        }
    }

    func deleteBook(id: Int64) async throws {
        do {
            try await BookRepositoryExtensionsKt.deleteBookOrThrow(repository, id: id)
        } catch {
            throw Self.translate(error)
        }
    }

    private static func translate(_ error: Error) -> Error {
        let nsError = error as NSError
        if let exception = nsError.userInfo["KotlinException"] as? BookBridgeException {
            if exception.kind is BookErrorTitleRequired { return BookBridgeFailure.titleRequired }
            if exception.kind is BookErrorNotFound { return BookBridgeFailure.notFound }
            if exception.kind is BookErrorRatingOutOfRange { return BookBridgeFailure.ratingOutOfRange }
            return BookBridgeFailure.other
        }
        return error
    }

    private func mapFromKotlin(_ item: KotlinModules.BookItem) -> BookItemSwift {
        BookItemSwift(
            id: item.id,
            title: item.title,
            author: item.author,
            status: item.status.asSwiftStatus,
            rating: item.rating.map { Int(truncating: $0) },
            notes: item.notes,
            createdAtMillis: item.createdAtMillis
        )
    }
}

extension BookStatus {
    func toKotlinStatus() -> KotlinModules.BookStatus {
        switch self {
        case .wantToRead: return KotlinModules.BookStatus.wanttoread
        case .reading: return KotlinModules.BookStatus.reading
        case .finished: return KotlinModules.BookStatus.finished
        }
    }
}

extension KotlinModules.BookStatus {
    var asSwiftStatus: BookStatus {
        switch self {
        case .wanttoread: return .wantToRead
        case .reading: return .reading
        case .finished: return .finished
        default: return .wantToRead
        }
    }
}
