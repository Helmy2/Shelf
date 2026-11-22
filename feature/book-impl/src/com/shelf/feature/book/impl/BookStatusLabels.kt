package com.shelf.feature.book.impl

import androidx.compose.runtime.Composable
import com.shelf.shared.core.strings.Res
import com.shelf.shared.core.strings.book_status_finished
import com.shelf.shared.core.strings.book_status_reading
import com.shelf.shared.core.strings.book_status_want_to_read
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BookStatusUi.label(): String = when (this) {
    BookStatusUi.WantToRead -> stringResource(Res.string.book_status_want_to_read)
    BookStatusUi.Reading -> stringResource(Res.string.book_status_reading)
    BookStatusUi.Finished -> stringResource(Res.string.book_status_finished)
}
