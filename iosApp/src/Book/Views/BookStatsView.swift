import SwiftUI

struct BookStatsView: View {
    @State private var viewModel: BookStatsViewModel
    @Environment(\.dreamColors) private var colors

    init(viewModel: BookStatsViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    var body: some View {
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
                DreamEmptyStateView(
                    title: "book_stats_empty_title",
                    message: "book_stats_empty_body"
                )

            case .content(let stats):
                content(stats)

            case .error:
                DreamErrorStateView(
                    title: "book_stats_error_title",
                    message: String(localized: "book_stats_error_body"),
                    retryLabel: "book_retry",
                    onRetry: { Task { await viewModel.load(force: true) } }
                )
            }
        }
        .navigationTitle("book_stats_title")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.load(force: true) }
    }

    private func content(_ stats: BookStats) -> some View {
        ScrollView {
            VStack(spacing: DreamSpacing.md) {
                VStack(spacing: DreamSpacing.sm) {
                    ForEach(BookStatus.allCases) { status in
                        statusCard(status, count: stats.count(for: status))
                    }
                }
                averageCard(stats)
            }
            .padding(DreamSpacing.lg)
            // One merged summary, matching Android (cards are visual only).
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(stats.accessibilitySummary)
        }
    }

    private func statusCard(_ status: BookStatus, count: Int) -> some View {
        DreamCard(variant: .elevated) {
            HStack {
                Text(status.displayName)
                    .font(.dreamBodyLarge)
                    .foregroundStyle(colors.onSurface)
                Spacer()
                Text("\(count)")
                    .font(.dreamTitleLarge)
                    .foregroundStyle(colors.primary)
            }
        }
    }

    private func averageCard(_ stats: BookStats) -> some View {
        DreamCard(variant: .elevated) {
            VStack(alignment: .leading, spacing: DreamSpacing.xs) {
                Text("book_stats_average_label")
                    .font(.dreamBodyMedium)
                    .foregroundStyle(colors.onSurfaceVariant)
                if let averageText = stats.averageText {
                    Text(String(
                        format: String(localized: "book_stats_average_value"), averageText
                    ))
                        .font(.dreamHeadlineMedium)
                        .foregroundStyle(colors.primary)
                    Text(String(
                        format: String(localized: "book_stats_rated_count"), String(stats.ratedCount)
                    ))
                    .font(.dreamBodyMedium)
                    .foregroundStyle(colors.onSurfaceVariant)
                } else {
                    Text("book_stats_no_ratings")
                        .font(.dreamBodyLarge)
                        .foregroundStyle(colors.onSurface)
                }
            }
        }
    }
}
