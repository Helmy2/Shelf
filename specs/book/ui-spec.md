# UI Spec: book (Shelf)

Slice 1 covers the list and the add form. Later slices extend this file; do not rewrite it.

## Book list (`BookListRoute`, start destination)

| State | Content |
|-------|---------|
| Loading | Centered progress indicator with `book_loading_title`. Top bar and Add action stay visible. |
| Empty | `book_empty_title`, `book_empty_body`, and an Add button (`book_add`). |
| Content | One row per book, newest first. Row: title (max 2 lines), author (hidden when empty), status text label. |
| Error | `book_error_title`, `book_error_body`, `book_retry` action. |

- **Android:** Scaffold, `TopAppBar` with a Tertiary `Button` for Add, rows as `Card` + `ListItem`. Use `shared.designsystem` components and `DreamTheme` tokens (`spacing.lg`, `spacing.md`, `spacing.sm`) before raw Material3.
- **iOS:** `NavigationStack`, toolbar plus button, `DreamEmptyStateView`, `DreamErrorStateView`, `List`, pull-to-refresh.
- **Slice 1 rows are not tappable:** no ripple, chevron or button semantics.
- **Accessibility:** each row is one combined element, labelled with `book_row_accessibility` or `book_row_accessibility_no_author`.
- **Extension points:** slice 2 makes rows tappable; slice 3 adds the rating in the row's trailing area; slice 4 adds filter chips as the first list item or header plus a per-filter empty state (`book_empty_filtered_*`, `book_filter_*`); slice 5 only brings the Empty state back after the last delete.

## Add form (`BookEditRoute(id = null)`)

Fields: Title (required, IME Next), Author (optional, IME Done), Status picker (Want to read / Reading / Finished, default Want to read).

| State | Behavior |
|-------|----------|
| Content | Form opens with Title focused. |
| Validation error | Blank or whitespace-only title shows `book_title_required`. Nothing is saved, focus returns to Title, the error clears when the user types a non-blank character. |
| Saving | Save disabled. |
| General error | Inline `book_save_error_title` + `book_save_error_body`; entered data is kept. |
| Success | Form closes; the new book is the top row of the list. |

- Save is disabled only while saving, never for an empty title, so the validation message stays discoverable.
- **Android:** full-screen route (not a sheet) with a pinned bottom bar holding Save and Cancel; bar and content respect IME insets so Save stays reachable with the keyboard open.
- **iOS:** modal sheet with a `Form`; Cancel and Save as toolbar items. Swipe-down counts as Cancel.
- Cancel discards the draft with no confirmation in slice 1.
- **Accessibility:** focus order Title, Author, Status, Save, Cancel. The status group is labelled; errors are announced.
- Slice 2 reuses this form for edit with `book_edit_title`.

## String keys (identical on Android `strings.xml` and iOS `Localizable.xcstrings`)

Format args: `%1$s` in XML, `%1$@` in xcstrings.

| Key | English |
|-----|---------|
| `book_list_title` | Shelf |
| `book_add` | Add book |
| `book_loading_title` | Loading your books |
| `book_empty_title` | Your shelf is empty |
| `book_empty_body` | Add the first book you want to read, are reading, or have finished. |
| `book_error_title` | Couldn't load your books |
| `book_error_body` | Something went wrong while loading your shelf. |
| `book_retry` | Retry |
| `book_status_want_to_read` | Want to read |
| `book_status_reading` | Reading |
| `book_status_finished` | Finished |
| `book_row_accessibility` | %1$s by %2$s, %3$s |
| `book_row_accessibility_no_author` | %1$s, %2$s |
| `book_add_title` | Add book |
| `book_details_header` | Details |
| `book_title_label` | Title |
| `book_author_label` | Author (optional) |
| `book_status_label` | Status |
| `book_title_required` | Title is required |
| `book_title_required_hint` | Required |
| `book_save` | Save |
| `book_cancel` | Cancel |
| `book_save_error_title` | Couldn't save the book |
| `book_save_error_body` | Something went wrong. Check your entry and try again. |

## Slice 2: Book detail and edit

Replaces the slice 1 note "rows are not tappable". Slice 1 sections are otherwise unchanged.

### List rows become tappable
- Tap opens `BookDetailRoute(id)`. Row content unchanged: title (max 2 lines), author (hidden when empty), status label.
- **Android:** rows use the design-system `Card` with `onClick` (replaces the raw `ElevatedCard`); `ListItem` stays as content; no extra chevron.
- **iOS:** each row is a `NavigationLink` (system chevron), or a full-width button row with explicit chevron if navigation is ViewModel-driven.
- **Accessibility:** same combined label (`book_row_accessibility*`), button role, hint `book_row_open_hint`.
- **Extension point:** slice 3 puts the rating in the trailing area before the chevron/affordance.

### Book detail (`BookDetailRoute(id)`)

| State | Trigger | User sees |
|-------|---------|-----------|
| Loading | First emission of `observeBook(id)` not yet arrived | Centered progress with `book_loading_title`; back visible; Edit hidden |
| Not found | `observeBook` emits null | `book_not_found_title`, `book_not_found_body`, `book_back_to_shelf` button; Edit hidden |
| Content | Book exists | Details below; Edit visible |
| Error | `observeBook` flow fails | `book_detail_error_title`, `book_detail_error_body`, `book_retry`; Edit hidden |

- **Content:** title as large heading (wraps, no truncation); author as secondary text, hidden entirely when empty; status as a labelled row (`book_status_label` + `book_status_*` text, never colour only). Reserve space below for slice 3 (rating, notes) and slice 5 (Delete, destructive, at the bottom); show nothing there in slice 2.
- **Behavior:** observes the book, so edits show without reload; if the book disappears, moves to Not found; single scrollable column.
- **Android:** Scaffold with `TopAppBar`, back icon (content description `book_back`), Tertiary `Button` `book_edit` as the Edit action; `spacing.lg` padding, `spacing.sm` between title/author/status; system back and back icon return to the list.
- **iOS:** pushed on the list's `NavigationStack`; Edit as trailing toolbar button (`book_edit`) presenting the form as a modal sheet; `DreamEmptyStateView` / `DreamErrorStateView` for Not found / Error.
- **Accessibility:** title is a heading; read order title, author, status ("Status" + value); Edit announced as button; focus order back, title, author, status, Edit.

### Edit form (`BookEditRoute(id)`, form reused)

Route `id` is non-null for edit. Fields, order, IME actions and status picker unchanged.

| State | Behavior |
|-------|----------|
| Loading | Edit only: centered progress with `book_loading_title`; Save disabled; Cancel/Back available |
| Content | Prefilled title, author, status; Title focused, cursor at end; screen title `book_edit_title` |
| Not found | Book gone on load or while saving: show detail Not found content; nothing saved |
| Validation error | As slice 1 (`book_title_required`, focus returns to Title, clears on non-blank input) |
| Saving | Save disabled |
| General error | Inline `book_save_error_title` + `book_save_error_body`; data kept |
| Success | Closes to the detail showing updated values; list row updates too |

- Save keeps `id` and `createdAtMillis`, so list order does not change. Cancel discards with no confirmation. The form is prefilled once; later repository emissions never overwrite what the user is typing.
- **Android:** full-screen route with pinned bottom bar and IME insets; pops back to the detail on success.
- **iOS:** modal sheet from the detail with Cancel/Save toolbar items; swipe-down is Cancel; navigation title `book_edit_title`; dismisses to the detail on success.
- **Accessibility:** focus order Title, Author, Status, Save, Cancel; loading announced; prefilled values read with labels; validation errors announced.

### New string keys (identical on Android and iOS)

Reused from slice 1: `book_loading_title`, `book_retry`, `book_status_*`, `book_status_label`, `book_title_label`, `book_author_label`, `book_save`, `book_cancel`, `book_title_required`, `book_save_error_*`, `book_row_accessibility*`.

| Key | English |
|-----|---------|
| `book_detail_title` | Book details |
| `book_edit` | Edit |
| `book_edit_title` | Edit book |
| `book_back` | Back |
| `book_row_open_hint` | Opens book details |
| `book_not_found_title` | Book not found |
| `book_not_found_body` | This book is no longer on your shelf. |
| `book_back_to_shelf` | Back to shelf |
| `book_detail_error_title` | Couldn't load this book |
| `book_detail_error_body` | Something went wrong while loading the book. |

## Slice 3: Rating and notes

Fills the slots reserved in slice 2. Domain rules are in the contract's slice 3 change note (rating optional, cleared when status is not Finished, 1..5 when set).

### Add/edit form

- **Field order:** Title, Author, Status, Rating (only when Status is Finished), Notes. **Focus order:** Title, Author, Status, Rating (when visible), Notes, Save, Cancel.
- **IME:** Title Next. Author changes from Done to Next (Notes follows; Next skips the non-text Status/Rating controls). Notes is multi-line with default IME action (Return = newline, no Done); keyboard dismissed by tapping outside, scrolling, or Save/Cancel.
- **Rating selector (Finished only):** `book_rating_label` heading, five equal star segments labelled 1-5, a Clear control (`book_rating_clear`). Selected value fills stars 1..n, unfilled are outlined; the value is also text (`book_rating_value`, "3 of 5", or `book_rating_none` "Not rated"), never colour only. Tapping star n selects n; tapping the selected star keeps it; only Clear unsets (enabled only when a rating is set). New book or a book switched to Finished starts unset. Targets at least 48dp (Android) / 44pt (iOS); Clear wraps to a second line when narrow. Spacing `spacing.sm` between targets, `spacing.md` between label row and stars.
- **Status changes:** switching to Want to read or Reading hides the selector and sets the form rating to null immediately; the hidden value is never saved. Switching back to Finished shows it unset (not remembered). Editing a Finished book with a rating prefills it. Short fade/size animation, none with reduce motion. Focus stays on Status.
- **Notes:** labelled (`book_notes_label`) multi-line field for all statuses; min height about 3 lines, grows to about 8 then scrolls inside; no counter or validation; saved as typed. Android scrolls it into view on focus.
- **Keyboard/primary action:** Android bottom bar stays pinned above the keyboard (IME insets) and the scroll content has bottom padding equal to the bar height. iOS Save/Cancel are toolbar items; keyboard dismisses on scroll-drag.
- **Components:** check `shared.designsystem` for a rating/star component first; if none, build the selector in the feature UI from design-system icon buttons or toggles with DreamTheme colour roles (primary filled, outline unfilled) and spacing tokens, no raw values. Android Notes uses the design-system text field (multi-line). iOS: `Form` section, five star buttons plus Clear; Notes as `TextEditor` or vertical-axis `TextField`.
- **Errors:** `RatingOutOfRange` shows `book_rating_out_of_range` inline under the rating row (under Status if the selector is hidden); unreachable from the selector but handled generically; nothing saved, data kept; cleared by fixing the value or status. General error unchanged. No new loading states.
- **Accessibility:** Android: selectable group labelled `book_rating_label`; each star a radio-button role with description `book_rating_star_a11y` and selected state; Clear a button. iOS: one adjustable element (label `book_rating_label`, value `book_rating_value` or `book_rating_none`; increment/decrement by 1 within 1..5, decrement from 1 is a no-op) plus a separate Clear button. Show/hide of the selector is announced; Notes announced with label and text; inline rating error announced as an alert.

### Book detail

Order: title, author, status, rating, notes.
- **Rating row:** only when status is Finished and a rating is set: `book_rating_label` + `book_rating_value` ("4 of 5") plus five decorative stars hidden from accessibility. Finished with no rating: row hidden (no "Not rated" on the detail). Read as one element, "Rating, 4 of 5".
- **Notes section:** `book_notes_label` heading (heading trait) and selectable body text (`SelectionContainer` / `textSelection(.enabled)`); long notes wrap; hidden entirely when empty or whitespace-only.
- Delete slot (slice 5) stays below notes. Rows spaced `spacing.sm`; notes section separated by `spacing.lg`. States unchanged.

### Book list rows

- Finished book with a rating shows "★ 4" (glyph plus number) in the trailing area: Android `ListItem` trailing slot; iOS trailing in the row's `HStack` before the system chevron. Hidden otherwise (title keeps the full width, 2-line limit). Notes are not shown in rows.
- Combined label extended: `book_row_accessibility_rated` (with author), `book_row_accessibility_no_author_rated` (without). Unrated rows keep `book_row_accessibility` and `book_row_accessibility_no_author`. Hint and button role stay.

### New string keys (slice 3)

Args: `%1$s` in XML; in xcstrings use plain `%@` (positional `%1$@` fails the parity check, same argument order).

| Key | English |
|-----|---------|
| `book_rating_label` | Rating |
| `book_rating_value` | %1$s of 5 |
| `book_rating_none` | Not rated |
| `book_rating_clear` | Clear rating |
| `book_rating_star_a11y` | %1$s of 5 stars |
| `book_rating_out_of_range` | Choose a rating from 1 to 5 |
| `book_notes_label` | Notes |
| `book_row_accessibility_rated` | %1$s by %2$s, %3$s, rated %4$s of 5 |
| `book_row_accessibility_no_author_rated` | %1$s, %2$s, rated %3$s of 5 |

## Slice 4: Status filter

Presentation-only (contract slice 4 change note). Fills the filter slot reserved in slice 1.

### Filter control

- **Options:** All, Want to read, Reading, Finished; single selection; All on every launch. Chip text reuses `book_status_*` plus `book_filter_all`.
- **Placement:** first item of the list's scrolling content, above the rows; scrolls away with the list (a pinned control costs vertical space on small phones, large text and when wrapped). The per-filter empty state sits below it in the same scroll container; iOS pull-to-refresh keeps working.
- **Visibility:** shown whenever at least one book exists (unfiltered count >= 1). Hidden in Loading, Error and the no-books-at-all Empty state. If the shelf becomes empty (slice 5, last delete), `selectedFilter` resets to All.
- **Android:** the design system has no chip component and its `SegmentedButton` truncates "Want to read" on narrow widths and at large font scale, so build a feature-local selectable chip: rounded pill, leading check icon when selected, in a wrapping flow row. DreamTheme tokens only: selected = primary-container fill, on-primary-container text and check; unselected = transparent, outline border, on-surface text. Spacing `spacing.sm` between chips, `spacing.lg` horizontal padding, `spacing.md` below the row. Chip target at least 48dp tall; label-large type role. Promote to the design system if a second screen needs chips (follow-up, not this slice).
- **iOS:** horizontally scrolling row of capsule chips (not a segmented control; four labels truncate at larger Dynamic Type). First row of the list section with clear background, no separators, zero row insets aligned to the content margin. Body/subheadline Dynamic Type style, 44pt minimum height; at accessibility sizes the row keeps scrolling and the selected chip is scrolled into view. Same colour roles and spacing as Android.
- **Selected state is not colour only:** leading check mark plus stronger text weight; holds in light, dark and increased contrast.
- **Interaction:** tapping an unselected chip selects it and the list updates immediately; tapping the selected chip does nothing (no deselect); changing the selection scrolls the list to the top; order stays newest first. No animation beyond platform defaults (reduced with reduce motion).

### Accessibility

- Group labelled `book_filter_label`. Android: `selectableGroup`, each chip a radio-button role with selected state. iOS: each chip a button with the selected trait in a container with the same label.
- The check icon is decorative and hidden from accessibility.
- On selection change announce `book_filter_result_a11y` (chip label, visible count, total count), e.g. "Reading, 2 of 5 books shown": Android polite live region/announcement, iOS announcement notification. Not announced for unrelated count changes (adding a book).
- Focus order: top bar, Add, filter group (All first), rows (or the empty-state action).

### Per-filter empty state (books exist, none match)

Trigger: `selectedFilter` is not All and zero visible rows. Chips stay visible with the selection kept. Below: `book_empty_filtered_title`, `book_empty_filtered_body` (names the filter through its status label), and a "Show all" button (`book_filter_show_all`). Show all sets the filter to All; the All chip does the same. Android: slice 1 empty-state layout inside the scroll container with a secondary-style button (not the Add button). iOS: `DreamEmptyStateView` with the action below the chips in the same list. The top bar Add action stays. This never replaces the slice 1 empty state (no books at all).

### States and behavior

| State | Behavior |
|-------|----------|
| Loading / Error | Unchanged; filter hidden |
| No books at all | Slice 1 empty state with Add; filter hidden |
| Content (All) | Chips plus all rows, newest first |
| Content (filtered) | Chips plus matching rows only, same order |
| Filtered, no match | Per-filter empty state |

- Selection survives opening detail or the add/edit form and returning (it lives in the list view-model state); not persisted, so a relaunch resets to All.
- A book added or edited under an active filter that does not match is not shown after saving (no auto reset, no toast); a status edit can make it disappear from or appear in the filter.
- Rating, notes and row content unchanged from slices 2-3.

### Test-relevant notes

- Visible rows = observed books filtered by status, repository newest-first order preserved; repository, domain, data untouched.
- Chips visible when unfiltered count >= 1; per-filter empty state when unfiltered count >= 1 and filtered count = 0.
- Default All; SelectFilter with the current value is a no-op; selection resets to All when the shelf becomes empty.

### New string keys (slice 4)

Reused: `book_status_want_to_read`, `book_status_reading`, `book_status_finished`. Args: `%1$s` in XML; plain `%@` in xcstrings in the same order.

| Key | English |
|-----|---------|
| `book_filter_label` | Filter by status |
| `book_filter_all` | All |
| `book_filter_result_a11y` | %1$s, %2$s of %3$s books shown |
| `book_empty_filtered_title` | No matching books |
| `book_empty_filtered_body` | You don't have any books marked "%1$s". |
| `book_filter_show_all` | Show all |

## Slice 5: Delete

Presentation on top of the contract's slice 5 change note. `shared.designsystem` already has what is needed: `Button` with `ButtonVariant.Destructive`, `Dialog` (title, text, confirm and dismiss slots) and `ProgressIndicator`.

### Delete action (detail screen only)

- **Where:** the slot reserved at the bottom of the detail content, below notes, last item of the scrolling column, separated by `spacing.lg`. No delete on list rows, no swipe-to-delete, nothing in the top bar. Edit stays in the top bar or toolbar.
- **Visibility:** detail Content state only; hidden in Loading, Not found and Error.
- **Label:** text plus a leading trash icon, never icon-only (`book_delete`; icon decorative).
- **Android:** design-system `Button(variant = Destructive)`, full width, min height 48dp, horizontal padding `spacing.lg`, `error`/`onError` roles. Content bottom padding `spacing.lg` plus the navigation-bar inset.
- **iOS:** full-width bordered button in the destructive role (error colour role), its own bottom section or a plain button below the content; min height 44pt; scales with Dynamic Type and wraps.
- **Edit while deleting:** disabled (not hidden) so the layout does not jump.

### Confirmation dialog

- **Title** `book_delete_confirm_title`; **body** `book_delete_confirm_body` naming the book title in quotes (long titles wrap, never truncate).
- **Buttons:** Cancel (Tertiary/text, `book_cancel`) is the safe default; Delete (`book_delete`, destructive). Android initial focus on Cancel; Delete is never pre-focused; Enter/Return/default action never deletes.
- **Android:** design-system `Dialog`; `dismissButton` = Cancel, `confirmButton` = destructive `Button`. Back, outside tap and Cancel all call `onDismissRequest` (= Cancel: closes, book unchanged). While Deleting `onDismissRequest` is ignored.
- **iOS:** an `alert` (short yes/no about one named item; an action sheet is for choosing among actions): `Button(role: .cancel)` `book_cancel`, `Button(role: .destructive)` `book_delete`; no tap-outside dismissal.

### States

| State | Trigger | User sees |
|-------|---------|-----------|
| Idle | Detail Content | Delete and Edit enabled |
| Confirming | Tap Delete | Dialog/alert; no state change behind it |
| Deleting | Confirm tapped | Platform split below; Edit disabled |
| Success | Delete returns Success or NotFound | Detail closes, back on the list; book absent from every filter |
| Failure | Delete returns `BookError.Unknown` | Detail stays open, book unchanged, inline error above the Delete button |

- **Deleting, Android:** dialog stays open; Delete disabled with a small `ProgressIndicator` instead of its label; Cancel disabled; back/outside tap ignored (no double submit, no mid-flight dismiss).
- **Deleting, iOS:** the alert dismisses on tap; on the detail the Delete button is disabled with a progress view labelled `book_delete_progress`; Edit disabled.
- **Failure:** inline error block above the Delete button, `spacing.sm` gap, `error` colour text plus an icon (not colour only): `book_delete_error_title`, `book_delete_error_body`. Retry = the same Delete action (reopens the confirmation; the error clears when the dialog opens, and on a successful observe update). Not a toast.
- **Success on the list:** the row is gone; deleting the last book shows the slice 1 Empty state, chips hidden, `selectedFilter` reset to All (slice 4); otherwise the selected filter is kept. No undo, no snackbar.
- **NotFound:** treated as success, no error shown. If the detail observes the book disappearing first (observer emits null) it shows the slice 2 Not found content with `book_back_to_shelf`.

### Accessibility

- **Delete button:** role button, label `book_delete`; content description / accessibility label `book_delete_a11y` ("Delete <title>"). Read order title, author, status, rating, notes, error (if any), Delete. Focus order: back, title, author, status, Edit, Delete.
- **Dialog:** title announced on open, body after; Android initial focus Cancel, order Cancel then Delete; modal (background not focusable).
- **Focus return after Cancel:** to the Delete button (Android focus requester; iOS system).
- **Success:** after popping to the list announce `book_delete_success_a11y` (title captured at confirm time) as a polite live region (Android) or announcement notification (iOS); NotFound uses the same announcement.
- **Failure:** inline error announced as an alert (polite on Android; announcement on iOS); the Delete button stays the focus target and usable.
- **Deleting:** progress announced once as `book_delete_progress`; disabled controls expose the disabled state.

### Edge: not found while the edit form is open

Delete is not reachable from the edit form (Android: full-screen route above the detail; iOS: modal sheet over the detail). If the book disappears for any other reason the slice 2 Not found behavior applies (the form shows Not found, nothing saved), and a late Delete returns NotFound, treated as success.

### New string keys (slice 5)

Reused: `book_cancel`, `book_back_to_shelf`, `book_not_found_*`. Args: `%1$s` in XML; plain `%@` in xcstrings in the same order. In `strings.xml` write quotes as `&quot;` and apostrophes as plain `'` (no backslash escapes; gate check S5).

| Key | English |
|-----|---------|
| `book_delete` | Delete |
| `book_delete_a11y` | Delete %1$s |
| `book_delete_confirm_title` | Delete this book? |
| `book_delete_confirm_body` | "%1$s" will be removed from your shelf. This can't be undone. |
| `book_delete_progress` | Deleting book |
| `book_delete_error_title` | Couldn't delete the book |
| `book_delete_error_body` | Your book is still on your shelf. Try again. |
| `book_delete_success_a11y` | %1$s deleted |

## Slice 6: Reading stats

Read-only screen derived from the existing book list. No domain, data or schema change beyond the pure `BookStats` computation.

### Entry point

- A "Stats" action (`book_stats_action`) next to Add in the list top bar (Android Tertiary button) / toolbar (iOS), shown only when the list has at least one book (list Content state; never in Loading, Empty or Error).
- Tapping it pushes the stats screen (Android `BookStatsRoute`; iOS NavigationStack push). Back returns to the list with the filter selection unchanged.

### Screen

- Title `book_stats_title` ("Reading stats"); back arrow (Android top bar) / system back (iOS).
- One non-interactive card per status in order Want to read, Reading, Finished: status label (existing status labels) and the count.
- One average card: label `book_stats_average_label`; when at least one Finished book has a rating, the value `book_stats_average_value` ("4.5 out of 5", one decimal, same on both platforms) and `book_stats_rated_count` ("2 rated"); otherwise only `book_stats_no_ratings` ("No ratings yet"). Finished books without a rating are excluded from the average and the rated count; ratings on non-Finished books never exist (cleared by validation).
- Updates live (Android observes the repository flow; iOS reloads each time the screen appears).
- Non-interactive cards must not look disabled: full-contrast text in light and dark mode (design-system `Card` without `onClick` renders disabled; override the content colour).

### States

| State | Content |
|-------|---------|
| Loading | progress indicator with the existing `book_loading_title` on both platforms (no stats-specific loading key) |
| Empty | all three counts are 0: `book_stats_empty_title`, `book_stats_empty_body`; Android also an Add book button (iOS: none, accepted difference; the Stats action is hidden on an empty shelf, so this is reachable only after the last book is deleted while the screen is open) |
| Content | cards as above |
| Error | `book_stats_error_title`, `book_stats_error_body`, Retry (`book_retry`) |

### Accessibility

- Content is announced as one polite combined summary (`book_stats_summary_a11y`): "1 want to read, 1 reading, 2 finished. Average rating: 4.5 out of 5, 2 rated" (or "... No ratings yet"). Same sentence on both platforms.
- The summary is the only accessibility element of the Content state on both platforms: the visual status and average cards are hidden from accessibility (no per-card labels, so counts are never read twice).
- Rounding: the average is rounded half up to one decimal on both platforms (4.25 shows "4.3"). The same fixtures (4,5 -> "4.5"; 4,4,4,5 -> "4.3"; 3,4,4 -> "3.7"; 5 -> "5.0") are asserted in the Kotlin and the Swift tests.
- Back and Retry are standard focusable controls with the 48dp / 44pt minimum target.

### Acceptance checklist

1. Empty shelf: no Stats action.
2. With Want to read 1, Reading 1, Finished 2 rated 4 and 5: counts 1/1/2, "4.5 out of 5", "2 rated".
3. Only an unrated Finished book: "No ratings yet", no "out of 5".
4. Back returns to the list; filter selection unchanged.
5. Text is readable (not faded) in light and dark mode.

### Platform notes (slice 6)

- Cosmetic differences between platforms are accepted where the design system differs (card variant, value font size); behavior, copy, rounding and accessibility are identical.
- iOS computes the stats in Swift from the bridge's `loadBooks()` result (same pattern as the list); the Kotlin `computeBookStats` stays the domain source of truth, and the shared fixtures above guard against drift.
