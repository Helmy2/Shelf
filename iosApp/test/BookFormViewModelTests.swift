import XCTest
@testable import app

final class BookFormViewModelTests: XCTestCase {
    func testBlankTitleSetsErrorAndDoesNotSave() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = BookFormViewModel(bridge: bridge)
        viewModel.title = "   "

        await viewModel.save()

        XCTAssertTrue(viewModel.titleError)
        XCTAssertFalse(viewModel.didSave)
        XCTAssertEqual(bridge.addCallCount, 0)
    }

    func testTitleErrorClearsWhenTypingNonBlank() async {
        let viewModel = BookFormViewModel(bridge: MockBookRepositoryBridge())
        await viewModel.save()
        XCTAssertTrue(viewModel.titleError)

        viewModel.title = "D"

        XCTAssertFalse(viewModel.titleError)
    }

    func testSaveSendsFieldsAndMarksSaved() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = BookFormViewModel(bridge: bridge)
        viewModel.title = "Dune"
        viewModel.author = "Frank Herbert"
        viewModel.status = .reading

        await viewModel.save()

        XCTAssertEqual(bridge.addCallCount, 1)
        XCTAssertEqual(bridge.lastAdded?.title, "Dune")
        XCTAssertEqual(bridge.lastAdded?.author, "Frank Herbert")
        XCTAssertEqual(bridge.lastAdded?.status, .reading)
        XCTAssertTrue(viewModel.didSave)
        XCTAssertFalse(viewModel.isSaving)
    }

    func testBridgeTitleRequiredFailureShowsTitleError() async {
        let bridge = MockBookRepositoryBridge()
        bridge.addBookError = BookBridgeFailure.titleRequired
        let viewModel = BookFormViewModel(bridge: bridge)
        viewModel.title = "Dune"

        await viewModel.save()

        XCTAssertTrue(viewModel.titleError)
        XCTAssertFalse(viewModel.saveError)
    }

    func testGeneralFailureKeepsDataAndShowsSaveError() async {
        let bridge = MockBookRepositoryBridge()
        bridge.addBookError = MockBridgeError(errorDescription: "boom")
        let viewModel = BookFormViewModel(bridge: bridge)
        viewModel.title = "Dune"
        viewModel.author = "Frank"

        await viewModel.save()

        XCTAssertTrue(viewModel.saveError)
        XCTAssertFalse(viewModel.didSave)
        XCTAssertFalse(viewModel.isSaving)
        XCTAssertEqual(viewModel.title, "Dune")
        XCTAssertEqual(viewModel.author, "Frank")
    }
}
