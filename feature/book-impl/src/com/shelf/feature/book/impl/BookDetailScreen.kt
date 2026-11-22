package com.shelf.feature.book.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.shelf.shared.core.strings.book_cancel
import com.shelf.shared.core.strings.book_delete
import com.shelf.shared.core.strings.book_delete_a11y
import com.shelf.shared.core.strings.book_delete_confirm_body
import com.shelf.shared.core.strings.book_delete_confirm_title
import com.shelf.shared.core.strings.book_delete_error_body
import com.shelf.shared.core.strings.book_delete_error_title
import com.shelf.shared.core.strings.book_delete_progress
import com.shelf.shared.designsystem.components.CircularProgressIndicator
import com.shelf.shared.designsystem.components.ComponentSize
import com.shelf.shared.designsystem.components.Dialog
import androidx.compose.foundation.text.selection.SelectionContainer
import com.shelf.shared.core.strings.book_notes_label
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.shelf.shared.core.strings.Res
import com.shelf.shared.core.strings.book_back
import com.shelf.shared.core.strings.book_detail_title
import com.shelf.shared.core.strings.book_edit
import com.shelf.shared.core.strings.book_status_label
import com.shelf.shared.designsystem.AppTheme
import com.shelf.shared.designsystem.DreamTheme
import com.shelf.shared.designsystem.components.Button
import com.shelf.shared.designsystem.components.ButtonVariant
import com.shelf.shared.designsystem.components.IconButton
import com.shelf.shared.designsystem.components.TopAppBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookDetailScreen(
    bookId: Long,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: BookDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel, bookId) {
        viewModel.onAction(BookDetailAction.Load(bookId))
    }
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BookDetailEvent.NavigateToEdit -> onNavigateToEdit(event.id)
                BookDetailEvent.NavigateBack -> onNavigateBack()
                BookDetailEvent.NavigateBackAfterDelete -> onDeleted()
            }
        }
    }

    BookDetailContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailContent(
    state: BookDetailState,
    onAction: (BookDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backDescription = stringResource(Res.string.book_back)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.book_detail_title),
                navigationIcon = {
                    IconButton(onClick = { onAction(BookDetailAction.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backDescription,
                        )
                    }
                },
                actions = {
                    if (state.contentState is BookDetailContentState.Content) {
                        Button(
                            text = stringResource(Res.string.book_edit),
                            onClick = { onAction(BookDetailAction.Edit) },
                            variant = ButtonVariant.Tertiary,
                            enabled = !state.isDeleting,
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
            BookDetailContentState.Loading -> BookLoadingContent(contentModifier)
            BookDetailContentState.NotFound -> BookNotFoundContent(
                onBack = { onAction(BookDetailAction.Back) },
                modifier = contentModifier,
            )

            is BookDetailContentState.Content -> BookDetailBody(
                book = contentState.book,
                state = state,
                onAction = onAction,
                modifier = contentModifier,
            )

            BookDetailContentState.Error -> BookLoadErrorContent(
                onRetry = { onAction(BookDetailAction.Retry) },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun BookDetailBody(
    book: BookDetailUi,
    state: BookDetailState,
    onAction: (BookDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteFocus = remember { FocusRequester() }
    var wasDialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.isDeleteDialogVisible) {
        // Focus returns to Delete after the dialog closes without deleting (Cancel, back, outside tap).
        if (wasDialogVisible && !state.isDeleteDialogVisible && !state.isDeleting) {
            runCatching { deleteFocus.requestFocus() }
        }
        wasDialogVisible = state.isDeleteDialogVisible
    }
    val statusLabel = stringResource(Res.string.book_status_label)
    val statusText = book.status.label()
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(DreamTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
    ) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (book.author.isNotBlank()) {
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = "$statusLabel, $statusText" },
            horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (book.status == BookStatusUi.Finished && book.rating != null) {
            BookRatingRow(rating = book.rating)
        }
        if (book.notes.isNotBlank()) {
            Column(
                modifier = Modifier.padding(top = DreamTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(Res.string.book_notes_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                SelectionContainer {
                    Text(text = book.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        BookDeleteSection(
            title = book.title,
            state = state,
            onAction = onAction,
            deleteFocus = deleteFocus,
        )
    }
    if (state.isDeleteDialogVisible) {
        BookDeleteDialog(title = book.title, isDeleting = state.isDeleting, onAction = onAction)
    }
}

@Composable
private fun BookDeleteSection(
    title: String,
    state: BookDetailState,
    onAction: (BookDetailAction) -> Unit,
    deleteFocus: FocusRequester,
) {
    val deleteDescription = stringResource(Res.string.book_delete_a11y, title)
    val progressDescription = stringResource(Res.string.book_delete_progress)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DreamTheme.spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
    ) {
        if (state.hasDeleteError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Column {
                    Text(
                        text = stringResource(Res.string.book_delete_error_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(Res.string.book_delete_error_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Button(
            text = stringResource(Res.string.book_delete),
            onClick = { onAction(BookDetailAction.RequestDelete) },
            variant = ButtonVariant.Destructive,
            enabled = !state.isDeleting,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = DreamTheme.spacing.sm),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .focusRequester(deleteFocus)
                .semantics {
                    contentDescription = if (state.isDeleting) progressDescription else deleteDescription
                },
        )
    }
}

@Composable
private fun BookDeleteDialog(
    title: String,
    isDeleting: Boolean,
    onAction: (BookDetailAction) -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    val progressDescription = stringResource(Res.string.book_delete_progress)
    LaunchedEffect(Unit) { runCatching { cancelFocus.requestFocus() } }
    Dialog(
        title = stringResource(Res.string.book_delete_confirm_title),
        text = stringResource(Res.string.book_delete_confirm_body, title),
        // Back, outside tap and Cancel all land here; ignored while the delete runs.
        onDismissRequest = { if (!isDeleting) onAction(BookDetailAction.CancelDelete) },
        dismissButton = {
            Button(
                text = stringResource(Res.string.book_cancel),
                onClick = { onAction(BookDetailAction.CancelDelete) },
                variant = ButtonVariant.Tertiary,
                enabled = !isDeleting,
                modifier = Modifier.focusRequester(cancelFocus),
            )
        },
        confirmButton = {
            Button(
                text = if (isDeleting) "" else stringResource(Res.string.book_delete),
                onClick = { onAction(BookDetailAction.ConfirmDelete) },
                variant = ButtonVariant.Destructive,
                enabled = !isDeleting,
                leadingIcon = if (isDeleting) {
                    { CircularProgressIndicator(modifier = Modifier.size(20.dp), size = ComponentSize.Small) }
                } else {
                    null
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { if (isDeleting) contentDescription = progressDescription },
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun BookDetailContentPreview() {
    AppTheme {
        BookDetailContent(
            state = BookDetailState(
                BookDetailContentState.Content(
                    BookDetailUi(1, "Dune", "Frank Herbert", BookStatusUi.Finished, rating = 4, notes = "A classic."),
                ),
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun BookDetailDeleteDialogPreview() {
    AppTheme {
        BookDetailContent(
            state = BookDetailState(
                BookDetailContentState.Content(BookDetailUi(1, "Dune", "Frank Herbert", BookStatusUi.Reading)),
                isDeleteDialogVisible = true,
                hasDeleteError = true,
            ),
            onAction = {},
        )
    }
}
