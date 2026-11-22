# Verification Report: book

## Slice 1 - foundation: list and add (2026-11-22)

Gate: `.opencode/scripts/verify-feature.sh book` (full): 41 passed, 0 FAILED, 1 warning, 0 not applicable, 0 not run.

| Item | Status |
|------|--------|
| Structure, architecture, wiring, strings, iOS structure, Maestro files (M, K, D, X, W, T, S1, S3, S4, I, E) | passed |
| `./kotlin build`, Android tests, iOS simulator tests, Android app build, iOS app build | passed |
| Swift tests (`appTests`, 13 tests, iPhone 18 Pro simulator) | passed |
| Android emulator: empty state, blank-title error, add "Dune", persistence after force-stop, logcat | passed |
| iOS simulator: empty state, blank-title error, add "Dune", persistence after terminate, logs, title focus | passed |
| Maestro iOS flow | passed |
| Maestro Android flow | blocked by environment (Maestro Android driver did not start on the emulator); the same steps were run manually and passed |
| Spec compliance against `ui-spec.md` (full visual review) | not run (checked acceptance behavior and states only; the inspector reviews the whole feature at the end) |
| S2 raw Material3 imports | warning, documented exception below |

### S2 exception (documented)

`BookListScreen.kt` and `BookFormScreen.kt` import `Scaffold`, `Text`, `MaterialTheme` and `ExperimentalMaterial3Api` directly; the design system has no wrapper for them. `BookListScreen.kt` also uses `ElevatedCard` because the design-system `Card` renders as disabled when it has no click handler, and slice 1 rows are not tappable. Slice 2 makes rows tappable and will switch the rows to the design-system `Card(variant = Elevated, onClick = ...)`. The Scaffold/Text/MaterialTheme usage remains and will be re-examined (design-system tokens or a wrapper) before the final review.

### Notes

- During the Android run a tap opened a "Delete this book?" dialog. That text and any delete code do not exist in the repository, and book rows have no click handler, so it did not come from the Shelf app (likely another app or system UI on the shared emulator). Not a Shelf defect.
- mobile-mcp's tree and screenshot twice lagged one read behind after Save; the next read showed the list. Treated as a tooling artifact.

## Slice 2 - detail and edit (2026-11-22)

Gate: `.opencode/scripts/verify-feature.sh book` (full), final run: 41 passed, 0 FAILED, 1 warning, 0 not applicable, 0 not run.

History: gate run 1 passed (39 passed, 0 FAILED) but the Android device check FAILED: Add book opened with stale form state after earlier form use (one BookFormViewModel shared across BookEditRoute entries), plus the edit prefill cursor started at position 0. Repair round 1 fixed both (per-entry ViewModel scoping via `rememberEntryViewModelKey`, state reset in `load()`, cursor at end via `shared/designsystem` TextField). Re-verified below.

| Item | Status |
|------|--------|
| Structure, architecture, wiring, strings, iOS structure, Maestro files | passed |
| `./kotlin build`, Android tests (book-impl 38), iOS simulator tests, both app builds, Swift tests (27 on iPhone 18 Pro) | passed |
| Android: add, row tap to detail, edit prefill (cursor at end), save updates detail and list, order unchanged, blank-title error, cancel, edit A then B, empty Add form after Add / Cancel / Edit, Add after Edit adds instead of overwriting, rotation keeps typed text, persistence, logcat | passed |
| iOS: add, row tap pushes detail, edit sheet prefill, save updates, swipe-down discards, persistence, logs | passed |
| Maestro iOS flow | passed |
| Maestro Android flow | blocked by environment (driver did not start on the emulator); the flows cover slice 1 only; steps were run manually and passed |
| iOS focus of the title field on edit-sheet open | not run (not separately observed in slice 2) |
| Spec compliance against `ui-spec.md` (full visual review) | not run (inspector reviews the whole feature at the end) |
| S2 raw Material3 imports | warning, documented exception (see slice 1): `Scaffold`, `Text`, `MaterialTheme`, `Icon`, `ExperimentalMaterial3Api` have no design-system wrapper in `BookStateContents.kt`, `BookListScreen.kt`, `BookDetailScreen.kt`, `BookFormScreen.kt`; the `ElevatedCard` exception is gone (rows use the design-system `Card`) |

### Notes for the final review

- Slice 2 changed a shared module: `shared/designsystem/.../components/TextField.kt` now wraps its value in a `TextFieldValue` with the selection at the end (public API unchanged).
- Keyed form/detail ViewModels live in the activity store until it finishes because `NavDisplay` has no ViewModel-store decorator. The cleaner fix (`rememberViewModelStoreNavEntryDecorator`, needs a `lifecycle-viewmodel-navigation3` dependency in `shared/navigation`) was not taken to avoid a shared dependency change; decide at review.
- iOS detail reloads when the edit sheet closes (bridge exposes one-shot loads), not a live observation.
- `iosApp/module.xcodeproj/project.pbxproj` churn from Toolchain regeneration is not committed.

## Slice 3 - rating and notes (2026-11-22)

Gate: `.opencode/scripts/verify-feature.sh book` (full): 41 passed, 0 FAILED, 1 warning, 0 not applicable, 0 not run. No repair rounds.

| Item | Status |
|------|--------|
| Structure, architecture, wiring, strings (43 keys in sync), iOS structure, Maestro files | passed |
| `./kotlin build`, Android tests (book-impl 53), iOS simulator tests, both app builds, Swift tests (37 on iPhone 18 Pro) | passed |
| Android: rating selector only for Finished (unset, star 4, Clear), status switch hides and clears (not remembered), multi-line notes (newline, grows then scrolls, Save reachable, Author Next to Notes), list row "★ n" only for Finished rated books, detail rating row and notes section visibility, edit prefill, status away from Finished clears the rating, whitespace-only notes hidden on detail, slice 1-2 regressions (empty Add form, blank title, cancel, edit A then B), rotation keeps typed text incl. notes, persistence after force-stop, logcat | passed |
| iOS: same scenarios (Return/Next focus flow, Clear, status switch, list/detail visibility, edit prefill, clearing, persistence, swipe-down discard, logs) | passed |
| Maestro iOS flow | passed |
| Maestro Android flow | blocked by environment (driver did not start on the emulator); manual run passed |
| TalkBack / VoiceOver, reduce motion | not run (accessibility trees inspected only) |
| iOS rotation; iOS whitespace-only notes and star-5-then-Clear-then-save | not run |
| Spec compliance against `ui-spec.md` (full visual review) | not run (inspector reviews the whole feature at the end) |
| S2 raw Material3 imports | warning, documented exception (see slices 1-2); `BookRatingViews.kt` now also contains raw `Text`/`Icon` usage (five files listed) |

### Notes for the final review

- **Behavior change from slice 1:** notes are no longer trimmed (contract slice 3: stored as given). Whitespace-only notes are stored and shown back in the edit form; the detail hides them.
- **Shared module change (second one):** `shared/designsystem/.../TextField.kt` gained an optional `maxLines` (default unchanged for single-line callers), so Notes can grow to about 8 lines then scroll. Slice 2 changed the same file (cursor at end).
- **Design system gap:** no rating/star component exists; the selector is built in `book-impl` from design-system `IconButton`/`Button`. Consider whether it belongs in `shared/designsystem`.
- **iOS star accessibility:** under XCUITest the star images expose `label="Favorite"` (SF Symbol default) instead of "N of 5 stars"; the container is meant to be one adjustable element with the rating as value. VoiceOver was not run; verify at review.
- **Android Clear button** always sits on its own line below the stars (spec allows wrapping when narrow).

## Slice 4 - status filter (2026-11-22)

Gate: `.opencode/scripts/verify-feature.sh book` (full), final run: 42 passed, 0 FAILED, 1 warning, 0 not applicable, 0 not run (includes the new S5).

History: gate run 1 passed (41 passed) but an Android device check found `book_empty_filtered_body` rendering literal backslashes (`You don\'t have any books marked \"Want to read\".`): `strings.xml` used Android-style `\'` / `\"` escapes, which Compose Multiplatform resources show verbatim. The same escape affected `book_error_title`, `book_save_error_title` and `book_detail_error_title` (shipped in slices 1-3; their error states were never rendered on a device). Repair round 1 replaced the escapes (plain `'`, `&quot;`) and added gate check S5 (fails on a backslash before `'` or `"` in composeResources strings files).

| Item | Status |
|------|--------|
| Structure, architecture, wiring, strings (49 keys in sync), iOS structure, Maestro files, S5 | passed |
| `./kotlin build`, Android tests (book-impl 61), iOS simulator tests, both app builds, Swift tests (47 on iPhone 18 Pro) | passed |
| Android: chips hidden with no books; All/Want to read/Reading/Finished filtering with newest-first order; per-filter empty state naming the filter (text verified without backslashes); Show all and All chip; tapping the selected chip is a no-op; selection kept after detail and after Cancel of the Add form; non-matching new book not listed under the active filter; relaunch resets to All; rotation keeps selection; slice 1-3 regressions; logcat | passed |
| iOS: same scenarios, selection kept after detail and sheet, relaunch resets, swipe-down discard, logs | passed |
| Maestro iOS flow | passed |
| Maestro Android flow | blocked by environment (UiAutomation collision with mobile-mcp; driver does not start); manual run passed |
| Rendering of the three error-title strings (`book_error_title`, `book_save_error_title`, `book_detail_error_title`) | not run (no safe way to force those error states without editing source); verified statically: plain-apostrophe values, S5 passes |
| Scroll-to-top on filter selection | not applicable (list too short to scroll during the run) |
| iOS pull-to-refresh with the chips row | not run (no crash, refresh not observable) |
| TalkBack / VoiceOver, Dynamic Type at accessibility sizes, dark mode, increased contrast | not run |
| `test-verify-feature.sh` (gate self-test) | passed (20 of 20); does not cover S5 (follow-up) |
| Spec compliance against `ui-spec.md` (full visual review) | not run (inspector reviews the whole feature at the end) |
| S2 raw Material3 imports | warning, documented exception (see slices 1-3) |

### Notes for the final review

- **Gate tooling edited:** `.opencode/scripts/verify-feature.sh` gained S5. Follow-ups: add an S5 case to `test-verify-feature.sh`, and S4's `unescape` strips backslashes before comparing Android and iOS values (this is how the bug slipped past S4); consider making S4 and S5 agree.
- Filter chips are a feature-local component in `book-impl` (the design system has no chip). Promote to `shared/designsystem` if a second screen needs it.
- iOS selected-chip fill is `primary.opacity(0.16)` because the iOS design tokens have no primary-container role.
- Filter selection lives in the list ViewModel (Android: Navigation entry scoped; iOS: `@State` view model) and resets on relaunch by design.

## Slice 5 - delete (2026-11-22)

Gate: `.opencode/scripts/verify-feature.sh book` (full): 42 passed, 0 FAILED, 1 warning, 0 not applicable, 0 not run (includes S4 and S5; 57 `book_` keys in sync). No repair rounds.

| Item | Status |
|------|--------|
| Structure, architecture, wiring, strings, iOS structure, Maestro files | passed |
| `./kotlin build`, Android tests (book-impl 71), iOS simulator tests, both app builds, Swift tests (55 on iPhone 18 Pro) | passed |
| Android: Delete only on the detail Content state (not in rows, no swipe-delete, not in forms); destructive button with trash icon; dialog title/body text (no backslashes); Cancel, back and outside tap keep the book; Delete returns to the list with no "Book not found" flash; persistence after force-stop; Reading filter kept with the per-filter empty state after deleting its only book; Show all; last book shows the slice 1 empty state, chips hidden, All selected for the next added book; edit/rating/notes regressions; logcat | passed |
| iOS: same scenarios (alert with Cancel and destructive Delete, pop to the list, persistence, filter behavior, last-book empty state, swipe-down discard, logs) | passed |
| Maestro iOS flow | passed |
| Maestro Android flow | blocked by environment (driver did not start on the emulator); manual run passed |
| Android focus return to Delete after Cancel; initial focus on Cancel | not run (needs TalkBack/focus inspection; the accessibility tree showed no focused element) |
| Failed-delete state (inline error, retry) and success announcements | not run (a failure cannot be forced without editing source; covered by ViewModel and Swift unit tests only) |
| TalkBack / VoiceOver, Dynamic Type at accessibility sizes, dark mode | not run |
| iOS notes field on a device in this run | not run (tap missed the field under the keyboard) |
| Spec compliance against `ui-spec.md` (full visual review) | not run (inspector reviews the whole feature next) |
| S2 raw Material3 imports | warning, documented exception (see slices 1-4) |

### Notes for the final review

- **"Delete this book?" dialog sighting (slice 1 run):** the tester later found that a leftover `/sdcard/u.xml` dump from 2026-11-21 (before this project's first feature) was being read in place of a fresh dump (`uiautomator dump` collides with mobile-mcp). That likely explains the slice 1 sighting; not proven for that run. In slice 5 every dialog was confirmed to belong to `com.shelf.android`, and the stale files were deleted.
- Delete hand-off on Android: `BookDeleteAnnouncer` (Koin singleton injected into the list and detail view models) carries the deleted title to the list's polite announcement because the Koin compiler plugin rejects lookups from composables (KOIN-D002).
- Android deleting state uses an empty-label `Button` with a spinner leading icon (the design-system `Button` needs a text argument).
- Possible double navigation-bar inset below the Delete section (`navigationBarsPadding()` plus Scaffold inset): visual check only.

## Inspector repair round 1 (2026-11-22)

Inspector findings (no critical): W1 keyed ViewModels leak, W2 shared TextField cursor behavior, W3 gate S4/S5 self-test hole, W4 Maestro covered add only; info: weak form tests, unused Ktor dependency in book-data, duplicated/verbose rating validation, plan wording. User chose: repair all.

Repairs: (W1) Navigation 3 ViewModel-store and saved-state entry decorators in `shared/navigation` (new `lifecycle-viewmodel-navigation3` dependency, `libs.versions.toml`), `EntryViewModelKey.kt` removed; (W2) `TextField` gained opt-in `cursorAtEnd` (default false = pre-slice-2 behavior), only the book title field uses it; (W3) S4 compares raw values, three new S5/S4 cases in `test-verify-feature.sh`; (W4) Maestro flows extended to slices 1-5 on both platforms, iOS filter steps swipe the chip row; info items: rating rule simplified with one shared normalizer, Ktor line removed from `book-data/module.yaml`, two form tests rewritten, plan wording aligned with the contract.

Final full gate after the code repairs: `verify-feature.sh book` 42 passed, 0 FAILED, 1 warning (S2), 0 not applicable, 0 not run; `test-verify-feature.sh` 23 passed, 0 failed. Afterwards only `maestro/features/book/ios.yaml` and `maestro/README.md` changed; `verify-feature.sh book --quick` after that: 35 passed, 0 FAILED, 1 warning (S2), 1 not run (B1, skipped by `--quick`).

| Item | Status |
|------|--------|
| Full gate and gate self-test (23 of 23, incl. new S4/S5 cases) | passed |
| Unit and Swift tests (book-domain 17, book-data 21, umbrella-core 2, book-impl 70, Swift via the gate) | passed |
| Android regression after the repairs: Add opens empty after Add/Cancel/Edit, Edit A then B, cursor at end of the edit title, rotation keeps typed text (Add and Edit, incl. Notes), typing in the middle of Author/Notes does not jump the cursor, repeated detail/form navigation, filter selection kept, delete flow and persistence, last book empty state, logcat | passed |
| iOS regression after the repairs: add with rating and notes, detail, edit sheet (cursor at end, swipe-down discard), filter, delete alert, relaunch persistence, crash reports | passed |
| Maestro iOS flow (slices 1-5, from the repo file, fresh build) | passed |
| Maestro Android flow (slices 1-5, from the repo file, fresh APK, cold-restarted emulator) | passed (the tester saw the last 40 lines of output; Maestro exits on the first failed step and the last step "Your shelf is empty" completed) |
| ViewModels actually cleared after a navigation pop (W1) | not run (no log or memory evidence; only the absence of stale state on screen) |
| TalkBack / VoiceOver, Dynamic Type at accessibility sizes, dark mode | not run |
| Failed-delete state, rendering of the three error-title strings, focus return to Delete after Cancel | not run |
| iOS rotation | not run |
| Spec compliance against `ui-spec.md` (full visual review) | not run |
| S2 raw Material3 imports | warning, documented exception |

### Notes

- Maestro flows depend on the device: the iOS filter steps swipe the chip row at 24% height (measured on iPhone 18 Pro) because "Finished" is clipped at the right edge at default text size; Android chips wrap and need no swipe.
- Maestro's `inputText` into the iOS multi-line Notes field leaves a leading space, so the flows match notes text with a regex (`.*Great read`); the app does not add the space (notes are stored as typed).
- Emulator: restarting right after `adb emu kill` can hit the AVD lock; wait for the lock to clear before relaunching.
- The mobile-mcp device tool was not available for the final rounds; those used `adb`, `xcrun simctl` and Maestro.
