import Foundation

@Observable
final class BookStatsViewModel {
    enum ContentState: Equatable {
        case loading
        case empty
        case content(BookStats)
        case error
    }

    private let bridge: any BookRepositoryBridge

    var contentState: ContentState = .loading

    init(bridge: any BookRepositoryBridge) {
        self.bridge = bridge
    }

    func load(force: Bool = false) async {
        if case .content = contentState, !force { return }
        if force, case .content = contentState {
            // Keep stats visible while refreshing.
        } else {
            contentState = .loading
        }

        do {
            let books = try await bridge.loadBooks()
            contentState = books.isEmpty ? .empty : .content(BookStats.compute(from: books))
        } catch {
            contentState = .error
        }
    }
}
