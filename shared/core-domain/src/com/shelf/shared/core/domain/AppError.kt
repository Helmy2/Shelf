package com.shelf.shared.core.domain

sealed interface AppError {
    data object Unknown : AppError
    data class Message(val value: String) : AppError
}
