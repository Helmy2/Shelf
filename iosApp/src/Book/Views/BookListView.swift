import SwiftUI

struct BookListView: View {
    @State private var viewModel: BookListViewModel
    @Environment(\.dreamColors) private var colors

    init(viewModel: BookListViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    var body: some View {
        NavigationStack {
            Group {
                switch viewModel.contentState {
                case .loading:
                    VStack(spacing: DreamSpacing.md) {
                        DreamCircularProgressIndicator(size: .large)
                        Text("book_loading_title")
                            .font(.dreamBodyMedium)
                            .foregroundStyle(colors.onSurfaceVariant)
                    }

                case .empty:
                    VStack(spacing: DreamSpacing.md) {
                        DreamEmptyStateView(
                            title: "book_empty_title",
                            message: "book_empty_body"
                        )
                        DreamButton("book_add", variant: .outlined) {
                            viewModel.startAdd()
                        }
                        .fixedSize()
                    }

                case .content(let books):
                    listContent(books)

                case .error:
                    DreamErrorStateView(
                        title: "book_error_title",
                        message: String(localized: "book_error_body"),
                        retryLabel: "book_retry",
                        onRetry: { Task { await viewModel.load(force: true) } }
                    )
                }
            }
            .navigationTitle("book_list_title")
            .toolbar {
                if viewModel.showsStatsAction {
                    ToolbarItem(placement: .primaryAction) {
                        NavigationLink(value: BookStatsDestination()) {
                            Label("book_stats_action", systemImage: "chart.bar")
                        }
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button("book_add", systemImage: "plus") {
                        viewModel.startAdd()
                    }
                }
            }
            .navigationDestination(for: Int64.self) { id in
                BookDetailView(viewModel: viewModel.makeDetailViewModel(id: id))
            }
            .navigationDestination(for: BookStatsDestination.self) { _ in
                BookStatsView(viewModel: viewModel.makeStatsViewModel())
            }
            .sheet(isPresented: $viewModel.showForm) {
                if let formViewModel = viewModel.formViewModel {
                    BookFormView(viewModel: formViewModel, onClose: { viewModel.dismissForm() })
                }
            }
            .task { await viewModel.load() }
            .refreshable { await viewModel.load(force: true) }
        }
        .dreamTheme()
    }

    private func listContent(_ books: [BookItemSwift]) -> some View {
        ScrollViewReader { proxy in
        List {
            filterChips
                .id(Self.topId)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets())
            if viewModel.showsFilteredEmptyState {
                VStack(spacing: DreamSpacing.md) {
                    DreamEmptyStateView(
                        title: "book_empty_filtered_title",
                        message: LocalizedStringKey(String(
                            format: String(localized: "book_empty_filtered_body"),
                            viewModel.selectedFilter.displayName
                        ))
                    )
                    DreamButton("book_filter_show_all", variant: .outlined) {
                        viewModel.selectFilter(.all)
                    }
                    .fixedSize()
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            }
            ForEach(viewModel.visibleBooks) { book in
            NavigationLink(value: book.id) {
            HStack(alignment: .center, spacing: DreamSpacing.sm) {
            VStack(alignment: .leading, spacing: DreamSpacing.xs) {
                Text(book.title)
                    .font(.dreamTitleMedium)
                    .foregroundStyle(colors.onSurface)
                    .lineLimit(2)
                if !book.author.isEmpty {
                    Text(book.author)
                        .font(.dreamBodyMedium)
                        .foregroundStyle(colors.onSurfaceVariant)
                }
                Text(book.status.displayName)
                    .font(.dreamBodyMedium)
                    .foregroundStyle(colors.primary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let rating = book.shownRating {
                Text("★ \(rating)")
                    .font(.dreamBodyMedium)
                    .foregroundStyle(colors.primary)
            }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(book.accessibilityLabel)
            }
            .accessibilityAddTraits(.isButton)
            .accessibilityHint(Text("book_row_open_hint"))
            }
        }
        .onChange(of: viewModel.filterChangeToken) {
            withAnimation { proxy.scrollTo(Self.topId, anchor: .top) }
            UIAccessibility.post(notification: .announcement, argument: viewModel.filterResultAnnouncement)
        }
        }
    }

    private static let topId = "book_filter_top"

    private var filterChips: some View {
        ScrollViewReader { chipProxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DreamSpacing.sm) {
                    ForEach(BookListFilter.allCases) { filter in
                        filterChip(filter).id(filter)
                    }
                }
                .padding(.horizontal, DreamSpacing.lg)
                .padding(.vertical, DreamSpacing.sm)
            }
            .onChange(of: viewModel.selectedFilter) {
                withAnimation { chipProxy.scrollTo(viewModel.selectedFilter, anchor: .center) }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text("book_filter_label"))
    }

    private func filterChip(_ filter: BookListFilter) -> some View {
        let selected = viewModel.selectedFilter == filter
        return Button {
            viewModel.selectFilter(filter)
        } label: {
            HStack(spacing: DreamSpacing.xs) {
                if selected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(colors.primary)
                        .accessibilityHidden(true)
                }
                Text(filter.displayName)
                    .fixedSize(horizontal: true, vertical: false)
            }
            .font(.subheadline)
            .fontWeight(selected ? .semibold : .regular)
            .foregroundStyle(colors.onSurface)
            .padding(.horizontal, DreamSpacing.md)
            .frame(minHeight: 44)
            .background(Capsule().fill(selected ? colors.primary.opacity(0.16) : Color.clear))
            .overlay(Capsule().strokeBorder(selected ? Color.clear : colors.outline, lineWidth: 1))
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }
}
