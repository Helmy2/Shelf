package com.shelf.feature.book.impl

/** UI-layer status; the presentation layer never exposes the domain `BookStatus` to views. */
enum class BookStatusUi { WantToRead, Reading, Finished }

data class BookItemUi(
    val id: Long,
    val title: String,
    /** Empty when the book has no author; the row hides the author line. */
    val author: String,
    val status: BookStatusUi,
    /** 1..5 for a Finished book with a rating, otherwise null. */
    val rating: Int? = null,
)

data class BookDetailUi(
    val id: Long,
    val title: String,
    /** Empty when the book has no author; the detail hides the author line. */
    val author: String,
    val status: BookStatusUi,
    /** 1..5 for a Finished book with a rating, otherwise null. */
    val rating: Int? = null,
    /** Raw notes; the detail hides the section when blank. */
    val notes: String = "",
)
