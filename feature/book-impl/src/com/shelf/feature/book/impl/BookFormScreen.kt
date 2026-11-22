package com.shelf.feature.book.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import com.shelf.shared.core.strings.book_notes_label
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.shelf.shared.core.strings.Res
import com.shelf.shared.core.strings.book_add_title
import com.shelf.shared.core.strings.book_author_label
import com.shelf.shared.core.strings.book_cancel
import com.shelf.shared.core.strings.book_details_header
import com.shelf.shared.core.strings.book_edit_title
import com.shelf.shared.core.strings.book_save
import com.shelf.shared.core.strings.book_save_error_body
import com.shelf.shared.core.strings.book_save_error_title
import com.shelf.shared.core.strings.book_status_label
import com.shelf.shared.core.strings.book_title_label
import com.shelf.shared.core.strings.book_title_required
import com.shelf.shared.core.strings.book_title_required_hint
import com.shelf.shared.designsystem.AppTheme
import com.shelf.shared.designsystem.DreamTheme
import com.shelf.shared.designsystem.components.Button
import com.shelf.shared.designsystem.components.ButtonVariant
import com.shelf.shared.designsystem.components.Card
import com.shelf.shared.designsystem.components.CardVariant
import com.shelf.shared.designsystem.components.SegmentedButton
import com.shelf.shared.designsystem.components.TextField
import com.shelf.shared.designsystem.components.TopAppBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val NOTES_MIN_LINES = 3
private const val NOTES_MAX_LINES = 8

@Composable
fun BookFormScreen(
    bookId: Long?,
    onClose: () -> Unit,
    viewModel: BookFormViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel, bookId) {
        viewModel.onAction(BookFormAction.Load(bookId))
    }
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                BookFormEvent.Saved, BookFormEvent.Cancelled -> onClose()
            }
        }
    }

    BookFormContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookFormContent(
    state: BookFormState,
    onAction: (BookFormAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(state.titleFocusRequests, state.loadState) {
        if (state.titleFocusRequests > 0 && state.loadState == BookFormLoadState.Ready) {
            titleFocus.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(
                    if (state.isEdit) Res.string.book_edit_title else Res.string.book_add_title,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            when (state.loadState) {
                BookFormLoadState.Ready -> BookFormFields(
                    state = state,
                    onAction = onAction,
                    titleFocus = titleFocus,
                    modifier = Modifier.weight(1f),
                )

                BookFormLoadState.Loading -> BookLoadingContent(Modifier.weight(1f))
                BookFormLoadState.NotFound -> BookNotFoundContent(
                    onBack = { onAction(BookFormAction.Cancel) },
                    modifier = Modifier.weight(1f),
                )

                BookFormLoadState.Error -> BookLoadErrorContent(
                    onRetry = { onAction(BookFormAction.RetryLoad) },
                    modifier = Modifier.weight(1f),
                )
            }
            BookFormBottomBar(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun BookFormFields(
    state: BookFormState,
    onAction: (BookFormAction) -> Unit,
    titleFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val titleRequired = stringResource(Res.string.book_title_required)
    val statusLabel = stringResource(Res.string.book_status_label)
    val notesLabel = stringResource(Res.string.book_notes_label)
    val notesFocus = remember { FocusRequester() }
    val notesBringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val statusOptions = BookStatusUi.entries
    val statusTexts = statusOptions.map { it.label() }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DreamTheme.spacing.lg, vertical = DreamTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.md),
    ) {
        Text(
            text = stringResource(Res.string.book_details_header),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        TextField(
            value = state.title,
            cursorAtEnd = true,
            onValueChange = { onAction(BookFormAction.TitleChanged(it)) },
            placeholder = stringResource(Res.string.book_title_label),
            isError = state.titleError,
            supportingText = if (state.titleError) titleRequired else stringResource(Res.string.book_title_required_hint),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .focusRequester(titleFocus)
                .semantics {
                    if (state.titleError) {
                        error(titleRequired)
                        liveRegion = LiveRegionMode.Polite
                    }
                },
        )
        TextField(
            value = state.author,
            onValueChange = { onAction(BookFormAction.AuthorChanged(it)) },
            placeholder = stringResource(Res.string.book_author_label),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            // Next skips the non-text Status and Rating controls and lands on Notes.
            keyboardActions = KeyboardActions(onNext = { notesFocus.requestFocus() }),
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedButton(
            selectedIndex = statusOptions.indexOf(state.status),
            onOptionSelect = { onAction(BookFormAction.StatusChanged(statusOptions[it])) },
            options = statusTexts,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = statusLabel },
        )
        if (state.ratingError && !state.showRating) {
            BookRatingErrorText()
        }
        BookRatingSelector(
            visible = state.showRating,
            rating = state.rating,
            hasError = state.ratingError,
            onRatingChange = { onAction(BookFormAction.RatingChanged(it)) },
        )
        TextField(
            value = state.notes,
            onValueChange = { onAction(BookFormAction.NotesChanged(it)) },
            placeholder = notesLabel,
            singleLine = false,
            minLines = NOTES_MIN_LINES,
            maxLines = NOTES_MAX_LINES,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            modifier = Modifier
                .bringIntoViewRequester(notesBringIntoView)
                .focusRequester(notesFocus)
                .onFocusChanged { if (it.isFocused) scope.launch { notesBringIntoView.bringIntoView() } }
                .semantics { contentDescription = notesLabel },
        )
        if (state.saveError) {
            Card(
                title = stringResource(Res.string.book_save_error_title),
                body = stringResource(Res.string.book_save_error_body),
                variant = CardVariant.Filled,
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun BookFormBottomBar(
    state: BookFormState,
    onAction: (BookFormAction) -> Unit,
) {
    val showSave = state.loadState == BookFormLoadState.Ready || state.loadState == BookFormLoadState.Loading
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DreamTheme.spacing.lg, vertical = DreamTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
    ) {
        if (showSave) {
            Button(
                text = stringResource(Res.string.book_save),
                onClick = { onAction(BookFormAction.Save) },
                enabled = !state.isSaving && state.loadState == BookFormLoadState.Ready,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            text = stringResource(Res.string.book_cancel),
            onClick = { onAction(BookFormAction.Cancel) },
            variant = ButtonVariant.Tertiary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewLightDark
@Composable
private fun BookFormContentPreview() {
    AppTheme {
        BookFormContent(
            state = BookFormState(title = "", titleError = true),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun BookFormFinishedPreview() {
    AppTheme {
        BookFormContent(
            state = BookFormState(
                title = "Dune",
                status = BookStatusUi.Finished,
                rating = 4,
                notes = "A classic.",
                bookId = 1,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun BookFormEditPreview() {
    AppTheme {
        BookFormContent(
            state = BookFormState(title = "Dune", author = "Frank Herbert", bookId = 1),
            onAction = {},
        )
    }
}
