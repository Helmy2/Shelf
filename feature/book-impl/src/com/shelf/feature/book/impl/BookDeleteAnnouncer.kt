package com.shelf.feature.book.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Hands the title of a just-deleted book from the detail screen to the list screen, which announces it
 * (polite live region) after the detail has been popped. Registered as a Koin singleton.
 */
class BookDeleteAnnouncer {
    private val _pendingTitle = MutableStateFlow<String?>(null)
    val pendingTitle: StateFlow<String?> = _pendingTitle

    fun post(title: String) {
        _pendingTitle.value = title
    }

    fun consume(title: String) {
        _pendingTitle.compareAndSet(title, null)
    }
}
