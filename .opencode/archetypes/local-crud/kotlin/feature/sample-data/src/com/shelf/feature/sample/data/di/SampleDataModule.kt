package com.shelf.feature.sample.data.di

import com.shelf.feature.sample.data.local.SampleDatabase
import com.shelf.feature.sample.data.local.SampleDatabaseFactory
import com.shelf.feature.sample.data.local.SampleItemDao
import com.shelf.feature.sample.data.local.createSampleDatabase
import com.shelf.feature.sample.data.repository.RoomSampleRepository
import com.shelf.feature.sample.domain.repository.SampleRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Data-layer wiring owned by the sample feature. Needs `TimeProvider` from the shared core module.
 */
fun sampleDataModule(sampleDatabaseFactory: SampleDatabaseFactory) = module {
    single { sampleDatabaseFactory }
    single<SampleDatabase> { createSampleDatabase(get()) }
    single<SampleItemDao> { get<SampleDatabase>().sampleItemDao() }
    singleOf(::RoomSampleRepository).bind<SampleRepository>()
}
