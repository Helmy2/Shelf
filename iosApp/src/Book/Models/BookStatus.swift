import Foundation

enum BookStatus: String, CaseIterable, Identifiable {
    case wantToRead
    case reading
    case finished

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .wantToRead: String(localized: "book_status_want_to_read")
        case .reading: String(localized: "book_status_reading")
        case .finished: String(localized: "book_status_finished")
        }
    }
}
