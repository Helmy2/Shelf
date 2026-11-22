import Foundation

@Observable
final class BookFormViewModel {
    private let bridge: any BookRepositoryBridge
    private let onSaved: () async -> Void
    /// Non-nil means edit mode for this book id.
    let bookId: Int64?

    var title: String = "" {
        didSet {
            if titleError, !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                titleError = false
            }
        }
    }
    var author: String = ""
    var status: BookStatus = .wantToRead {
        didSet {
            if status != .finished {
                rating = nil
            }
            ratingError = false
        }
    }
    /// 1...5 or nil; always nil unless status is Finished.
    var rating: Int?
    var notes: String = ""
    var ratingError: Bool = false
    var titleError: Bool = false
    var saveError: Bool = false
    var isSaving: Bool = false
    /// Edit mode only: true while the book is being loaded for prefill.
    var isLoading: Bool = false
    /// Edit mode only: the book is gone (on load or while saving).
    var notFound: Bool = false
    /// Edit mode only: loading the book failed.
    var loadError: Bool = false
    /// Set to true after a successful save so the presenting view can dismiss the sheet.
    var didSave: Bool = false

    private var prefilled = false

    var isEditing: Bool { bookId != nil }

    init(bridge: any BookRepositoryBridge, bookId: Int64? = nil, onSaved: @escaping () async -> Void = {}) {
        self.bridge = bridge
        self.bookId = bookId
        self.onSaved = onSaved
        self.isLoading = bookId != nil
    }

    /// Edit mode: loads the book and prefills the fields once; later calls never overwrite user input.
    func load() async {
        guard let bookId, !prefilled else { return }
        isLoading = true
        loadError = false
        do {
            if let book = try await bridge.loadBook(id: bookId) {
                title = book.title
                author = book.author
                status = book.status
                rating = book.status == .finished ? book.rating : nil
                notes = book.notes
                prefilled = true
            } else {
                notFound = true
            }
        } catch {
            loadError = true
        }
        isLoading = false
    }

    /// Sets the rating from the selector; values outside 1...5 are ignored.
    func setRating(_ value: Int) {
        guard status == .finished, (1...5).contains(value) else { return }
        rating = value
        ratingError = false
    }

    func clearRating() {
        rating = nil
        ratingError = false
    }

    func incrementRating() {
        setRating((rating ?? 0) + 1)
    }

    /// Decrementing from 1 (or unset) is a no-op.
    func decrementRating() {
        guard let rating, rating > 1 else { return }
        setRating(rating - 1)
    }

    func save() async {
        guard !isSaving, !isLoading, !notFound else { return }
        saveError = false
        ratingError = false

        if title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            titleError = true
            return
        }

        isSaving = true
        do {
            if let bookId {
                _ = try await bridge.updateBook(id: bookId, title: title, author: author, status: status, rating: rating, notes: notes)
            } else {
                _ = try await bridge.addBook(title: title, author: author, status: status, rating: rating, notes: notes)
            }
            isSaving = false
            didSave = true
            await onSaved()
        } catch let failure as BookBridgeFailure where failure == .titleRequired {
            isSaving = false
            titleError = true
        } catch let failure as BookBridgeFailure where failure == .ratingOutOfRange {
            isSaving = false
            ratingError = true
        } catch let failure as BookBridgeFailure where failure == .notFound {
            isSaving = false
            notFound = true
        } catch {
            isSaving = false
            saveError = true
        }
    }
}
