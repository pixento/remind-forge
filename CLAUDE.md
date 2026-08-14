# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

RemindForge (`nl.pixento.remindforge`) is a single-module Android app (Kotlin + Jetpack Compose) that
periodically alerts the user (vibration or ringtone) at a configurable interval within a configurable
daily time window — e.g. "buzz every 15 minutes between 09:00 and 17:00". There is one screen
(Settings) and no server/backend; all state lives in local DataStore Preferences.

## Common commands

Run all commands from the repo root via the Gradle wrapper.

```bash
./gradlew build                                    # full build (assemble + check)
./gradlew testDebugUnitTest                         # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "nl.pixento.remindforge.domain.NextTriggerCalculatorTest"
./gradlew testDebugUnitTest --tests "*.NextTriggerCalculatorTest.within window*"  # single test method
./gradlew connectedDebugAndroidTest                 # instrumented tests (app/src/androidTest), needs device/emulator
./gradlew assembleDebug                             # build a debug APK
./gradlew lint                                      # Android Lint
```

Notes on the build:
- `compileSdk`/`targetSdk` = 37, `minSdk` = 24. Kotlin 2.2.10, AGP 9.3.1, Compose BOM 2026.02.01.
- Unit tests are forced to run on JDK 21 (`Test.javaLauncher`) and Kotlin compiles to bytecode
  target 11, independent of whatever JDK the Gradle daemon itself uses — Robolectric's bundled ASM
  can't parse class files from very new JDKs. Don't "fix" this pinning if a newer local JDK is
  present; it's intentional (see comments in `app/build.gradle.kts`).
- Unit tests use Robolectric (`app/src/test/resources/robolectric.properties` pins `sdk=34`), MockK
  and JUnit4; instrumented tests use the Compose test rule. MockK is deliberately a
  `testImplementation`-only dependency: `mockk-android` ships a JVMTI agent `.so` that isn't 16 KB
  page aligned, which makes the emulator pop up an alignment warning on every instrumented run.

## Verifying a change

Finishing an implementation means running it on the emulator, not just going green on tests. After
the build and tests pass, always:

```bash
export PATH="$HOME/Library/Android/sdk/platform-tools:$HOME/Library/Android/sdk/emulator:$PATH"
adb devices                                        # or: emulator -avd Medium_Phone &
./gradlew connectedDebugAndroidTest                 # instrumented tests on the running emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n nl.pixento.remindforge/.MainActivity
```

Then drive the actual UI to the screen that changed and look at it — `adb shell input tap X Y`,
`adb shell uiautomator dump /sdcard/ui.xml` to find tap targets and read back radio/switch state, and
`adb shell screencap -p /sdcard/s.png` + `adb pull` to view the result. Report what the screenshots
showed. Vibration itself can't be verified on an emulator; say so rather than implying it was checked.

## Translations

The app ships four translations alongside the default `res/values/strings.xml`: `values-nl`,
`values-de`, `values-es`, `values-fr`. **Every new or renamed string has to be added to all five**,
in the same position under the same section comment. `./gradlew lint` fails on `MissingTranslation`,
so a change that only touches `res/values` will not get past `build` — add the translations as part
of the change rather than discovering it at the end.

## Architecture

Manual dependency injection, no DI framework. `AppContainer` (app/src/main/java/nl/pixento/remindforge/AppContainer.kt)
is the composition root — it lazily builds the repository, scheduler, alert player, and use cases, and
is exposed as `container` on `RemindForgeApplication`. Both `MainActivity` and the two
`BroadcastReceiver`s reach into `(application as RemindForgeApplication).container` to get their
dependencies; there's no other wiring mechanism.

### The alarm chain (core mechanism)

The app doesn't use a repeating `AlarmManager` alarm. Instead it's a **self-rescheduling chain**: each
fired alarm computes and schedules exactly one next alarm, always keyed off the previously *scheduled*
time (not actual fire time) to avoid drift accumulating over a long-running chain.

- `NextTriggerCalculator` (domain, pure/framework-free) — given a reference instant, interval, and
  a `DailyWindow?`, computes the next trigger instant, clamping into the window (`DailyWindow.contains`
  owns the semantics: start inclusive, end exclusive, overnight windows where `end < start`, and
  `start == end` meaning "always active"). A **null** window means no time-of-day constraint at all,
  which is how `ActiveWindowMode.DO_NOT_DISTURB_OFF` is expressed: whether DND is on can only be read
  for *now*, never predicted for a future instant, so that mode can't clamp and is judged per tick.
  It also takes `now` (defaulting to the reference, which is right for the compute-fresh case) and
  skips whole intervals that already elapsed: doze routinely delivers an exact alarm minutes late,
  and without that the next slot would land in the past, fire immediately, and make the chain replay
  every missed tick back to back. Skipping whole intervals rather than restarting from `now` keeps
  the chain on its original cadence.
- `TriggerReminderUseCase.onAlarmFired(scheduledAtMillis)` — called by `ReminderAlarmReceiver` on
  every tick. Re-reads current settings (may have changed since this alarm was scheduled), fires the
  alert only if still enabled and currently active, then always computes+schedules the next tick.
  "Currently active" is the window check in `CUSTOM_TIMES` mode and a `DoNotDisturbMonitor` read in
  `DO_NOT_DISTURB_OFF` mode (outcomes `OUTSIDE_WINDOW` / `DO_NOT_DISTURB`); both skip the alert and
  still reschedule, so the chain keeps its cadence and resumes the moment the condition clears.
- `ReminderScheduleCoordinator.rescheduleFromNow()` — the *other* entry point into the chain, used
  whenever the chain needs to restart fresh from "now" rather than continue: on enable, on disable
  (cancels), on a change to one of the settings the chain is *computed from* (interval, active-window
  mode, window start/end), and on boot / app update. Called from `SettingsViewModel.persist()` and
  `BootCompletedReceiver`. Deliberately **not** called for the alert-channel settings (vibration
  pattern, ringtone), or when a picker re-confirms the value it already had: every tick re-reads
  settings anyway, so restarting the chain there would only push the next reminder a full interval
  away. `ReminderSettings.schedulesSameAs` is the predicate that decides this.
- Whatever schedules a tick also records its instant through `ScheduleStateRepository`, since
  `AlarmManager` has no cross-process query for a pending alarm's trigger time and the Settings
  screen would otherwise have to guess ("now + interval") — a guess that drifts away from the real
  chain and made merely opening the app *look* like it reset the countdown. `healIfNeeded()` treats
  a pending alarm with no recorded instant as a broken chain and restarts it (this happens once, on
  upgrade from a version that didn't record it).
- `AndroidAlarmScheduler` wraps `AlarmManager.setExactAndAllowWhileIdle` behind a single stable
  `PendingIntent` (fixed request code) so there is never more than one alarm pending; it explicitly
  cancels before rescheduling rather than relying on `FLAG_UPDATE_CURRENT` alone.
- `ReminderAlarmReceiver` and `BootCompletedReceiver` run their work synchronously via `runBlocking`
  (not `goAsync()`), since the work per tick (one DataStore read + a Vibrator/NotificationManager/
  AlarmManager call) is fast and comfortably within the broadcast time budget. Keep this pattern if
  extending them — don't reach for coroutine-async receiver patterns without a reason.

### Layers

- `domain/` — pure logic and use cases (`NextTriggerCalculator`, `TriggerReminderUseCase`,
  `ReminderScheduleCoordinator`) plus `domain/model` (`ReminderSettings`, `ActiveWindowMode`,
  `DailyWindow`, `VibrationPatternType`). No Android framework dependencies except where noted.
  `ReminderSettings.activeWindow` is the bridge between the mode and the calculator — it returns the
  stored `DailyWindow` in `CUSTOM_TIMES` and `null` in `DO_NOT_DISTURB_OFF`. Both window times stay
  stored in the DND mode (so switching back restores them) and stay in `schedulesSameAs`.
- `data/` — `SettingsRepository` interface + `data/datastore/SettingsRepositoryImpl`, backed by
  Preferences DataStore (`data/datastore/PreferencesKeys`, `SettingsMapper` convert between
  `Preferences` and `ReminderSettings`). `AlertModeMigration` is a one-shot `DataMigration` that
  translates the removed `alert_mode` preference into the two independent channels below; keep
  schema changes to one-shot migrations rather than fallbacks in `SettingsMapper`, which would
  re-apply on every read. `ScheduleStateRepository` (+ `ScheduleStateRepositoryImpl`) shares the same
  DataStore but holds alarm-chain *state* rather than user settings — currently just the pending
  alarm's trigger instant. Keep it out of `ReminderSettings`, which is the user's settings only.
- `alerting/` — `AlertPlayer`/`AndroidAlertPlayer` (Vibrator + RingtoneManager, played
  programmatically; no notification channel or tray notification is involved — the alarm chain is a
  plain `BroadcastReceiver` and needs neither), `VibrationPatterns`. Vibration and sound are two
  **independent** channels — a tick can buzz, ring, or both. `VibrationPatternType.SILENT` (whose
  `waveformFor` is `null`) silences the first, a null `ringtoneUri` the second; silencing both is a
  legal state that the Settings screen warns about and `TriggerReminderUseCase` reports as
  `AlarmFiredOutcome.NO_ALERT_SELECTED` while still rescheduling.

  `DoNotDisturbMonitor`/`AndroidDoNotDisturbMonitor` reads `getCurrentInterruptionFilter()`. That
  needs **no permission and no Do-Not-Disturb access grant** (only *changing* the policy does) and
  dates to API 23, so there's nothing in the manifest and no SDK_INT gate. Anything but
  `INTERRUPTION_FILTER_ALL` counts as DND, deliberately including `INTERRUPTION_FILTER_ALARMS` even
  though the OS would let our alarm-classed alerts through it; `INTERRUPTION_FILTER_UNKNOWN` fails
  open. Note the app cannot *read* the user's DND **schedule** — `getAutomaticZenRules()` returns
  only rules the caller owns and the system's rule belongs to package `"android"` — so following DND
  is necessarily a live per-tick read, not a set of times copied into `ReminderSettings`.
  `DoNotDisturbSettings.buildRequestIntent` deep-links into the system DND screen, walking a
  resolve-checked fallback chain (`android.settings.ZEN_MODE_SETTINGS`, whose `Settings` constant is
  `@hide`, then the public `ACTION_ZEN_MODE_PRIORITY_SETTINGS`, then `ACTION_SOUND_SETTINGS`). The
  manifest `<queries>` block is what keeps those resolvable under Android 11+ package visibility.
- `scheduling/` — `AlarmScheduler`/`AndroidAlarmScheduler`, plus `ExactAlarmPermission` and
  `BatteryOptimization` helpers for the two runtime-permission-like states the Settings screen has to
  surface (SCHEDULE_EXACT_ALARM grant, battery optimization exemption) since both are granted via a
  system settings screen rather than a normal permission dialog.
- `receivers/` — `ReminderAlarmReceiver` (alarm chain tick) and `BootCompletedReceiver` (restarts the
  chain after reboot/app update, since `AlarmManager` alarms don't survive reboot).
- `ui/settings/` — single-screen Compose UI. `SettingsViewModel` holds `SettingsUiState`, collects
  `SettingsRepository.settings` and writes through `persist { ... }`, which compares the settings
  before and after the write and calls `scheduleCoordinator.rescheduleFromNow()` only when the write
  actually changed the chain's inputs — so an interval/window change takes effect immediately
  instead of waiting for the in-flight chain, while an alert-channel change leaves the countdown
  running. The screen's "next reminder" line shows the recorded pending instant
  (`SettingsUiState.nextTriggerAtMillis`), not a recomputed estimate. `ui/settings/components/`
  holds the individual controls (interval picker, time window picker, ringtone picker launcher,
  permission banners), and `ui/settings/vibration/` the vibration pattern picker.

  The screen follows the platform sound-and-vibration settings idiom: rounded grouped cards of rows,
  each row a title over its current value in the accent colour, the whole row tappable to open a
  picker. Build new settings out of `components/SettingsList.kt` (`SettingsGroup`, `SettingsRow`,
  `SettingsDivider`) and `components/NumberInputDialog.kt`, rather than adding bespoke inline
  controls. `NumberInputDialog` keeps the typed value as a draft until OK so nothing outside its
  `range` can ever be committed (OK is disabled while the field is empty or out of range, which is
  why the interval row can offer free numeric entry without the caller clamping a surprise value).
  A radio-style chooser is a full screen rather than a dialog here — see `ui/settings/vibration/`.
  `SettingsRow` also supports `checked` (a checkbox row, `Role.Checkbox`) and `enabled = false`,
  which dims a row to Material's 0.38 alpha while keeping its click modifier, so the row is
  *announced* as disabled rather than being silently inert.

  The Schedule group is one card: interval, then `components/ActiveWindowPicker.kt` — the "Disable
  when Do Not Disturb is on" checkbox, the two time rows, and the system-DND shortcut as the last
  row. Ticking the checkbox **disables** the time rows rather than hiding them (they keep their
  stored values, so unticking restores them, and the group doesn't jump around); the shortcut row
  stays live either way, since it's also how you check what your DND schedule is before deciding.
  `ActiveWindowPicker` emits a *run of sibling rows*, so it needs the `SettingsGroup` Column around
  it — put it in a `Box` and the rows stack, leaving the last one to silently swallow every click.
  Note also that `uiautomator dump` does not faithfully report Compose's `checked`/`enabled`
  semantics for these rows; trust the Compose test assertions and screenshots, not the XML dump.

  Both alert channels open a *separate picker activity*, so the two rows behave alike: Sound launches
  the system `ACTION_RINGTONE_PICKER`, Vibration launches `VibrationPickerActivity` through the
  `PickVibrationPattern` result contract. The vibration picker applies on tap (it buzzes the pattern
  and updates its activity result) but deliberately doesn't write settings itself — the result is
  delivered when the screen finishes, and `SettingsViewModel.persist()` stays the single write path.

### Permission model

No dangerous runtime permissions requiring a request dialog. `SCHEDULE_EXACT_ALARM` and battery
optimization exemption are both granted through a system settings screen outside the app, so
`MainActivity` re-checks their state in `onResume` (`SettingsViewModel.onExactAlarmPermissionResumeCheck`
→ `refreshDeviceState()`) rather than via an `ActivityResultContract` callback. The current Do Not
Disturb state is re-read on the same hook — it needs no permission, but it changes outside the app
(system settings, quick-settings tile, a schedule starting), so returning to the screen is when the
"reminders are paused" notice gets refreshed. Live updates while the screen sits open would need a
*runtime-registered* receiver for `ACTION_INTERRUPTION_FILTER_CHANGED`; a manifest `<receiver>`
would never fire, since that broadcast carries `FLAG_RECEIVER_REGISTERED_ONLY`.

## Git workflow

Don't commit automatically after making changes. Only commit when the user explicitly asks for it,
and when they do, use a short, single-sentence, imperative style, descriptive commit message.

Commit to whatever branch is currently checked out. Don't create a branch first — not even when the
current branch is the default one, which would normally call for it. Only branch when the user
explicitly asks for a branch.

Anything under `.idea/` is IDE bookkeeping that the project tracks on purpose. Stage whatever of it
happens to be dirty along with the rest of the change, and don't mention it in the commit message or
flag it in the summary — it says nothing about what changed.