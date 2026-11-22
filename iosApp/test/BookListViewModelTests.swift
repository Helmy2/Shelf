import XCTest
@testable import app

final class BookListViewModelTests: XCTestCase {
    func testLoadSetsContentWhenBridgeReturnsBooks() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook()]
        let viewModel = BookListViewModel(bridge: bridge)

        await viewModel.load(force: true)

        guard case .content(let books) = viewModel.contentState else {
            return XCTFail("Expected content state")
        }
        XCTAssertEqual(books.count, 1)
        XCTAssertEqual(books.first?.title, "Dune")
    }

    func testLoadSetsEmptyWhenNoBooks() async {
        let viewModel = BookListViewModel(bridge: MockBookRepositoryBridge())

        await viewModel.load(force: true)

        guard case .empty = viewModel.contentState else {
            return XCTFail("Expected empty state")
        }
    }

    func testLoadSetsErrorWhenBridgeThrows() async {
        let bridge = MockBookRepositoryBridge()
        bridge.loadBooksError = MockBridgeError(errorDescription: "boom")
        let viewModel = BookListViewModel(bridge: bridge)

        await viewModel.load(force: true)

        guard case .error = viewModel.contentState else {
            return XCTFail("Expected error state")
        }
    }

    func testRetryRecoversFromError() async {
        let bridge = MockBookRepositoryBridge()
        bridge.loadBooksError = MockBridgeError(errorDescription: "boom")
        let viewModel = BookListViewModel(bridge: bridge)
        await viewModel.load(force: true)

        bridge.loadBooksError = nil
        bridge.booksToReturn = [makeBook()]
        await viewModel.load(force: true)

        guard case .content = viewModel.contentState else {
            return XCTFail("Expected content after retry")
        }
    }

    func testStartAddShowsFormAndDismissHidesIt() {
        let viewModel = BookListViewModel(bridge: MockBookRepositoryBridge())

        viewModel.startAdd()
        XCTAssertTrue(viewModel.showForm)

        viewModel.dismissForm()
        XCTAssertFalse(viewModel.showForm)
    }

    func testSavingFromFormReloadsListWithNewBookOnTop() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 10, title: "Old")]
        let viewModel = BookListViewModel(bridge: bridge)
        await viewModel.load(force: true)

        viewModel.startAdd()
        let form = viewModel.formViewModel
        form?.title = "New"
        await form?.save()

        XCTAssertEqual(form?.didSave, true)
        guard case .content(let books) = viewModel.contentState else {
            return XCTFail("Expected content state")
        }
        XCTAssertEqual(books.first?.title, "New")
        XCTAssertEqual(books.count, 2)
    }
}

final class BookListFilterTests: XCTestCase {
    private func loaded(_ books: [BookItemSwift], bridge: MockBookRepositoryBridge = MockBookRepositoryBridge()) async -> (BookListViewModel, MockBookRepositoryBridge) {
        bridge.booksToReturn = books
        let viewModel = BookListViewModel(bridge: bridge)
        await viewModel.load(force: true)
        return (viewModel, bridge)
    }

    private var sample: [BookItemSwift] {
        [
            makeBook(id: 4, title: "D", status: .reading),
            makeBook(id: 3, title: "C", status: .finished),
            makeBook(id: 2, title: "B", status: .wantToRead),
            makeBook(id: 1, title: "A", status: .reading)
        ]
    }

    func testDefaultIsAllWithEveryBook() async {
        let (vm, _) = await loaded(sample)
        XCTAssertEqual(vm.selectedFilter, .all)
        XCTAssertEqual(vm.visibleBooks.count, 4)
        XCTAssertTrue(vm.showsFilterChips)
        XCTAssertFalse(vm.showsFilteredEmptyState)
    }

    func testSelectingEachStatusFilters() async {
        let (vm, _) = await loaded(sample)
        vm.selectFilter(.wantToRead)
        XCTAssertEqual(vm.visibleBooks.map(\.id), [2])
        vm.selectFilter(.reading)
        XCTAssertEqual(vm.visibleBooks.map(\.id), [4, 1])
        vm.selectFilter(.finished)
        XCTAssertEqual(vm.visibleBooks.map(\.id), [3])
        vm.selectFilter(.all)
        XCTAssertEqual(vm.visibleBooks.map(\.id), [4, 3, 2, 1])
    }

    func testNoMatchShowsFilteredEmptyStateAndKeepsChips() async {
        let (vm, _) = await loaded([makeBook(id: 1, status: .reading)])
        vm.selectFilter(.finished)
        XCTAssertTrue(vm.visibleBooks.isEmpty)
        XCTAssertTrue(vm.showsFilteredEmptyState)
        XCTAssertTrue(vm.showsFilterChips)
        XCTAssertEqual(vm.selectedFilter, .finished)
    }

    func testNoBooksHidesChipsAndFilteredEmptyState() async {
        let (vm, _) = await loaded([])
        XCTAssertFalse(vm.showsFilterChips)
        XCTAssertFalse(vm.showsFilteredEmptyState)
    }

    func testChipsHiddenInLoadingAndError() async {
        let bridge = MockBookRepositoryBridge()
        bridge.loadBooksError = MockBridgeError(errorDescription: "boom")
        let vm = BookListViewModel(bridge: bridge)
        XCTAssertFalse(vm.showsFilterChips)
        await vm.load(force: true)
        XCTAssertFalse(vm.showsFilterChips)
    }

    func testSelectingCurrentValueIsNoOp() async {
        let (vm, _) = await loaded(sample)
        let token = vm.filterChangeToken
        vm.selectFilter(.all)
        XCTAssertEqual(vm.filterChangeToken, token)
        vm.selectFilter(.reading)
        XCTAssertEqual(vm.filterChangeToken, token + 1)
        vm.selectFilter(.reading)
        XCTAssertEqual(vm.filterChangeToken, token + 1)
    }

    func testResetsToAllWhenListBecomesEmpty() async {
        let (vm, bridge) = await loaded(sample)
        vm.selectFilter(.reading)
        bridge.booksToReturn = []
        await vm.load(force: true)
        XCTAssertEqual(vm.selectedFilter, .all)
        bridge.booksToReturn = [makeBook(id: 9, status: .finished)]
        await vm.load(force: true)
        XCTAssertEqual(vm.visibleBooks.map(\.id), [9])
    }

    func testSelectionKeptAfterAddReload() async {
        let (vm, _) = await loaded(sample)
        vm.selectFilter(.reading)
        vm.startAdd()
        vm.formViewModel?.title = "New"
        vm.formViewModel?.status = .wantToRead
        await vm.formViewModel?.save()
        XCTAssertEqual(vm.selectedFilter, .reading)
        XCTAssertEqual(vm.visibleBooks.map(\.id), [4, 1])
    }

    func testSelectionKeptAfterEditReloadAndStatusChangeMovesBook() async {
        let (vm, bridge) = await loaded(sample)
        vm.selectFilter(.reading)
        let detail = vm.makeDetailViewModel(id: 4)
        _ = detail
        bridge.booksToReturn[0] = makeBook(id: 4, title: "D", status: .finished)
        await vm.load(force: true)
        XCTAssertEqual(vm.selectedFilter, .reading)
        XCTAssertEqual(vm.visibleBooks.map(\.id), [1])
    }

    func testFilterAnnouncementUsesLabelAndCounts() async {
        let (vm, _) = await loaded(sample)
        vm.selectFilter(.reading)
        XCTAssertEqual(vm.filterResultAnnouncement, "Reading, 2 of 4 books shown")
    }

    func testDeletingLastBookEmptiesListAndResetsFilter() async {
        let bridge = MockBookRepositoryBridge()
        bridge.booksToReturn = [makeBook(id: 1, status: .reading)]
        let viewModel = BookListViewModel(bridge: bridge)
        await viewModel.load()
        viewModel.selectFilter(.reading)
        let detail = viewModel.makeDetailViewModel(id: 1)
        await detail.load()

        detail.requestDelete()
        await detail.confirmDelete()?.value

        guard case .empty = viewModel.contentState else { return XCTFail("expected empty") }
        XCTAssertEqual(viewModel.selectedFilter, .all)
    }
}
