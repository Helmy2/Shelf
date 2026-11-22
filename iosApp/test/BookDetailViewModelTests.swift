import XCTest
@testable import app

final class BookDetailViewModelTests: XCTestCase {
    func testLoadShowsContent() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 7, title: "Dune")]
        let viewModel = BookDetailViewModel(bridge: bridge, bookId: 7)

        await viewModel.load()

        guard case .content(let book) = viewModel.contentState else { return XCTFail("expected content") }
        XCTAssertEqual(book.title, "Dune")
        XCTAssertTrue(viewModel.canEdit)
    }

    func testMissingBookShowsNotFoundAndHidesEdit() async {
        let viewModel = BookDetailViewModel(bridge: MockBookRepositoryBridge(), bookId: 9)

        await viewModel.load()

        guard case .notFound = viewModel.contentState else { return XCTFail("expected notFound") }
        XCTAssertFalse(viewModel.canEdit)
    }

    func testFailureShowsError() async {
        let bridge = MockBookRepositoryBridge()
        bridge.loadBookError = MockBridgeError(errorDescription: "boom")
        let viewModel = BookDetailViewModel(bridge: bridge, bookId: 1)

        await viewModel.load()

        guard case .error = viewModel.contentState else { return XCTFail("expected error") }
        XCTAssertFalse(viewModel.canEdit)
    }

    func testStartEditCreatesEditFormOnlyWithContent() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 1)]
        let viewModel = BookDetailViewModel(bridge: bridge, bookId: 1)
        viewModel.startEdit()
        XCTAssertNil(viewModel.formViewModel)

        await viewModel.load()
        viewModel.startEdit()

        XCTAssertEqual(viewModel.formViewModel?.bookId, 1)
        XCTAssertTrue(viewModel.showForm)
    }

    func testDismissFormReloadsUpdatedValuesAndNotifies() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 1, title: "Dune")]
        var changed = 0
        let viewModel = BookDetailViewModel(bridge: bridge, bookId: 1, onChanged: { changed += 1 })
        await viewModel.load()
        viewModel.startEdit()
        _ = try? await bridge.updateBook(id: 1, title: "Dune Messiah", author: "FH", status: .reading, rating: nil, notes: "")

        await viewModel.dismissForm()

        guard case .content(let book) = viewModel.contentState else { return XCTFail("expected content") }
        XCTAssertEqual(book.title, "Dune Messiah")
        XCTAssertNil(viewModel.formViewModel)
        XCTAssertEqual(changed, 1)
    }

    func testBookRemovedWhileEditingMovesToNotFound() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 1)]
        let viewModel = BookDetailViewModel(bridge: bridge, bookId: 1)
        await viewModel.load()
        bridge.booksToReturn = []

        await viewModel.dismissForm()

        guard case .notFound = viewModel.contentState else { return XCTFail("expected notFound") }
    }

    // MARK: Delete

    private func loadedViewModel(
        _ bridge: MockBookRepositoryBridge,
        onChanged: @escaping () async -> Void = {}
    ) async -> BookDetailViewModel {
        bridge.booksToReturn = [makeBook(id: 1, title: "Dune")]
        let viewModel = BookDetailViewModel(bridge: bridge, bookId: 1, onChanged: onChanged)
        await viewModel.load()
        return viewModel
    }

    func testRequestAndCancelDelete() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = await loadedViewModel(bridge)

        viewModel.requestDelete()
        XCTAssertTrue(viewModel.showDeleteConfirmation)
        viewModel.cancelDelete()

        XCTAssertEqual(viewModel.deleteState, .idle)
        XCTAssertEqual(bridge.deleteCallCount, 0)
        XCTAssertFalse(viewModel.didDelete)
    }

    func testRequestDeleteIgnoredWithoutContent() async {
        let viewModel = BookDetailViewModel(bridge: MockBookRepositoryBridge(), bookId: 5)
        await viewModel.load()
        viewModel.requestDelete()
        XCTAssertEqual(viewModel.deleteState, .idle)
    }

    func testConfirmDeleteSucceedsAndNotifies() async {
        let bridge = MockBookRepositoryBridge()
        var changed = 0
        let viewModel = await loadedViewModel(bridge, onChanged: { changed += 1 })

        viewModel.requestDelete()
        await viewModel.confirmDelete()?.value

        XCTAssertTrue(viewModel.didDelete)
        XCTAssertEqual(viewModel.deletedTitle, "Dune")
        XCTAssertEqual(changed, 1)
        XCTAssertTrue(bridge.booksToReturn.isEmpty)
    }

    func testNotFoundIsTreatedAsSuccess() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = await loadedViewModel(bridge)
        bridge.deleteBookError = BookBridgeFailure.notFound

        viewModel.requestDelete()
        await viewModel.confirmDelete()?.value

        XCTAssertTrue(viewModel.didDelete)
        XCTAssertFalse(viewModel.hasDeleteError)
    }

    func testFailureKeepsDetailWithError() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = await loadedViewModel(bridge)
        bridge.deleteBookError = BookBridgeFailure.other

        viewModel.requestDelete()
        await viewModel.confirmDelete()?.value

        XCTAssertFalse(viewModel.didDelete)
        XCTAssertTrue(viewModel.hasDeleteError)
        XCTAssertTrue(viewModel.canEdit)
        guard case .content(let book) = viewModel.contentState else { return XCTFail("expected content") }
        XCTAssertEqual(book.title, "Dune")
        XCTAssertEqual(bridge.booksToReturn.count, 1)
    }

    func testErrorClearsWhenConfirmationOpensAndRetrySucceeds() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = await loadedViewModel(bridge)
        bridge.deleteBookError = BookBridgeFailure.other
        viewModel.requestDelete()
        await viewModel.confirmDelete()?.value
        XCTAssertTrue(viewModel.hasDeleteError)

        viewModel.requestDelete()
        XCTAssertFalse(viewModel.hasDeleteError)

        bridge.deleteBookError = nil
        await viewModel.confirmDelete()?.value
        XCTAssertTrue(viewModel.didDelete)
    }

    func testNoDoubleSubmitAndEditDisabledWhileDeleting() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = await loadedViewModel(bridge)
        var release: CheckedContinuation<Void, Never>?
        bridge.deleteGate = { await withCheckedContinuation { release = $0 } }

        viewModel.requestDelete()
        let task = viewModel.confirmDelete()
        XCTAssertTrue(viewModel.isDeleting)
        XCTAssertFalse(viewModel.canEdit)
        XCTAssertNil(viewModel.confirmDelete())
        viewModel.requestDelete()
        XCTAssertTrue(viewModel.isDeleting)

        while release == nil { await Task.yield() }
        release?.resume()
        await task?.value

        XCTAssertEqual(bridge.deleteCallCount, 1)
        XCTAssertTrue(viewModel.didDelete)
    }
}
