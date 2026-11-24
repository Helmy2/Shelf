package com.shelf.feature.book.impl.di

import com.shelf.feature.book.api.navigation.BookDetailRoute
import com.shelf.feature.book.api.navigation.BookEditRoute
import com.shelf.feature.book.api.navigation.BookListRoute
import com.shelf.feature.book.api.navigation.BookStatsRoute
import com.shelf.feature.book.domain.usecase.AddBook
import com.shelf.feature.book.domain.usecase.DeleteBook
import com.shelf.feature.book.domain.usecase.ObserveBook
import com.shelf.feature.book.domain.usecase.ObserveBookStats
import com.shelf.feature.book.domain.usecase.ObserveBooks
import com.shelf.feature.book.domain.usecase.UpdateBook
import com.shelf.feature.book.impl.BookDeleteAnnouncer
import com.shelf.feature.book.impl.BookDetailScreen
import com.shelf.feature.book.impl.BookDetailViewModel
import com.shelf.feature.book.impl.BookFormScreen
import com.shelf.feature.book.impl.BookFormViewModel
import com.shelf.feature.book.impl.BookListScreen
import com.shelf.feature.book.impl.BookListViewModel
import com.shelf.feature.book.impl.BookPresentationMapper
import com.shelf.feature.book.impl.BookStatsScreen
import com.shelf.feature.book.impl.BookStatsViewModel
import com.shelf.shared.navigation.Navigator
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.koin.plugin.module.dsl.viewModel

@OptIn(KoinExperimentalAPI::class)
val bookUiModule = module {
    factory<BookPresentationMapper> { BookPresentationMapper() }
    factory<ObserveBooks> { ObserveBooks(get()) }
    factory<ObserveBookStats> { ObserveBookStats(get()) }
    factory<ObserveBook> { ObserveBook(get()) }
    factory<AddBook> { AddBook(get()) }
    factory<UpdateBook> { UpdateBook(get()) }
    factory<DeleteBook> { DeleteBook(get()) }
    singleOf(::BookDeleteAnnouncer)
    viewModel<BookListViewModel>()
    viewModel<BookFormViewModel>()
    viewModel<BookDetailViewModel>()
    viewModel<BookStatsViewModel>()

    navigation<BookListRoute> {
        val navigator = koinInject<Navigator>()
        BookListScreen(
            onNavigateToAdd = { navigator.goTo(BookEditRoute()) },
            onNavigateToDetail = { id -> navigator.goTo(BookDetailRoute(id)) },
            onNavigateToStats = { navigator.goTo(BookStatsRoute) },
        )
    }

    navigation<BookStatsRoute> {
        val navigator = koinInject<Navigator>()
        BookStatsScreen(
            onNavigateBack = { navigator.goBack() },
            onNavigateToAdd = { navigator.goTo(BookEditRoute()) },
        )
    }

    navigation<BookDetailRoute> { route ->
        val navigator = koinInject<Navigator>()
        BookDetailScreen(
            bookId = route.id,
            onNavigateToEdit = { id -> navigator.goTo(BookEditRoute(id)) },
            onNavigateBack = { navigator.goBack() },
            onDeleted = { navigator.goBack() },
        )
    }

    navigation<BookEditRoute> { route ->
        val navigator = koinInject<Navigator>()
        BookFormScreen(bookId = route.id, onClose = { navigator.goBack() })
    }
}
