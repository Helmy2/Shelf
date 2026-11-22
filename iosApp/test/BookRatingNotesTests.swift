import XCTest
@testable import app

final class BookRatingNotesTests: XCTestCase {
    func testSettingRatingRequiresFinished() {
        let viewModel = BookFormViewModel(bridge: MockBookRepositoryBridge())
        viewModel.setRating(3)
        XCTAssertNil(viewModel.rating)

        viewModel.status = .finished
        viewModel.setRating(3)
        XCTAssertEqual(viewModel.rating, 3)
    }

    func testStatusChangeClearsRatingAndReturnStartsUnset() {
        let viewModel = BookFormViewModel(bridge: MockBookRepositoryBridge())
        viewModel.status = .finished
        viewModel.setRating(4)

        viewModel.status = .reading
        XCTAssertNil(viewModel.rating)

        viewModel.status = .finished
        XCTAssertNil(viewModel.rating)
    }

    func testAdjustableIncrementDecrementBounds() {
        let viewModel = BookFormViewModel(bridge: MockBookRepositoryBridge())
        viewModel.status = .finished

        viewModel.decrementRating()
        XCTAssertNil(viewModel.rating)
        viewModel.incrementRating()
        XCTAssertEqual(viewModel.rating, 1)
        viewModel.decrementRating()
        XCTAssertEqual(viewModel.rating, 1)
        for _ in 0..<10 { viewModel.incrementRating() }
        XCTAssertEqual(viewModel.rating, 5)
        viewModel.clearRating()
        XCTAssertNil(viewModel.rating)
    }

    func testAddPassesRatingAndNotes() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = BookFormViewModel(bridge: bridge)
        viewModel.title = "Dune"
        viewModel.status = .finished
        viewModel.setRating(5)
        viewModel.notes = "Line one\nLine two"

        await viewModel.save()

        XCTAssertTrue(viewModel.didSave)
        XCTAssertEqual(bridge.lastAdded?.rating, 5)
        XCTAssertEqual(bridge.lastAdded?.notes, "Line one\nLine two")
    }

    func testAddNonFinishedSendsNilRating() async {
        let bridge = MockBookRepositoryBridge()
        let viewModel = BookFormViewModel(bridge: bridge)
        viewModel.title = "Dune"
        viewModel.status = .finished
        viewModel.setRating(2)
        viewModel.status = .reading

        await viewModel.save()

        XCTAssertNil(bridge.lastAdded?.rating)
    }

    func testEditPrefillsAndUpdateWritesFormValues() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 5, status: .finished, rating: 3, notes: "Great")]
        let viewModel = BookFormViewModel(bridge: bridge, bookId: 5)

        await viewModel.load()
        XCTAssertEqual(viewModel.rating, 3)
        XCTAssertEqual(viewModel.notes, "Great")

        viewModel.setRating(4)
        viewModel.notes = "Better"
        await viewModel.save()

        XCTAssertEqual(bridge.lastUpdated?.rating, 4)
        XCTAssertEqual(bridge.lastUpdated?.notes, "Better")
    }

    func testRatingOutOfRangeShowsInlineErrorAndKeepsData() async {
        let bridge = MockBookRepositoryBridge()
        bridge.addBookError = BookBridgeFailure.ratingOutOfRange
        let viewModel = BookFormViewModel(bridge: bridge)
        viewModel.title = "Dune"
        viewModel.notes = "keep me"

        await viewModel.save()

        XCTAssertTrue(viewModel.ratingError)
        XCTAssertFalse(viewModel.saveError)
        XCTAssertFalse(viewModel.didSave)
        XCTAssertEqual(viewModel.notes, "keep me")

        viewModel.status = .finished
        XCTAssertFalse(viewModel.ratingError)
    }

    func testDetailRatingVisibleOnlyWhenFinishedAndRated() {
        XCTAssertEqual(makeBook(status: .finished, rating: 4).shownRating, 4)
        XCTAssertNil(makeBook(status: .finished, rating: nil).shownRating)
        XCTAssertNil(makeBook(status: .reading, rating: 4).shownRating)
    }

    func testDetailNotesHiddenWhenBlank() {
        XCTAssertFalse(makeBook(notes: "").hasNotes)
        XCTAssertFalse(makeBook(notes: " \n ").hasNotes)
        XCTAssertTrue(makeBook(notes: "hello").hasNotes)
    }

    func testRowAccessibilityLabelIncludesRating() {
        let rated = makeBook(title: "Dune", author: "Frank", status: .finished, rating: 4)
        XCTAssertTrue(rated.accessibilityLabel.contains("rated 4 of 5"))
        let noAuthor = makeBook(title: "Dune", author: "", status: .finished, rating: 4)
        XCTAssertTrue(noAuthor.accessibilityLabel.contains("rated 4 of 5"))
        let unrated = makeBook(title: "Dune", author: "Frank", status: .finished, rating: nil)
        XCTAssertFalse(unrated.accessibilityLabel.contains("rated"))
    }
}
