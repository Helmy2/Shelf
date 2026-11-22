package com.shelf.feature.book.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object BookListRoute : NavKey

/** Add form when [id] is null; edit mode (slice 2) when set. */
@Serializable
data class BookEditRoute(val id: Long? = null) : NavKey

/** Read-only detail of the book with the given [id]. */
@Serializable
data class BookDetailRoute(val id: Long) : NavKey
