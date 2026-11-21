package com.shelf.feature.sample.domain.repository

import com.shelf.feature.sample.domain.model.SampleItem
import com.shelf.feature.sample.domain.model.SampleCategory
import com.shelf.shared.core.domain.Result

interface SampleRepository {
    suspend fun loadItems(): Result<List<SampleItem>>

    suspend fun loadItem(id: String): Result<SampleItem?>

    suspend fun createItem(
        title: String,
        description: String,
        category: SampleCategory,
    ): Result<SampleItem>

    suspend fun updateItem(item: SampleItem): Result<SampleItem>
}
