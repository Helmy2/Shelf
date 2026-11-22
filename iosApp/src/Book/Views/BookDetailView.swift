import SwiftUI

struct BookDetailView: View {
    @State private var viewModel: BookDetailViewModel
    @Environment(\.dreamColors) private var colors
    @Environment(\.dismiss) private var dismiss

    init(viewModel: BookDetailViewModel) {
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

            case .notFound:
                BookNotFoundView(onBack: { dismiss() })

            case .content(let book):
                content(book)

            case .error:
                DreamErrorStateView(
                    title: "book_detail_error_title",
                    message: String(localized: "book_detail_error_body"),
                    retryLabel: "book_retry",
                    onRetry: { Task { await viewModel.load() } }
                )
            }
        }
        .navigationTitle("book_detail_title")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if viewModel.canEdit {
                ToolbarItem(placement: .primaryAction) {
                    Button("book_edit") { viewModel.startEdit() }
                        .disabled(!viewModel.canEdit)
                }
            }
        }
        .sheet(isPresented: $viewModel.showForm) {
            if let formViewModel = viewModel.formViewModel {
                BookFormView(
                    viewModel: formViewModel,
                    onClose: { Task { await viewModel.dismissForm() } }
                )
            }
        }
        .alert(
            "book_delete_confirm_title",
            isPresented: $viewModel.showDeleteConfirmation
        ) {
            Button("book_cancel", role: .cancel) {}
            Button("book_delete", role: .destructive) { viewModel.confirmDelete() }
        } message: {
            if case .content(let book) = viewModel.contentState {
                Text(String(format: String(localized: "book_delete_confirm_body"), book.title))
            }
        }
        .onChange(of: viewModel.didDelete) { _, deleted in
            guard deleted else { return }
            if let announcement = viewModel.deleteSuccessAnnouncement {
                UIAccessibility.post(notification: .announcement, argument: announcement)
            }
            dismiss()
        }
        .onChange(of: viewModel.hasDeleteError) { _, failed in
            guard failed else { return }
            UIAccessibility.post(
                notification: .announcement,
                argument: String(localized: "book_delete_error_title")
            )
        }
        .task { await viewModel.load() }
    }

    private func deleteSection(_ book: BookItemSwift) -> some View {
        VStack(alignment: .leading, spacing: DreamSpacing.sm) {
            if viewModel.hasDeleteError {
                HStack(alignment: .top, spacing: DreamSpacing.sm) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: DreamSpacing.xs) {
                        Text("book_delete_error_title")
                            .font(.dreamTitleMedium)
                        Text("book_delete_error_body")
                            .font(.dreamBodyMedium)
                    }
                }
                .foregroundStyle(colors.error)
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isStaticText)
            }
            Button(role: .destructive) {
                viewModel.requestDelete()
            } label: {
                Group {
                    if viewModel.isDeleting {
                        ProgressView()
                            .accessibilityLabel(Text("book_delete_progress"))
                    } else {
                        Label("book_delete", systemImage: "trash")
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 44)
            }
            .buttonStyle(.bordered)
            .disabled(viewModel.isDeleting)
            .accessibilityLabel(
                Text(String(format: String(localized: "book_delete_a11y"), book.title))
            )
        }
        .padding(.top, DreamSpacing.lg)
    }

    private func content(_ book: BookItemSwift) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DreamSpacing.sm) {
                Text(book.title)
                    .font(.dreamHeadlineMedium)
                    .foregroundStyle(colors.onSurface)
                    .accessibilityAddTraits(.isHeader)
                if !book.author.isEmpty {
                    Text(book.author)
                        .font(.dreamBodyLarge)
                        .foregroundStyle(colors.onSurfaceVariant)
                }
                HStack(spacing: DreamSpacing.xs) {
                    Text("book_status_label")
                        .foregroundStyle(colors.onSurfaceVariant)
                    Text(book.status.displayName)
                        .foregroundStyle(colors.primary)
                }
                .font(.dreamBodyMedium)
                .accessibilityElement(children: .combine)
                if let rating = book.shownRating {
                    HStack(spacing: DreamSpacing.xs) {
                        Text("book_rating_label")
                            .foregroundStyle(colors.onSurfaceVariant)
                        Text(String(format: String(localized: "book_rating_value"), String(rating)))
                            .foregroundStyle(colors.primary)
                        HStack(spacing: 2) {
                            ForEach(1...5, id: \.self) { value in
                                Image(systemName: value <= rating ? "star.fill" : "star")
                                    .foregroundStyle(colors.primary)
                            }
                        }
                        .accessibilityHidden(true)
                    }
                    .font(.dreamBodyMedium)
                    .accessibilityElement(children: .combine)
                }
                if book.hasNotes {
                    VStack(alignment: .leading, spacing: DreamSpacing.sm) {
                        Text("book_notes_label")
                            .font(.dreamTitleMedium)
                            .foregroundStyle(colors.onSurface)
                            .accessibilityAddTraits(.isHeader)
                        Text(book.notes)
                            .font(.dreamBodyLarge)
                            .foregroundStyle(colors.onSurface)
                            .textSelection(.enabled)
                    }
                    .padding(.top, DreamSpacing.lg)
                }
                deleteSection(book)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DreamSpacing.lg)
        }
    }
}

/// Shared Not found content for the detail screen and the edit sheet.
struct BookNotFoundView: View {
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: DreamSpacing.md) {
            DreamEmptyStateView(title: "book_not_found_title", message: "book_not_found_body")
            DreamButton("book_back_to_shelf", variant: .outlined, action: onBack)
                .fixedSize()
        }
    }
}
