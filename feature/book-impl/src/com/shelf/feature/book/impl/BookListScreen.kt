package com.shelf.feature.book.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shelf.shared.core.strings.book_empty_filtered_body
import com.shelf.shared.core.strings.book_empty_filtered_title
import com.shelf.shared.core.strings.book_filter_all
import com.shelf.shared.core.strings.book_filter_label
import com.shelf.shared.core.strings.book_filter_result_a11y
import com.shelf.shared.core.strings.book_filter_show_all
import com.shelf.shared.core.strings.Res
import com.shelf.shared.core.strings.book_delete_success_a11y
import org.jetbrains.compose.resources.getString
import com.shelf.shared.core.strings.book_add
import com.shelf.shared.core.strings.book_empty_body
import com.shelf.shared.core.strings.book_empty_title
import com.shelf.shared.core.strings.book_error_body
import com.shelf.shared.core.strings.book_error_title
import com.shelf.shared.core.strings.book_list_title
import com.shelf.shared.core.strings.book_loading_title
import com.shelf.shared.core.strings.book_retry
import com.shelf.shared.core.strings.book_row_accessibility
import com.shelf.shared.core.strings.book_row_accessibility_no_author
import com.shelf.shared.core.strings.book_row_open_hint
import com.shelf.shared.core.strings.book_row_accessibility_rated
import com.shelf.shared.core.strings.book_row_accessibility_no_author_rated
import com.shelf.shared.designsystem.AppTheme
import com.shelf.shared.designsystem.DreamTheme
import com.shelf.shared.designsystem.components.Button
import com.shelf.shared.designsystem.components.ButtonVariant
import com.shelf.shared.designsystem.components.Card
import com.shelf.shared.designsystem.components.CardVariant
import com.shelf.shared.designsystem.components.CircularProgressIndicator
import com.shelf.shared.designsystem.components.TopAppBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookListScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: BookListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val pendingTitle by viewModel.pendingDeleteTitle.collectAsState()
    var deleteAnnouncement by remember { mutableStateOf("") }
    LaunchedEffect(pendingTitle) {
        pendingTitle?.let { title ->
            // Spoken politely once the list is showing again after the detail was popped.
            deleteAnnouncement = getString(Res.string.book_delete_success_a11y, title)
            viewModel.consumeDeleteAnnouncement(title)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                BookListEvent.NavigateToAdd -> onNavigateToAdd()
                is BookListEvent.NavigateToDetail -> onNavigateToDetail(event.id)
            }
        }
    }

    BookListContent(
        state = state,
        onAction = viewModel::onAction,
        deleteAnnouncement = deleteAnnouncement,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListContent(
    state: BookListState,
    onAction: (BookListAction) -> Unit,
    modifier: Modifier = Modifier,
    deleteAnnouncement: String = "",
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.book_list_title),
                actions = {
                    Button(
                        text = stringResource(Res.string.book_add),
                        onClick = { onAction(BookListAction.OpenAdd) },
                        variant = ButtonVariant.Tertiary,
                    )
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        // Zero-size polite live region for the post-delete announcement; present in every state.
        Box(
            Modifier
                .size(0.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = deleteAnnouncement
                },
        )
        when (val contentState = state.contentState) {
            BookListContentState.Loading -> Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.md, Alignment.CenterVertically),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(Res.string.book_loading_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            BookListContentState.Empty -> Box(
                modifier = contentModifier.padding(DreamTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    title = stringResource(Res.string.book_empty_title),
                    body = stringResource(Res.string.book_empty_body),
                    variant = CardVariant.Filled,
                    actionLabel = stringResource(Res.string.book_add),
                    onAction = { onAction(BookListAction.OpenAdd) },
                )
            }

            is BookListContentState.Content -> BookListLoaded(
                state = state,
                content = contentState,
                onAction = onAction,
                modifier = contentModifier,
            )

            BookListContentState.Error -> Box(
                modifier = contentModifier.padding(DreamTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    title = stringResource(Res.string.book_error_title),
                    body = stringResource(Res.string.book_error_body),
                    variant = CardVariant.Filled,
                    accentColor = MaterialTheme.colorScheme.error,
                    actionLabel = stringResource(Res.string.book_retry),
                    onAction = { onAction(BookListAction.Retry) },
                )
            }
        }
    }
}

@Composable
private fun BookListLoaded(
    state: BookListState,
    content: BookListContentState.Content,
    onAction: (BookListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val filter = state.selectedFilter
    val announcement = filterAnnouncement(filter, content)
    // Announce only when the selection changes (not on add or on returning to this screen).
    var announced by remember { mutableStateOf("") }
    var lastFilter by remember { mutableStateOf(filter) }
    LaunchedEffect(filter) {
        if (filter != lastFilter) {
            lastFilter = filter
            announced = announcement
            listState.scrollToItem(0)
        }
    }
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = DreamTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.md),
        ) {
            item(key = "filter-header", contentType = "filter-header") {
                BookFilterChips(
                    selected = filter,
                    onSelect = { onAction(BookListAction.SelectFilter(it)) },
                    modifier = Modifier.padding(horizontal = DreamTheme.spacing.lg),
                )
            }
            if (content.items.isEmpty()) {
                item(key = "filter-empty", contentType = "filter-empty") {
                    FilteredEmptyContent(
                        filter = filter,
                        onShowAll = { onAction(BookListAction.SelectFilter(BookListFilter.All)) },
                        modifier = Modifier.padding(horizontal = DreamTheme.spacing.lg),
                    )
                }
            } else {
                items(content.items, key = { it.id }) { book ->
                    BookRow(
                        book,
                        onClick = { onAction(BookListAction.OpenBook(book.id)) },
                        modifier = Modifier.padding(horizontal = DreamTheme.spacing.lg),
                    )
                }
            }
        }
        // Zero-size polite live region: spoken when its text changes after a selection change.
        Box(
            Modifier
                .size(0.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announced
                },
        )
    }
}

@Composable
private fun filterAnnouncement(filter: BookListFilter, content: BookListContentState.Content): String =
    stringResource(
        Res.string.book_filter_result_a11y,
        filter.label(),
        content.items.size.toString(),
        content.unfilteredCount.toString(),
    )

@Composable
private fun BookListFilter.label(): String = when (this) {
    BookListFilter.All -> stringResource(Res.string.book_filter_all)
    BookListFilter.WantToRead -> BookStatusUi.WantToRead.label()
    BookListFilter.Reading -> BookStatusUi.Reading.label()
    BookListFilter.Finished -> BookStatusUi.Finished.label()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookFilterChips(
    selected: BookListFilter,
    onSelect: (BookListFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupLabel = stringResource(Res.string.book_filter_label)
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
    ) {
        BookListFilter.entries.forEach { filter ->
            BookFilterChip(
                label = filter.label(),
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

/** Feature-local selectable chip (no design-system chip yet); selection is never colour only. */
@Composable
private fun BookFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(percent = 50)
    val contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) colors.primaryContainer else Color.Transparent, shape)
            .border(1.dp, if (selected) colors.primaryContainer else colors.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = DreamTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.xs),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else null,
        )
    }
}

@Composable
private fun FilteredEmptyContent(
    filter: BookListFilter,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            title = stringResource(Res.string.book_empty_filtered_title),
            body = stringResource(Res.string.book_empty_filtered_body, filter.label()),
            variant = CardVariant.Filled,
        ) {
            Button(
                text = stringResource(Res.string.book_filter_show_all),
                onClick = onShowAll,
                variant = ButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun BookRow(book: BookItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val statusLabel = book.status.label()
    val rating = book.rating?.takeIf { book.status == BookStatusUi.Finished }
    val description = when {
        rating != null && book.author.isBlank() ->
            stringResource(Res.string.book_row_accessibility_no_author_rated, book.title, statusLabel, rating.toString())

        rating != null ->
            stringResource(Res.string.book_row_accessibility_rated, book.title, book.author, statusLabel, rating.toString())

        book.author.isBlank() -> stringResource(Res.string.book_row_accessibility_no_author, book.title, statusLabel)
        else -> stringResource(Res.string.book_row_accessibility, book.title, book.author, statusLabel)
    }
    val openHint = stringResource(Res.string.book_row_open_hint)
    Card(
        variant = CardVariant.Elevated,
        onClick = onClick,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick(label = openHint) {
                onClick()
                true
            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.md),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.xs),
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (book.author.isNotBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (rating != null) {
                    // Glyph plus number, never colour only; the row's combined label carries the spoken rating.
                    Text(
                        text = "\u2605 $rating",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BookListContentPreview() {
    AppTheme {
        BookListContent(
            state = BookListState(
                BookListContentState.Content(
                    listOf(
                        BookItemUi(1, "Dune", "Frank Herbert", BookStatusUi.Finished, rating = 4),
                        BookItemUi(2, "Untitled notebook", "", BookStatusUi.WantToRead),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
