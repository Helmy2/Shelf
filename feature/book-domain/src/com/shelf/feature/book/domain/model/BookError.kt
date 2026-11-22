package com.shelf.feature.book.domain.model

sealed interface BookError {
    data object TitleRequired : BookError
    data object RatingOutOfRange : BookError
    data object NotFound : BookError
    data object Unknown : BookError
}

sealed interface BookResult<out T> {
    data class Success<T>(val value: T) : BookResult<T>
    data class Failure(val error: BookError) : BookResult<Nothing>
}
