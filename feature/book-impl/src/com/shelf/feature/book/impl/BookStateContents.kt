package com.shelf.feature.book.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shelf.shared.core.strings.Res
import com.shelf.shared.core.strings.book_back_to_shelf
import com.shelf.shared.core.strings.book_detail_error_body
import com.shelf.shared.core.strings.book_detail_error_title
import com.shelf.shared.core.strings.book_loading_title
import com.shelf.shared.core.strings.book_not_found_body
import com.shelf.shared.core.strings.book_not_found_title
import com.shelf.shared.core.strings.book_retry
import com.shelf.shared.designsystem.DreamTheme
import com.shelf.shared.designsystem.components.Card
import com.shelf.shared.designsystem.components.CardVariant
import com.shelf.shared.designsystem.components.CircularProgressIndicator
import org.jetbrains.compose.resources.stringResource

/** Centered progress with the loading title; shared by the detail and the edit form. */
@Composable
internal fun BookLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.md, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(Res.string.book_loading_title),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** "Book not found" with a way back to the shelf; shared by the detail and the edit form. */
@Composable
internal fun BookNotFoundContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(DreamTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            title = stringResource(Res.string.book_not_found_title),
            body = stringResource(Res.string.book_not_found_body),
            variant = CardVariant.Filled,
            actionLabel = stringResource(Res.string.book_back_to_shelf),
            onAction = onBack,
        )
    }
}

/** "Couldn't load this book" with Retry; shared by the detail and the edit form. */
@Composable
internal fun BookLoadErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(DreamTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            title = stringResource(Res.string.book_detail_error_title),
            body = stringResource(Res.string.book_detail_error_body),
            variant = CardVariant.Filled,
            accentColor = MaterialTheme.colorScheme.error,
            actionLabel = stringResource(Res.string.book_retry),
            onAction = onRetry,
        )
    }
}
