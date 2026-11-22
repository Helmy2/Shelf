import Foundation

/// Composition root for the iOS app. View models take bridges as parameters; tests pass mocks.
final class AppDependencies {
    static let shared = AppDependencies()
    let bookBridge: any BookRepositoryBridge
    private init() { bookBridge = BookBridge() }
}
