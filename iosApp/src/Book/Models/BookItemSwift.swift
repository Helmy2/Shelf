import Foundation

struct BookItemSwift: Identifiable, Hashable {
    let id: Int64
    let title: String
    let author: String
    let status: BookStatus
    let rating: Int?
    let notes: String
    let createdAtMillis: Int64

    var accessibilityLabel: String {
        if let shownRating {
            if author.isEmpty {
                return String(
                    format: String(localized: "book_row_accessibility_no_author_rated"),
                    title, status.displayName, String(shownRating)
                )
            }
            return String(
                format: String(localized: "book_row_accessibility_rated"),
                title, author, status.displayName, String(shownRating)
            )
        }
        if author.isEmpty {
            return String(
                format: String(localized: "book_row_accessibility_no_author"),
                title, status.displayName
            )
        }
        return String(
            format: String(localized: "book_row_accessibility"),
            title, author, status.displayName
        )
    }

    /// Rating to show: only Finished books with a rating.
    var shownRating: Int? {
        status == .finished ? rating : nil
    }

    var hasNotes: Bool {
        !notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
