# Plan: Book (Shelf reading log)

## Priority

`normal`

## Goal

A local-only reading log where users add books, track status, rating and notes, filter the list by status, and view or edit each book.

## Archetype

`local-crud`

- **Why it fits**: a user-owned collection stored on the device, with list, detail and create/edit screens, validation, offline-first, no backend (matches every `when_to_use` bullet; none of the `when_not_to_use` apply).
- **Deviations**:
  - Entity `BookItem` has title, author, status, rating (nullable), notes, createdAtMillis. Remove the demo `SampleCategory` enum and replace it with `BookStatus` (`WantToRead`, `Reading`, `Finished`).
  - Adds a status filter on the list (not in the archetype), as a ViewModel state on both platforms.
  - Rating rules in domain: rating is 1..5 and only allowed when status is `Finished`.
  - Android uses three ViewModels (list, form, detail) instead of the archetype's single shared one, because the form and detail are separate routes; a small `BookDeleteAnnouncer` singleton carries the deleted title to the list's announcement (Koin compiler plugin KOIN-D002 workaround). The domain error type is a feature-local `BookResult<T>` instead of the shared `Result`.
- **Demo content to drop**: the three seed rows (`seedIfNeeded`), `SampleCategory`, all `sample_*` strings, `placeholder_*` strings, the placeholder home screen. Maestro flows are rewritten for Shelf text.

## Modules

### Create
- `feature/book-domain` — `BookItem`, `BookStatus`, `BookRepository`, validation rules, use cases
- `feature/book-data` — Room database, DAO, entity, mapper, `RoomBookRepository`, `bookDataModule`, iOS bridge helpers
- `feature/book-api` — `BookListRoute`, `BookDetailRoute(id)`, `BookEditRoute(id?)` (NavKey only)
- `feature/book-impl` — Compose list, detail, form screens, ViewModel(s), UI models, mapper, `bookUiModule` (Android only)

### Modify
- `project.yaml` — four module entries
- `shared/umbrella-core/module.yaml` — export `-data` and `-domain`
- `shared/umbrella-ui/module.yaml` and `shared/umbrella-ui/src/**/di/UiModule.kt` — dependencies, start destination = `BookListRoute`, Koin modules, `initKoin` params
- `androidApp/src/**/MainActivity.kt` — pass the database factory to `initKoin`
- `shared/core-strings` — `book_*` strings; remove `placeholder_*`
- `iosApp/src/ContentView.swift`, `iosApp/src/Core/AppDependencies.swift`, `iosApp/src/Localizable.xcstrings`
- Delete `shared/umbrella-ui/src/**/home/PlaceholderHome.kt` and `placeholderHomeModule`
- `maestro/features/book/`, `AGENTS.md`, `README.md`, `maestro/README.md`
- iOS: new `iosApp/src/Book/{Models,ViewModels,Views,Bridge}` and `iosApp/test` Swift tests

## Screens

| Screen | Purpose | States |
|--------|---------|--------|
| Book list | Shows books, status filter chips (All / Want to read / Reading / Finished), add button, tap opens detail | Loading / Empty (no books, and "no books in this filter") / Content / Error |
| Book detail | Shows title, author, status, rating (finished only), notes; edit and delete actions (delete asks for confirmation) | Loading / Not found (deleted) / Content / Error |
| Add / edit form | Title (required), author, status picker, rating 1-5 (visible only when Finished), notes; save | Loading (edit) / Content / Validation error / Error |

## Architecture Decisions

- **Data flow**: one-way. Repository exposes `Flow<List<BookItem>>`, the ViewModel filters by the selected status, and the UI renders state.
- **Storage**: Room (schema v1, exported schema), one table `book_items`, database `shelf_book.db`. No destructive migration.
- **Time provider**: true. `createdAtMillis` is set through `TimeProvider` (list order: newest first). `FakeTimeProvider` in tests.
- **Backend**: disabled. No accounts, no network.
- **Cross-feature reuse**: none.
- **Validation (domain)**: title trimmed and non-blank, else `TitleRequired`. Rating, when present, is 1..5, else `RatingOutOfRange`. A rating on a non-finished book is cleared (normalized to null), not rejected: the form hides the rating field and clears it when the status changes away from Finished, and the domain normalizes it as well (contract slice 3).
- **Delete**: included (slice 5). Domain use case `DeleteBook(id)`; delete is triggered from the detail screen and always asks for confirmation first. After deleting, the app returns to the list.

## Dependencies

None new. Room, Koin, Navigation 3 and the design system are already in the starter.

## Slices

| # | Slice | Adds | Acceptance (observable) | Status |
|---|-------|------|-------------------------|--------|
| 1 | Foundation: list and add | All four modules with the full `BookItem` shape (incl. rating, notes), DI and navigation wiring, SwiftUI and bridge, tests, Maestro flow, placeholder home removed; list and a form with title, author, status | On both platforms: app opens to the Shelf list; empty state shows; adding a book with a title shows it in the list and it survives relaunch; saving an empty title shows an error | done (842ccce) |
| 2 | Detail and edit | `BookDetailRoute`, detail screen, edit mode of the form | Tapping a book opens its detail; Edit changes title, author or status and the list and detail update | done (7f2b64f) |
| 3 | Rating and notes | Rating selector (1-5, only when Finished), notes field, display in detail and list rows; rating rules | Setting a book to Finished reveals a 1-5 rating; rating cannot be outside 1-5; switching away from Finished clears the rating; notes save and show in detail | done (d9005b4) |
| 4 | Status filter | Filter chips on the list, per-filter empty state | Selecting Reading shows only reading books; All restores the list; an empty filter shows its own empty message | done (09dd080) |
| 5 | Delete | `DeleteBook` use case, repository delete, delete action with confirmation dialog on the detail screen | Delete on a book's detail asks to confirm; Cancel keeps the book; Confirm removes it, returns to the list, and it stays gone after relaunch; deleting the last book shows the empty state | done (33b30a1) |

## Per-Slice Loop

Each slice follows the standard loop: contract amendment, then (if needed) UI spec, then domain/data/api and tests, Compose and SwiftUI in parallel, wiring, the full `verify-feature.sh book` gate on Android and iOS, a device launch on both, and a commit on `dream/feature/book` as `book: slice N - <title>`. After slice 5: `dream-inspector` over the whole feature, then the merge summary.
