package com.shelf.feature.sample.data.bridge

import com.shelf.feature.sample.data.repository.RoomSampleRepository
import com.shelf.feature.sample.domain.repository.SampleRepository
import com.shelf.feature.sample.data.local.IosSampleDatabaseFactory
import com.shelf.feature.sample.data.local.createSampleDatabase
import com.shelf.shared.core.domain.SystemTimeProvider

fun iosSampleRepository(): SampleRepository {
    val database = createSampleDatabase(IosSampleDatabaseFactory())
    return RoomSampleRepository(
        dao = database.sampleItemDao(),
        timeProvider = SystemTimeProvider,
    )
}
