package com.shelf.feature.sample.impl.di

import com.shelf.feature.sample.api.navigation.SampleDetailRoute
import com.shelf.feature.sample.api.navigation.SampleRoute
import com.shelf.feature.sample.impl.SampleDetailScreen
import com.shelf.feature.sample.impl.SampleListScreen
import com.shelf.feature.sample.impl.SamplePresentationMapper
import com.shelf.feature.sample.impl.SampleViewModel
import com.shelf.shared.navigation.Navigator
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.koin.plugin.module.dsl.viewModel

@OptIn(KoinExperimentalAPI::class)
val sampleUiModule = module {
    factory<SamplePresentationMapper> { SamplePresentationMapper(get()) }
    viewModel<SampleViewModel>()

    navigation<SampleRoute> {
        val navigator = koinInject<Navigator>()
        SampleListScreen(
            onNavigateDetail = { id -> navigator.goTo(SampleDetailRoute(id)) },
        )
    }

    navigation<SampleDetailRoute> { route ->
        val navigator = koinInject<Navigator>()
        SampleDetailScreen(
            selectedId = route.id,
            onNavigateBack = { navigator.goBack() },
        )
    }
}
