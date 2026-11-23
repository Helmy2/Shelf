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
                    Text("book_stats_loading")
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
        }
    }

    private func statusCard(_ status: BookStatus, count: Int) -> some View {
        DreamCard(variant: .outlined) {
            HStack {
                Text(status.displayName)
                    .font(.dreamBodyLarge)
                    .foregroundStyle(colors.onSurface)
                Spacer()
                Text("\(count)")
                    .font(.dreamTitleMedium)
                    .foregroundStyle(colors.primary)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            String(
                format: String(localized: "book_stats_status_count_a11y"),
                status.displayName, String(count)
            )
        )
    }

    private func averageCard(_ stats: BookStats) -> some View {
        DreamCard(variant: .filled) {
            VStack(alignment: .leading, spacing: DreamSpacing.xs) {
                Text("book_stats_average_label")
                    .font(.dreamBodyMedium)
                    .foregroundStyle(colors.onSurfaceVariant)
                if let averageText = stats.averageText {
                    Text(averageText)
                        .font(.dreamTitleMedium)
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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(stats.accessibilitySummary)
    }
}
