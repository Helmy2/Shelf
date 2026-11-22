package com.shelf.shared.di

import com.shelf.feature.book.api.navigation.BookListRoute
import com.shelf.feature.book.data.di.bookDataModule
import com.shelf.feature.book.data.local.BookDatabaseFactory
import com.shelf.feature.book.impl.di.bookUiModule
import com.shelf.shared.navigation.Navigator
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module


val navigatorModule = module {
    single { Navigator(startDestination = BookListRoute) }
}

private var hasStartedKoin = false

/**
 * Composition root. Feature modules are listed in [startKoin]'s `modules(...)` call: add each feature's
 * data module and UI module here, and give [initKoin] whatever platform factories they need.
 */
fun initKoin(
    bookDatabaseFactory: BookDatabaseFactory,
    appContext: Any? = null,
    appDeclaration: KoinAppDeclaration = {}
) {
    if (hasStartedKoin) return

    startKoin {
        appDeclaration()
        modules(
            coreModule(appContext),
            bookDataModule(bookDatabaseFactory),
            bookUiModule,
            navigatorModule,
        )
    }
    hasStartedKoin = true
}
