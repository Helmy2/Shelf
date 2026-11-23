package com.shelf.feature.book.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.shelf.shared.core.strings.Res
import com.shelf.shared.core.strings.book_add
import com.shelf.shared.core.strings.book_back
import com.shelf.shared.core.strings.book_retry
import com.shelf.shared.core.strings.book_stats_average_label
import com.shelf.shared.core.strings.book_stats_average_value
import com.shelf.shared.core.strings.book_stats_empty_body
import com.shelf.shared.core.strings.book_stats_empty_title
import com.shelf.shared.core.strings.book_stats_error_body
import com.shelf.shared.core.strings.book_stats_error_title
import com.shelf.shared.core.strings.book_stats_no_ratings
import com.shelf.shared.core.strings.book_stats_rated_count
import com.shelf.shared.core.strings.book_stats_status_count_a11y
import com.shelf.shared.core.strings.book_stats_summary_a11y
import com.shelf.shared.core.strings.book_stats_title
import com.shelf.shared.designsystem.AppTheme
import com.shelf.shared.designsystem.DreamTheme
import com.shelf.shared.designsystem.components.Card
import com.shelf.shared.designsystem.components.CardVariant
import com.shelf.shared.designsystem.components.TopAppBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookStatsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: BookStatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                BookStatsEvent.NavigateBack -> onNavigateBack()
                BookStatsEvent.NavigateToAdd -> onNavigateToAdd()
            }
        }
    }
    BookStatsContent(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookStatsContent(
    state: BookStatsState,
    onAction: (BookStatsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backDescription = stringResource(Res.string.book_back)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.book_stats_title),
                navigationIcon = {
                    IconButton(onClick = { onAction(BookStatsAction.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backDescription,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (val contentState = state.contentState) {
            BookStatsContentState.Loading -> BookLoadingContent(contentModifier)

            BookStatsContentState.Empty -> Box(
                modifier = contentModifier.padding(DreamTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    title = stringResource(Res.string.book_stats_empty_title),
                    body = stringResource(Res.string.book_stats_empty_body),
                    variant = CardVariant.Filled,
                    actionLabel = stringResource(Res.string.book_add),
                    onAction = { onAction(BookStatsAction.AddBook) },
                )
            }

            is BookStatsContentState.Content -> BookStatsLoaded(contentState.stats, contentModifier)

            BookStatsContentState.Error -> Box(
                modifier = contentModifier.padding(DreamTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    title = stringResource(Res.string.book_stats_error_title),
                    body = stringResource(Res.string.book_stats_error_body),
                    variant = CardVariant.Filled,
                    accentColor = MaterialTheme.colorScheme.error,
                    actionLabel = stringResource(Res.string.book_retry),
                    onAction = { onAction(BookStatsAction.Retry) },
                )
            }
        }
    }
}

@Composable
private fun BookStatsLoaded(stats: BookStatsUi, modifier: Modifier = Modifier) {
    val averageSpoken = if (stats.averageText == null) {
        stringResource(Res.string.book_stats_no_ratings)
    } else {
        stringResource(Res.string.book_stats_average_label) + ": " +
            stringResource(Res.string.book_stats_average_value, stats.averageText) + ", " +
            stringResource(Res.string.book_stats_rated_count, stats.ratedCount.toString())
    }
    val summary = stringResource(
        Res.string.book_stats_summary_a11y,
        stats.wantToReadCount.toString(),
        stats.readingCount.toString(),
        stats.finishedCount.toString(),
        averageSpoken,
    )
    // One combined polite sentence; the visual rows below are hidden from accessibility.
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(DreamTheme.spacing.lg)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = summary
            },
        verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.md),
    ) {
        Card(variant = CardVariant.Elevated, modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            // The design-system Card is non-clickable here, so Material3 supplies the disabled content color.
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Column(verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm)) {
                    StatusCountRow(BookStatusUi.WantToRead.label(), stats.wantToReadCount)
                    StatusCountRow(BookStatusUi.Reading.label(), stats.readingCount)
                    StatusCountRow(BookStatusUi.Finished.label(), stats.finishedCount)
                }
            }
        }
        Card(variant = CardVariant.Elevated, modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Column(verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.xs)) {
                    Text(
                        text = stringResource(Res.string.book_stats_average_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (stats.averageText == null) {
                        Text(
                            text = stringResource(Res.string.book_stats_no_ratings),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.book_stats_average_value, stats.averageText),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(Res.string.book_stats_rated_count, stats.ratedCount.toString()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCountRow(label: String, count: Int) {
    val description = stringResource(Res.string.book_stats_status_count_a11y, label, count.toString())
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
    }
}

@PreviewLightDark
@Composable
private fun BookStatsContentPreview() {
    AppTheme {
        BookStatsContent(
            state = BookStatsState(BookStatsContentState.Content(BookStatsUi(3, 1, 4, 3, "4.2"))),
            onAction = {},
        )
    }
}
