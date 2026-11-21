package com.shelf.feature.sample.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SampleRoute : NavKey

@Serializable
data class SampleDetailRoute(val id: String) : NavKey
