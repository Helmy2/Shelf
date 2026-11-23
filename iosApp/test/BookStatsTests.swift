import XCTest
@testable import app

final class BookStatsTests: XCTestCase {
    func testEmptyListHasZeroCountsAndNilAverage() {
        let stats = BookStats.compute(from: [])
        XCTAssertEqual(stats.wantToReadCount, 0)
        XCTAssertEqual(stats.readingCount, 0)
        XCTAssertEqual(stats.finishedCount, 0)
        XCTAssertEqual(stats.ratedCount, 0)
        XCTAssertNil(stats.averageRating)
        XCTAssertNil(stats.averageText)
    }

    func testMixedCountsPerStatus() {
        let stats = BookStats.compute(from: [
            makeBook(id: 1, status: .wantToRead),
            makeBook(id: 2, status: .reading),
            makeBook(id: 3, status: .reading),
            makeBook(id: 4, status: .finished, rating: 3)
        ])
        XCTAssertEqual(stats.wantToReadCount, 1)
        XCTAssertEqual(stats.readingCount, 2)
        XCTAssertEqual(stats.finishedCount, 1)
        XCTAssertEqual(stats.ratedCount, 1)
        XCTAssertEqual(stats.averageRating, 3.0)
    }

    func testUnratedFinishedBooksExcludedFromAverage() {
        let stats = BookStats.compute(from: [
            makeBook(id: 1, status: .finished, rating: 4),
            makeBook(id: 2, status: .finished, rating: nil)
        ])
        XCTAssertEqual(stats.finishedCount, 2)
        XCTAssertEqual(stats.ratedCount, 1)
        XCTAssertEqual(stats.averageRating, 4.0)
    }

    func testFinishedWithNoRatingsHasNilAverage() {
        let stats = BookStats.compute(from: [makeBook(status: .finished, rating: nil)])
        XCTAssertEqual(stats.ratedCount, 0)
        XCTAssertNil(stats.averageRating)
    }

    func testRatingOnNonFinishedIsIgnored() {
        let stats = BookStats.compute(from: [
            makeBook(id: 1, status: .reading, rating: 5),
            makeBook(id: 2, status: .wantToRead, rating: 1)
        ])
        XCTAssertEqual(stats.ratedCount, 0)
        XCTAssertNil(stats.averageRating)
    }

    func testFourAndFiveAverageFourPointFive() {
        let stats = BookStats.compute(from: [
            makeBook(id: 1, status: .finished, rating: 4),
            makeBook(id: 2, status: .finished, rating: 5)
        ])
        XCTAssertEqual(stats.averageRating, 4.5)
        XCTAssertEqual(stats.ratedCount, 2)
        XCTAssertEqual(stats.averageText, "4.5")
    }

    func testAverageFormattedWithOneDecimal() {
        let stats = BookStats.compute(from: [
            makeBook(id: 1, status: .finished, rating: 4),
            makeBook(id: 2, status: .finished, rating: 4),
            makeBook(id: 3, status: .finished, rating: 5),
            makeBook(id: 4, status: .finished, rating: 4),
            makeBook(id: 5, status: .finished, rating: 4)
        ])
        XCTAssertEqual(stats.averageText, "4.2")
    }

    func testAccessibilitySummary() {
        let stats = BookStats.compute(from: [
            makeBook(id: 1, status: .wantToRead),
            makeBook(id: 2, status: .finished, rating: 4)
        ])
        XCTAssertEqual(
            stats.accessibilitySummary,
            "1 want to read, 0 reading, 1 finished. 4.0 out of 5"
        )
        let unrated = BookStats.compute(from: [makeBook(status: .reading)])
        XCTAssertEqual(
            unrated.accessibilitySummary,
            "0 want to read, 1 reading, 0 finished. No ratings yet"
        )
    }
}

final class BookStatsViewModelTests: XCTestCase {
    func testInitialStateIsLoading() {
        let viewModel = BookStatsViewModel(bridge: MockBookRepositoryBridge())
        XCTAssertEqual(viewModel.contentState, .loading)
    }

    func testEmptyWhenNoBooks() async {
        let viewModel = BookStatsViewModel(bridge: MockBookRepositoryBridge())
        await viewModel.load(force: true)
        XCTAssertEqual(viewModel.contentState, .empty)
    }

    func testContentWithComputedStats() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [
            makeBook(id: 1, status: .finished, rating: 4),
            makeBook(id: 2, status: .finished, rating: 5),
            makeBook(id: 3, status: .reading)
        ]
        let viewModel = BookStatsViewModel(bridge: bridge)
        await viewModel.load(force: true)
        guard case .content(let stats) = viewModel.contentState else {
            return XCTFail("Expected content state")
        }
        XCTAssertEqual(stats.averageRating, 4.5)
        XCTAssertEqual(stats.readingCount, 1)
        XCTAssertEqual(stats.finishedCount, 2)
    }

    func testErrorThenRetryRecovers() async {
        let bridge = MockBookRepositoryBridge()
        bridge.loadBooksError = MockBridgeError(errorDescription: "boom")
        let viewModel = BookStatsViewModel(bridge: bridge)
        await viewModel.load(force: true)
        XCTAssertEqual(viewModel.contentState, .error)

        bridge.loadBooksError = nil
        bridge.booksToReturn = [makeBook(status: .wantToRead)]
        await viewModel.load(force: true)
        guard case .content(let stats) = viewModel.contentState else {
            return XCTFail("Expected content after retry")
        }
        XCTAssertEqual(stats.wantToReadCount, 1)
    }

    func testListViewModelShowsStatsActionOnlyWithBooks() async {
        let bridge = MockBookRepositoryBridge()
        let list = BookListViewModel(bridge: bridge)
        XCTAssertFalse(list.showsStatsAction)
        await list.load(force: true)
        XCTAssertFalse(list.showsStatsAction)
        bridge.booksToReturn = [makeBook()]
        await list.load(force: true)
        XCTAssertTrue(list.showsStatsAction)
    }
}
