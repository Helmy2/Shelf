# Retrospective: book (Shelf reading log)

Feature built in 5 slices on `dream/feature/book` (archetype `local-crud`), reviewed by `dream-inspector` (approved after one repair round).

## What went well
- Vertical slicing worked: every slice ended with a green full gate on Android and iOS plus device checks, and each was a small, reviewable commit.
- Putting the full `BookItem` shape in slice 1 meant slices 2-3 extended behavior without schema changes (Room stayed at v1).
- Per-slice device runs caught real bugs the unit tests and the gate could not: stale Add form (shared ViewModel across routes), literal backslashes from `\'` / `\"` in `strings.xml`.

## What went wrong or cost time
- **Stale form state (slice 2):** the starter's `NavDisplay` had no ViewModel-store decorator, so `koinViewModel()` was activity-scoped. The first fix (keyed ViewModels) leaked; the inspector found it and the decorator was added in the repair round. Template improvement: install the Navigation 3 entry decorators in the starter's `AppNavigation`, and say in the archetype that screens use plain `koinViewModel()`.
- **Escaped quotes in strings (slice 4):** the designer and implementers wrote Android-style `\'` / `\"` into Compose Resources, which renders them verbatim. The gate's S4 stripped backslashes and hid it. Fixed: S5 check added, S4 compares raw values. Template improvement: put the string-escape rule in the strings best-practice file and in the designer brief.
- **Designer had no write tool:** `dream-designer` could not write `ui-spec.md` in three slices; the orchestrator saved the text. Template improvement: give the designer a write tool or document that the orchestrator writes the spec.
- **Maestro:** flows were first written for slice 1 only and extended in the repair round; the Android driver collided with mobile-mcp's UiAutomation until the run order was fixed (Maestro first, `pkill uiautomator`, cold emulator restart after `adb emu kill` waits out the AVD lock). A stale `/sdcard/u.xml` dump misled one tester observation ("Delete this book?" dialog). Template improvement: write Maestro coverage per slice, and add the procedure to the verification guidance.
- **Shared-module edits** (designsystem `TextField`, `shared/navigation`, `libs.versions.toml`, gate scripts) happened during slices; the inspector flagged them. Prefer opt-in parameters over changing shared defaults.
- **Environment:** the xcodebuild destination `iPhone 17 Pro` in `AGENTS.md` does not exist on this machine; the gate picks an available simulator. A spend-limit error interrupted one tester run; it was resumed from the start.

## Not verified (carried in the verification report)
ViewModel clearing after a pop, TalkBack/VoiceOver, Dynamic Type at accessibility sizes, dark mode, the failed-delete state, rendering of the three error-title strings, focus return to Delete after Cancel, iOS rotation, full visual review against `ui-spec.md`. S2 (raw Material3 `Scaffold`/`Text`/`MaterialTheme`/`Icon`) is a documented warning.

## Follow-ups worth doing
- Promote the filter chips and star selector to `shared/designsystem` if a second screen needs them; add design-system wrappers for `Scaffold`/`Text` or change S2.
- Add a Swift VoiceOver check for the rating stars (XCUITest showed the SF Symbol label "Favorite").
- Consider a full-output Maestro Android run in the next gate (the last report relied on the final 40 lines).
