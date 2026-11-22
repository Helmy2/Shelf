import Foundation

enum BookListFilter: CaseIterable, Identifiable {
    case all
    case wantToRead
    case reading
    case finished

    var id: Self { self }

    var status: BookStatus? {
        switch self {
        case .all: nil
        case .wantToRead: .wantToRead
        case .reading: .reading
        case .finished: .finished
        }
    }

    var displayName: String {
        status?.displayName ?? String(localized: "book_filter_all")
    }
}

@Observable
final class BookListViewModel {
    enum ContentState {
        case loading
        case empty
        case content([BookItemSwift])
        case error
    }

    private let bridge: any BookRepositoryBridge

    var contentState: ContentState = .loading
    var formViewModel: BookFormViewModel?
    private(set) var selectedFilter: BookListFilter = .all

    /// Bumped on every real selection change; the view scrolls to the top when it changes.
    private(set) var filterChangeToken = 0

    private var allBooks: [BookItemSwift] {
        if case .content(let books) = contentState { return books }
        return []
    }

    /// Loaded books filtered by status; repository order (newest first) preserved.
    var visibleBooks: [BookItemSwift] {
        guard let status = selectedFilter.status else { return allBooks }
        return allBooks.filter { $0.status == status }
    }

    /// Chips show whenever at least one book exists.
    var showsFilterChips: Bool { !allBooks.isEmpty }

    /// Books exist but none match the selected filter.
    var showsFilteredEmptyState: Bool { !allBooks.isEmpty && visibleBooks.isEmpty }

    var filterResultAnnouncement: String {
        String(
            format: String(localized: "book_filter_result_a11y"),
            selectedFilter.displayName,
            String(visibleBooks.count),
            String(allBooks.count)
        )
    }

    func selectFilter(_ filter: BookListFilter) {
        guard filter != selectedFilter else { return }
        selectedFilter = filter
        filterChangeToken += 1
    }

    var showForm: Bool {
        get { formViewModel != nil }
        set { if !newValue { formViewModel = nil } }
    }

    init(bridge: any BookRepositoryBridge) {
        self.bridge = bridge
    }

    func load(force: Bool = false) async {
        if case .content = contentState, !force { return }
        if force, case .content = contentState {
            // Keep rows visible during pull-to-refresh.
        } else {
            contentState = .loading
        }

        do {
            let books = try await bridge.loadBooks()
            contentState = books.isEmpty ? .empty : .content(books)
            if books.isEmpty { selectedFilter = .all }
        } catch {
            contentState = .error
        }
    }

    func startAdd() {
        formViewModel = BookFormViewModel(bridge: bridge, onSaved: { [weak self] in
            await self?.load(force: true)
        })
    }

    func makeDetailViewModel(id: Int64) -> BookDetailViewModel {
        BookDetailViewModel(bridge: bridge, bookId: id, onChanged: { [weak self] in
            await self?.load(force: true)
        })
    }

    func dismissForm() {
        formViewModel = nil
    }
}
