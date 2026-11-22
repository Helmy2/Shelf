import Foundation

@Observable
final class BookDetailViewModel {
    enum ContentState {
        case loading
        case notFound
        case content(BookItemSwift)
        case error
    }

    private let bridge: any BookRepositoryBridge
    private let bookId: Int64
    private let onChanged: () async -> Void

    enum DeleteState: Equatable {
        case idle
        case confirming
        case deleting
        case failed
    }

    var contentState: ContentState = .loading
    private(set) var deleteState: DeleteState = .idle
    /// Set after a successful (or NotFound) delete; the view pops back to the list.
    private(set) var didDelete = false
    /// Title captured at confirm time, announced after the pop.
    private(set) var deletedTitle: String?
    var formViewModel: BookFormViewModel?

    var showForm: Bool {
        get { formViewModel != nil }
        set { if !newValue { formViewModel = nil } }
    }

    var canEdit: Bool {
        if case .content = contentState { return deleteState != .deleting }
        return false
    }

    var canDelete: Bool {
        if case .content = contentState { return true }
        return false
    }

    var isDeleting: Bool { deleteState == .deleting }
    var hasDeleteError: Bool { deleteState == .failed }

    var showDeleteConfirmation: Bool {
        get { deleteState == .confirming }
        set { if !newValue { cancelDelete() } }
    }

    var deleteSuccessAnnouncement: String? {
        deletedTitle.map { String(format: String(localized: "book_delete_success_a11y"), $0) }
    }

    init(bridge: any BookRepositoryBridge, bookId: Int64, onChanged: @escaping () async -> Void = {}) {
        self.bridge = bridge
        self.bookId = bookId
        self.onChanged = onChanged
    }

    func load() async {
        if case .content = contentState {
            // Keep current content visible while refreshing.
        } else {
            contentState = .loading
        }
        do {
            if let book = try await bridge.loadBook(id: bookId) {
                contentState = .content(book)
            } else {
                contentState = .notFound
            }
        } catch {
            contentState = .error
        }
    }

    func startEdit() {
        guard canEdit else { return }
        formViewModel = BookFormViewModel(bridge: bridge, bookId: bookId, onSaved: { [weak self] in
            await self?.load()
            await self?.onChanged()
        })
    }

    /// Called when the edit sheet closes; reloads so updated (or missing) values show.
    func dismissForm() async {
        formViewModel = nil
        await load()
        await onChanged()
    }

    // MARK: Delete

    /// Opens the confirmation; clears any earlier delete error.
    func requestDelete() {
        guard canDelete, deleteState != .deleting else { return }
        deleteState = .confirming
    }

    func cancelDelete() {
        guard deleteState == .confirming else { return }
        deleteState = .idle
    }

    /// Synchronously enters the deleting state (no double submit) and runs the delete.
    @discardableResult
    func confirmDelete() -> Task<Void, Never>? {
        guard deleteState == .confirming, case .content(let book) = contentState else { return nil }
        deleteState = .deleting
        let title = book.title
        return Task { [weak self] in await self?.performDelete(title: title) }
    }

    private func performDelete(title: String) async {
        do {
            try await bridge.deleteBook(id: bookId)
            await finishDelete(title: title)
        } catch BookBridgeFailure.notFound {
            await finishDelete(title: title)
        } catch {
            deleteState = .failed
        }
    }

    private func finishDelete(title: String) async {
        deletedTitle = title
        deleteState = .idle
        didDelete = true
        await onChanged()
    }
}
