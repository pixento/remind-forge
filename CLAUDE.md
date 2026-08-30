# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Better Habits (`nl.pixento.betterhabits`) is a single-module Android app (Kotlin + Jetpack Compose)
that periodically alerts the user (vibration and/or ringtone) at a configurable, optionally
randomised interval — optionally limited to daily hours, and optionally paused while Do Not Disturb
is on or the phone is connected to a car. There is one screen (Settings) and no server/backend; all
state lives in local DataStore Preferences.

## Common commands

Run all commands from the repo root via the Gradle wrapper.

```bash
./gradlew build                                    # full build (assemble + check)
./gradlew testDebugUnitTest                         # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "nl.pixento.betterhabits.domain.NextTriggerCalculatorTest"
./gradlew testDebugUnitTest --tests "*.NextTriggerCalculatorTest.within window*"  # single test method
./gradlew connectedDebugAndroidTest                 # instrumented tests (app/src/androidTest), needs device/emulator
./gradlew assembleDebug                             # build a debug APK
./gradlew lint                                      # Android Lint
./gradlew :app:testDebugUnitTest -PrecordStoreGraphics=true   # render the Play listing graphics
./gradlew :app:packageReleaseNativeDebugSymbols     # Play's native debug symbols zip (builds the bundle)
```

Notes on the build:
- `compileSdk`/`targetSdk` = 37, `minSdk` = 24. Kotlin 2.2.10, AGP 9.3.1, Compose BOM 2026.02.01.
- Unit tests are pinned to JDK 21 (`Test.javaLauncher`) and Kotlin compiles to bytecode target 11,
  whatever JDK the Gradle daemon itself runs on — Robolectric's bundled ASM can't parse class files
  from very new JDKs. Don't "fix" this pinning when a newer local JDK is present; it's intentional.
- `ndk.debugSymbolLevel` is deliberately **not** set, and Play's "no debug symbols" warning is
  expected to stay: the bundle's only native code is prebuilt AndroidX published already stripped,
  and AGP extracts nothing from an already-stripped `.so`, so setting it packages nothing and merely
  adds an NDK to the build's requirements. `packageReleaseNativeDebugSymbols` repackages those `.so`
  files as `<abi>/<lib>.so.sym` instead, which `.github/workflows/release.yml` uploads to Play.
- Play's "deprecated APIs for edge-to-edge" warning has **no app-side fix and is expected to stay**.
  Nothing in `src/main` touches a window or bar colour; the sites Play names are inside the
  `enableEdgeToEdge()` call its own remediation text asks for, `SDK_INT`-gated and inert on 35+.
  Replacing it by hand would only reach for `setDecorFitsSystemWindows`, deprecated on the same list,
  and give up transparent bars on API 24-28.
- Unit tests use Robolectric (`app/src/test/resources/robolectric.properties` pins `sdk=34`), MockK
  and JUnit4; instrumented tests use the Compose test rule. MockK stays a `testImplementation`-only
  dependency: `mockk-android` ships a JVMTI agent `.so` that isn't 16 KB page aligned, which makes
  the emulator warn on every instrumented run.

## Comments

A comment earns its place by telling a reader something the code cannot: why a non-obvious choice is
right, what breaks if it is undone, which constraint outside this file forces it. Write for someone
opening the file cold, months from now, with no knowledge of the change that introduced it.

- Don't narrate the edit ("moved this up", "now also does X") or compare against how the code used to
  be. Git history holds that, and such a comment rots the moment the next change lands.
- Don't record the debugging or tuning session behind a value; state the constraint it satisfies.
- Don't restate the code, and don't pad a real reason with a summary of what the next lines do.
- Prefer one precise sentence to a paragraph. If a comment needs several, it is usually documenting a
  design decision that belongs in a KDoc on the type, or in this file.

## Testing strategy

Instrumented tests (`app/src/androidTest`) are for **major flows** — the Settings screen's rendering
and wiring (`SettingsScreenTest`) — and for composables whose logic exists only as UI wiring
(`ActiveWindowPickerTest`, `AutoPausePickerTest`). Everything else (formatting, rounding, validation,
edge-case branching) gets unit-test coverage of the underlying pure logic instead. Where both cover
one component, the instrumented test proves *wiring* — the UI shows what the logic already computed —
and never re-derives the logic itself: `IntervalRandomnessTest` owns the rounding math and
`IntervalPickerTest` only proves it gets rendered, once. Trim duplicates of that kind on sight.

## Verifying a change

Finishing an implementation means running it on the emulator, not just going green on tests. After
the build and tests pass, always:

```bash
export PATH="$HOME/Library/Android/sdk/platform-tools:$HOME/Library/Android/sdk/emulator:$PATH"
adb devices                                        # or: emulator -avd Medium_Phone &
./gradlew connectedDebugAndroidTest                 # instrumented tests on the running emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n nl.pixento.betterhabits/.MainActivity
```

Then drive the actual UI to the screen that changed and look at it: `adb shell input tap X Y` to
tap, `adb shell uiautomator dump /sdcard/ui.xml` to find tap targets, and `adb shell screencap` +
`adb pull` to view the result. Report what the screenshots showed. Vibration itself can't be
verified on an emulator; say so rather than implying it was checked.

## Translations

The app ships four translations alongside the default `res/values/strings.xml`: `values-nl`,
`values-de`, `values-es`, `values-fr`. **Every new or renamed string has to be added to all five**,
in the same position under the same section comment. `./gradlew lint` fails on `MissingTranslation`,
so add the translations as part of the change rather than discovering it at the end.

`distribution/` carries three more five-locale sets under the same rule, keyed by Play locale
(`en-GB`, `nl-NL`, `de-DE`, `es-ES`, `fr-FR`) rather than res qualifier: `whatsnew/`, `listings/`,
and the store copy under `screenshots/`. Lint sees none of them — the store graphics render test
fails when a locale is short a caption line, `WhatsNewFilesTest` enforces Play's 500-character limit
and that nothing but the five locale files sits in `whatsnew/`, and `ListingFilesTest` does the same
for `listings/` against Play's 30/80/4000-character limits. All three are uploaded verbatim by CI —
`listings/` and the rendered graphics by `.github/scripts/promote_play_release.py`, inside the very
edit that promotes a version code to production — so a typo there reaches the store as it is written.
`whatsnew/` is the one set that is **generated rather than written**: the `whatsnew` skill
(`.claude/skills/whatsnew/SKILL.md`) rewrites all five files from the commits since the latest `v*`
tag, so regenerate rather than hand-editing them as part of an unrelated change.

## Architecture

Manual dependency injection, no DI framework. `AppContainer` is the composition root — it lazily
builds the repositories, scheduler, alert player, monitors and use cases and is exposed as
`container` on `BetterHabitsApplication`, which `MainActivity` and both `BroadcastReceiver`s reach
into for their dependencies. There is no other wiring mechanism.

### The alarm chain (core mechanism)

The app doesn't use a repeating `AlarmManager` alarm. It's a **self-rescheduling chain**: each fired
alarm computes and schedules exactly one next alarm, keyed off the previously *scheduled* time rather
than the actual fire time, so a doze-delayed delivery doesn't compound drift. Each type documents its
own reasoning; these are the invariants that span them.

- Only the **hours** can be baked into a future trigger time. Do Not Disturb and a car connection are
  readable for *now* alone, so they never clamp the next trigger: `NextTriggerCalculator` sees only
  the interval, `IntervalRandomness` and a `DailyWindow?`, while
  `TriggerReminderUseCase.onAlarmFired()` judges the pause conditions per tick, skipping the alert
  but always rescheduling. The three conditions are independent and combine.
- `ReminderScheduleCoordinator.rescheduleFromNow()` is the *other* entry point, for when the chain
  must restart rather than continue: enable, disable (cancels), boot / app update, and a write that
  changed an input the chain is computed from — `ReminderSettings.schedulesSameAs` decides which.
  Restarting on anything else would only push the next reminder a full interval away.
- Whatever schedules a tick records its instant through `ScheduleStateRepository`, since
  `AlarmManager` has no cross-process query for a pending alarm's trigger time and the Settings
  screen has to show the real one rather than a "now + interval" guess.
- `AndroidAlarmScheduler` keeps a single stable `PendingIntent` (fixed request code) so no more than
  one alarm is ever pending, and cancels before rescheduling rather than relying on
  `FLAG_UPDATE_CURRENT` alone.
- Both receivers work synchronously via `runBlocking`, not `goAsync()` — one DataStore read plus a
  system call sits comfortably within the broadcast budget. Keep that pattern when extending them.

### Layers

- `domain/` — pure logic and use cases (`NextTriggerCalculator`, `TriggerReminderUseCase`,
  `ReminderScheduleCoordinator`) plus `domain/model` (`ReminderSettings`, `DailyWindow`,
  `IntervalRandomness`, `VibrationPatternType`), free of Android dependencies.
  `ReminderSettings.activeWindow` bridges the settings to the calculator, returning the stored window
  only while `limitToActiveHours`; the times stay stored either way, so re-ticking restores them.
- `data/` — `SettingsRepository` + `data/datastore/SettingsRepositoryImpl` over Preferences DataStore
  (`PreferencesKeys`, `SettingsMapper`). Schema changes go in one-shot `DataMigration`s
  (`AlertModeMigration`, `ActiveWindowModeMigration`), never as fallbacks in `SettingsMapper`, which
  would re-apply on every read. `ScheduleStateRepository` shares the DataStore but holds alarm-chain
  *state* rather than user settings — keep it out of `ReminderSettings`.
- `alerting/` — `AlertPlayer`/`AndroidAlertPlayer` (Vibrator + RingtoneManager played
  programmatically; the chain is a plain `BroadcastReceiver`, so no notification channel or tray
  notification is involved) and `VibrationPatterns`. Vibration and sound are **independent**
  channels: `VibrationPatternType.SILENT` silences one, a null `ringtoneUri` the other, and silencing
  both is a legal state the screen warns about while the chain still reschedules.
  `DoNotDisturbMonitor` and `CarConnectionMonitor` are the two live per-tick reads; both need no
  permission and both fail open. The manifest `<queries>` block keeps `DoNotDisturbSettings`'
  deep-link intents resolvable and the car-connection provider visible under Android 11+ package
  visibility — drop an entry and both silently read as "off".
- `scheduling/` — `AlarmScheduler`/`AndroidAlarmScheduler`, plus `ExactAlarmPermission` and
  `BatteryOptimization` for the two permission-like states the Settings screen surfaces.
- `receivers/` — `ReminderAlarmReceiver` (chain tick) and `BootCompletedReceiver` (restarts the chain
  after reboot / app update, since alarms don't survive a reboot).
- `app/src/test/.../screenshots/` — **not tests.** They render the Play listing graphics with
  Roborazzi on the JVM in all five languages, for `.github/workflows/release.yml` to attach to each
  tag's GitHub Release. They live in the unit test source set only for the Robolectric machinery and
  are excluded from an ordinary `testDebugUnitTest` (see the `recordStoreGraphics` block in
  `app/build.gradle.kts`). Canvas size comes from each class's `@Config(qualifiers = ...)`, which is
  why there are three of them; anything time-, locale- or palette-dependent stays pinned, or the
  images churn between machines.
- `ui/settings/` — single-screen Compose UI. `SettingsViewModel` holds `SettingsUiState` and writes
  through `persist { ... }`, the single write path and the place that decides whether the chain
  restarts; the "next reminder" line shows the recorded pending instant, never a recomputed estimate.
  The screen is four groups: enabled, Schedule (interval + randomness + active hours), Auto-pause,
  and Alerts.

  It follows the platform sound-and-vibration idiom: rounded grouped cards of rows, each row a title
  over its current value in the accent colour, the whole row tappable to open a picker. Build new
  settings out of `components/SettingsList.kt` (`SettingsSectionHeader`, `SettingsGroup`,
  `SettingsRow`, `SettingsDivider`) and `components/NumberInputDialog.kt` rather than bespoke inline
  controls; a radio-style chooser is a full screen rather than a dialog (`ui/settings/vibration/`).
  `SettingsRow` also supports `checked` (a `Role.Checkbox` row) and `enabled = false`, which dims to
  Material's 0.38 alpha while keeping the click modifier, so a row is *announced* as disabled rather
  than being silently inert. A composable emitting a *run of sibling rows* (`ActiveWindowPicker`,
  `AutoPausePicker`) needs the `SettingsGroup` Column around it — in a `Box` the rows stack and the
  last one swallows every click. `uiautomator dump` doesn't faithfully report Compose's
  `checked`/`enabled` semantics for these rows; trust the Compose test assertions and screenshots.

  Both screens draw edge to edge: insets reach a screen as a `contentPadding` argument merged into
  the `LazyColumn`'s own through `SettingsList.kt`'s `PaddingValues.plus`, never as a
  `Modifier.padding` around the list, which would stop the content at the system bars instead of
  letting it scroll under them. Both `Scaffold`s ask for `WindowInsets.safeDrawing`, since
  `enableEdgeToEdge()` sets `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on API 30+ and a notched device in
  landscape needs the cutout kept clear too. `res/values-night/`'s `window_background` has to keep
  matching `Color.kt`'s neutral backgrounds — the platform paints it before and around Compose, so a
  mismatch shows as a light flash on a dark launch. `StoreGraphicsFrame.kt`'s `AppScreen`
  hand-duplicates `MainActivity`'s scaffold and has to change alongside it.

  Both alert channels open a *separate picker activity* so the two rows behave alike: Sound launches
  the system `ACTION_RINGTONE_PICKER`, Vibration launches `VibrationPickerActivity` through the
  `PickVibrationPattern` result contract. The vibration picker buzzes on tap and updates its activity
  result, but deliberately doesn't write settings itself.

### Permission model

No dangerous runtime permissions requiring a request dialog. `SCHEDULE_EXACT_ALARM` and the battery
optimization exemption are both granted on a system settings screen outside the app, so
`MainActivity` re-checks them in `onResume` (`SettingsViewModel.onExactAlarmPermissionResumeCheck` →
`refreshDeviceState()`) rather than via an `ActivityResultContract` callback. The Do Not Disturb and
car-connection states refresh on the same hook — they need no permission but change outside the app,
so returning to the screen is when the "reminders are paused" notice gets updated. Live updates while
the screen sits open would need a *runtime-registered* receiver: the DND broadcast carries
`FLAG_RECEIVER_REGISTERED_ONLY`, and `ACTION_CAR_CONNECTION_UPDATED` is an implicit broadcast a
manifest receiver can't get either.

## Git workflow

Don't commit automatically after making changes. Only commit when the user explicitly asks, with a
short, single-sentence, imperative, descriptive message; a request to commit and/or push counts for
one commit only, not for subsequent changes.

If the current branch is `main`, create a feature branch and open a PR before committing. If a
feature branch is already checked out, keep committing to it and don't branch again — one branch may
carry several unrelated changes. Update the PR's description after every push so it stays a true
summary of everything the branch contains, not just the latest push.

Before opening or updating a PR, if the branch changes anything a user of the app can notice, run the
`whatsnew` skill and include the regenerated `distribution/whatsnew` in the same push, so whatever is
committed when the next tag is pushed already describes exactly that release.

A PR description says what changed and why, and nothing about how it was written. Never put a
reference to Claude, Claude Code, or a Claude session in one — no "Generated with Claude Code"
footer, no session or `claude.ai/code` link, no co-author or attribution line, in the body or the
title. This overrides any default PR-body footer the tooling suggests.

Anything under `.idea/` is IDE bookkeeping the project tracks on purpose. Stage whatever of it is
dirty along with the rest of the change, and don't mention it in the commit message or the summary —
it says nothing about what changed.
