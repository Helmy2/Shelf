import XCTest
@testable import app

final class BookEditFormViewModelTests: XCTestCase {
    private func makeBridge() -> MockBookRepositoryBridge {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 5, title: "Dune", author: "Frank", status: .reading)]
        return bridge
    }

    func testLoadPrefillsFields() async {
        let viewModel = BookFormViewModel(bridge: makeBridge(), bookId: 5)
        XCTAssertTrue(viewModel.isLoading)

        await viewModel.load()

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.title, "Dune")
        XCTAssertEqual(viewModel.author, "Frank")
        XCTAssertEqual(viewModel.status, .reading)
    }

    func testPrefillHappensOnlyOnce() async {
        let bridge = makeBridge()
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5)
        await viewModel.load()
        viewModel.title = "Typing"

        await viewModel.load()

        XCTAssertEqual(viewModel.title, "Typing")
        XCTAssertEqual(bridge.loadBookCallCount, 1)
    }

    func testMissingBookShowsNotFoundAndSaveDoesNothing() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5)

        await viewModel.load()
        await viewModel.save()

        XCTAssertTrue(viewModel.notFound)
        XCTAssertEqual(bridge.updateCallCount, 0)
        XCTAssertFalse(viewModel.didSave)
    }

    func testUpdateSuccessSendsIdAndFieldsAndNotifies() async {
        let bridge = makeBridge()
        var saved = 0
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5, onSaved: { saved += 1 })
        await viewModel.load()
        viewModel.title = "Dune Messiah"
        viewModel.status = .finished

        await viewModel.save()

        XCTAssertEqual(bridge.updateCallCount, 1)
        XCTAssertEqual(bridge.addCallCount, 0)
        XCTAssertEqual(bridge.lastUpdated?.id, 5)
        XCTAssertEqual(bridge.lastUpdated?.title, "Dune Messiah")
        XCTAssertEqual(bridge.lastUpdated?.status, .finished)
        XCTAssertTrue(viewModel.didSave)
        XCTAssertEqual(saved, 1)
    }

    func testBlankTitleSetsErrorAndDoesNotUpdate() async {
        let bridge = makeBridge()
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5)
        await viewModel.load()
        viewModel.title = "  "

        await viewModel.save()

        XCTAssertTrue(viewModel.titleError)
        XCTAssertEqual(bridge.updateCallCount, 0)
    }

    func testNotFoundWhileSavingShowsNotFound() async {
        let bridge = makeBridge()
        bridge.updateBookError = BookBridgeFailure.notFound
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5)
        await viewModel.load()

        await viewModel.save()

        XCTAssertTrue(viewModel.notFound)
        XCTAssertFalse(viewModel.didSave)
        XCTAssertFalse(viewModel.saveError)
    }

    func testGeneralFailureKeepsDataAndShowsSaveError() async {
        let bridge = makeBridge()
        bridge.updateBookError = MockBridgeError(errorDescription: "boom")
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5)
        await viewModel.load()
        viewModel.title = "Edited"

        await viewModel.save()

        XCTAssertTrue(viewModel.saveError)
        XCTAssertFalse(viewModel.didSave)
        XCTAssertEqual(viewModel.title, "Edited")
    }

    func testLoadFailureSetsLoadError() async {
        let bridge = makeBridge()
        bridge.loadBookError = MockBridgeError(errorDescription: "boom")
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5)

        await viewModel.load()

        XCTAssertTrue(viewModel.loadError)
        XCTAssertFalse(viewModel.isLoading)
    }
}
