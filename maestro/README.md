# Maestro Smoke Flows

Dream uses Maestro for black-box smoke validation after a platform build installs and launches successfully.

Smoke files are feature-owned. During feature generation, create or update flows under `maestro/features/<feature-name>/` with checks that match the generated feature contract.

Shelf's flows live in `maestro/features/book/` (`android.yaml`, `ios.yaml`, kept in sync) and cover all five slices in one scenario: empty state, blank-title validation, add "Dune"; add "Emma" by "Austen" as Finished with rating 4 and notes; row shows the rating; open the detail and check title, author, "Rating, 4 of 5" and notes; edit the title to "Emma Revised" and check detail and list; status filter chips (Finished, Want to read, the per-filter empty state with "Show all", and All); delete with the confirmation dialog (Cancel keeps the book, Delete removes it); deleting the last book returns to "Your shelf is empty".

On iOS the status filter chips are a horizontally scrolling row, so the filter steps swipe the chip row (`swipe` at 24% y) before tapping Finished and All; the 24% y-position was measured on iPhone 18 Pro and may need adjusting on other devices. Android chips wrap and need no swipe.

Selectors: iOS uses accessibility identifiers for form fields and rating stars (`book_title_field`, `book_author_field`, `book_notes_field`, `book_rating_star_N`) and text elsewhere; Android has no testTags, so it uses visible text and content descriptions (e.g. `4 of 5 stars`). List rows expose one combined label (`Emma by Austen, Finished, rated 4 of 5`, `Dune, Want to read`), so flows match rows by that label. Maestro text selectors are full-match regexes: escape parentheses and use `.*`. Use `.opencode/guidance/templates/maestro-smoke-checklist-template.md` when adding flows for new slices.

Required structure:

```text
maestro/features/<feature-name>/android.yaml
maestro/features/<feature-name>/ios.yaml
```

Every user-visible generated feature should have at least one Android smoke flow and one iOS smoke flow when the feature is supported on that platform. Do not rely on one global smoke file to cover multiple features. App-level startup smoke checks are optional and do not replace feature-level coverage.

## Reliable Editable-Form Patterns

Prefer smoke flows that prove user-visible success without relying on fragile keyboard internals.

For iOS single-line text replacement, this pattern is usually more reliable than `hideKeyboard`:

```yaml
- tapOn: "Title"
- eraseText
- inputText: "Updated title"
- pressKey: Enter
- tapOn: "Save changes"
```

For create and update forms, the strongest keyboard-related smoke proof is:

- enter text into fields
- assert the primary save action is visible or reachable while the software keyboard may still be open
- tap save without a manual dismiss step when possible
- assert success-state content, updated summary text, or list/detail state after save

Avoid treating direct keyboard-hidden assertions as the only proof. `hideKeyboard`, tapping static text, and exact currency-node assertions can be brittle on simulators.

For detail screens, assert rendered content rather than partial labels. Prefer destination title, item title, formatted amount, and full-row regex selectors such as `Occurred: .*` or `Created: .*`. Do not assume the app/window title is always exposed as visible Android text.

## Environment Fallbacks

If no Android device or emulator is attached, record Android smoke as `blocked by environment` and include the setup action needed. Do not mark it passed from `./kotlin build` success.

If Maestro `launchApp` fails on a physical Android device but `adb install` and `adb shell am start` work, treat that as a Maestro/device-driver blocker. Capture the working `adb` evidence, try an emulator-backed Maestro run when feasible, and record the physical-device Maestro result separately.

If iOS simulator keyboard behavior makes a flow unreliable, use the `pressKey: Enter` fallback for single-line fields and verify the completed user outcome rather than the keyboard state itself.

App IDs: Android `com.shelf.android`, iOS `com.shelf.ios`. Build the apps with `./kotlin build -m androidApp --platform=android` and `./kotlin build -m iosApp --platform=iosSimulatorArm64` (see `.opencode/guidance/reference/kotlin-toolchain.md` for the artifact paths).

Recommended use:

```bash
maestro test maestro/features/<feature-name>/android.yaml
maestro test maestro/features/<feature-name>/ios.yaml
```

Before running either command, boot or open the target emulator/simulator, install the app, and confirm the app launches with platform tooling. Maestro smoke flows validate user behavior after runtime launch is healthy; they are not a substitute for installing the app first.

Clear app state at the start of every smoke flow so seeded data, first-run UI, and local persistence are deterministic. Add `clearState` before `launchApp` unless a contract explicitly requires testing persisted state across launches.

If Maestro is not installed or a simulator/device is unavailable, record the check as `blocked by environment` in `specs/verification-report.md` instead of claiming validation passed.

When a flow exposes a reusable template issue or a newly reliable smoke pattern, capture it with `.opencode/guidance/templates/template-improvement-retrospective-template.md`.
