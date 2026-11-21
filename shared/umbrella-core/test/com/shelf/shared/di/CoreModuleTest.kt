package com.shelf.shared.di

import com.shelf.shared.core.domain.TimeProvider
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

class CoreModuleTest {
    @Test
    fun resolvesTimeProvider() {
        val koin = koinApplication {
            modules(coreModule())
        }.koin

        assertNotNull(koin.get<TimeProvider>())
    }
}
