import SwiftUI

struct BookFormView: View {
    private enum Field: Hashable {
        case title
        case author
        case notes
    }

    @Bindable var viewModel: BookFormViewModel
    let onClose: () -> Void
    @Environment(\.dreamColors) private var colors
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @FocusState private var focus: Field?

    var body: some View {
        NavigationStack {
            formBody
            .navigationTitle(viewModel.isEditing ? "book_edit_title" : "book_add_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("book_cancel", action: onClose)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("book_save") {
                        Task {
                            await viewModel.save()
                            if viewModel.titleError { focus = .title }
                        }
                    }
                    .disabled(viewModel.isSaving || viewModel.isLoading || viewModel.notFound || viewModel.loadError)
                }
            }
            .onChange(of: viewModel.didSave) { _, saved in
                if saved { onClose() }
            }
            .task {
                await viewModel.load()
                if !viewModel.notFound, !viewModel.loadError { focus = .title }
            }
        }
        .interactiveDismissDisabled(viewModel.isSaving)
        .dreamTheme()
    }

    @ViewBuilder
    private var formBody: some View {
        if viewModel.isLoading {
            VStack(spacing: DreamSpacing.md) {
                DreamCircularProgressIndicator(size: .large)
                Text("book_loading_title")
                    .font(.dreamBodyMedium)
                    .foregroundStyle(colors.onSurfaceVariant)
            }
        } else if viewModel.notFound {
            BookNotFoundView(onBack: onClose)
        } else if viewModel.loadError {
            DreamErrorStateView(
                title: "book_detail_error_title",
                message: String(localized: "book_detail_error_body"),
                retryLabel: "book_retry",
                onRetry: { Task { await viewModel.load() } }
            )
        } else {
            Form {
                Section("book_details_header") {
                    DreamTextField(
                        "book_title_label",
                        text: $viewModel.title,
                        isError: viewModel.titleError,
                        errorMessage: "book_title_required",
                        accessibilityIdentifier: "book_title_field"
                    )
                    .focused($focus, equals: .title)
                    .submitLabel(.next)
                    .onSubmit { focus = .author }
                    .accessibilityHint(Text("book_title_required_hint"))

                    DreamTextField(
                        "book_author_label",
                        text: $viewModel.author,
                        accessibilityIdentifier: "book_author_field"
                    )
                    .focused($focus, equals: .author)
                    .submitLabel(.next)
                    .onSubmit { focus = .notes }
                }

                Section("book_status_label") {
                    DreamSegmentedPicker(selection: $viewModel.status) {
                        ForEach(BookStatus.allCases) { status in
                            Text(status.displayName).tag(status)
                        }
                    }
                    .accessibilityLabel(Text("book_status_label"))

                    if viewModel.status != .finished, viewModel.ratingError {
                        ratingErrorText
                    }
                }

                if viewModel.status == .finished {
                    Section {
                        BookRatingSelector(
                            rating: viewModel.rating,
                            onSelect: { viewModel.setRating($0) },
                            onClear: { viewModel.clearRating() },
                            onIncrement: { viewModel.incrementRating() },
                            onDecrement: { viewModel.decrementRating() }
                        )
                        if viewModel.ratingError {
                            ratingErrorText
                        }
                    }
                }

                Section("book_notes_label") {
                    DreamTextField(
                        "book_notes_label",
                        text: $viewModel.notes,
                        axis: .vertical,
                        lineLimit: 3...8,
                        accessibilityIdentifier: "book_notes_field"
                    )
                    .focused($focus, equals: .notes)
                    .accessibilityLabel(Text("book_notes_label"))
                }

                if viewModel.saveError {
                    Section {
                        VStack(alignment: .leading, spacing: DreamSpacing.xs) {
                            Text("book_save_error_title")
                                .font(.dreamTitleMedium)
                                .foregroundStyle(colors.error)
                            Text("book_save_error_body")
                                .font(.dreamBodyMedium)
                                .foregroundStyle(colors.error)
                        }
                        .accessibilityElement(children: .combine)
                    }
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.2), value: viewModel.status)
        }
    }

    private var ratingErrorText: some View {
        Text("book_rating_out_of_range")
            .font(.dreamBodyMedium)
            .foregroundStyle(colors.error)
            .accessibilityAddTraits(.isStaticText)
            .onAppear {
                UIAccessibility.post(
                    notification: .announcement,
                    argument: String(localized: "book_rating_out_of_range")
                )
            }
    }
}

/// Five star buttons plus Clear; the stars are one adjustable accessibility element.
struct BookRatingSelector: View {
    let rating: Int?
    let onSelect: (Int) -> Void
    let onClear: () -> Void
    let onIncrement: () -> Void
    let onDecrement: () -> Void
    @Environment(\.dreamColors) private var colors

    private var valueText: String {
        if let rating {
            return String(format: String(localized: "book_rating_value"), String(rating))
        }
        return String(localized: "book_rating_none")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DreamSpacing.md) {
            HStack {
                Text("book_rating_label")
                    .font(.dreamTitleMedium)
                    .foregroundStyle(colors.onSurface)
                Spacer()
                Text(valueText)
                    .font(.dreamBodyMedium)
                    .foregroundStyle(colors.onSurfaceVariant)
            }
            .accessibilityHidden(true)

            HStack(spacing: DreamSpacing.sm) {
                ForEach(1...5, id: \.self) { value in
                    Image(systemName: value <= (rating ?? 0) ? "star.fill" : "star")
                        .foregroundStyle(colors.primary)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .contentShape(Rectangle())
                        .onTapGesture { onSelect(value) }
                        .accessibilityIdentifier("book_rating_star_\(value)")
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("book_rating_label"))
            .accessibilityValue(Text(valueText))
            .accessibilityAdjustableAction { direction in
                switch direction {
                case .increment: onIncrement()
                case .decrement: onDecrement()
                @unknown default: break
                }
            }

            Button("book_rating_clear", action: onClear)
                .buttonStyle(.borderless)
                .frame(minHeight: 44)
                .disabled(rating == nil)
                .accessibilityIdentifier("book_rating_clear")
        }
    }
}
