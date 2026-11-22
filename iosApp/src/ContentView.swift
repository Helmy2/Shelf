import SwiftUI

struct ContentView: View {
    var body: some View {
        BookListView(viewModel: BookListViewModel(bridge: AppDependencies.shared.bookBridge))
    }
}
