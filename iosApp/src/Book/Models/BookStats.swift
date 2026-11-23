import Foundation

struct BookStats: Equatable {
    let wantToReadCount: Int
    let readingCount: Int
    let finishedCount: Int
    let ratedCount: Int
    /// Average over Finished books with a rating; nil when none are rated.
    let averageRating: Double?

    var totalCount: Int { wantToReadCount + readingCount + finishedCount }

    func count(for status: BookStatus) -> Int {
        switch status {
        case .wantToRead: wantToReadCount
        case .reading: readingCount
        case .finished: finishedCount
        }
    }

    /// One-decimal average, e.g. "4.2"; nil when there are no ratings.
    var averageText: String? {
        guard let averageRating else { return nil }
        // Round half up to match Android's formatOneDecimal (String(format:) is half-to-even).
        let tenths = Int((averageRating * 10).rounded(.toNearestOrAwayFromZero))
        return "\(tenths / 10).\(tenths % 10)"
    }

    static func compute(from books: [BookItemSwift]) -> BookStats {
        let ratings = books.compactMap { $0.status == .finished ? $0.rating : nil }
        let average: Double? = ratings.isEmpty
            ? nil
            : Double(ratings.reduce(0, +)) / Double(ratings.count)
        return BookStats(
            wantToReadCount: books.filter { $0.status == .wantToRead }.count,
            readingCount: books.filter { $0.status == .reading }.count,
            finishedCount: books.filter { $0.status == .finished }.count,
            ratedCount: ratings.count,
            averageRating: average
        )
    }

    var accessibilitySummary: String {
        let averagePart: String
        if let averageText {
            averagePart = String(localized: "book_stats_average_label") + ": "
                + String(format: String(localized: "book_stats_average_value"), averageText) + ", "
                + String(format: String(localized: "book_stats_rated_count"), String(ratedCount))
        } else {
            averagePart = String(localized: "book_stats_no_ratings")
        }
        return String(
            format: String(localized: "book_stats_summary_a11y"),
            String(wantToReadCount), String(readingCount), String(finishedCount), averagePart
        )
    }
}
