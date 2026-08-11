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
- Unit tests use Robolectric (`app/src/test/resources/robolectric.properties` pins `sdk=34`), MockK,
  Turbine (for Flow testing), and JUnit4.

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
  daily window, computes the next trigger instant, clamping into the window (including overnight
  windows where `windowEnd < windowStart`, and `windowStart == windowEnd` meaning "always active").
  It also takes `now` (defaulting to the reference, which is right for the compute-fresh case) and
  skips whole intervals that already elapsed: doze routinely delivers an exact alarm minutes late,
  and without that the next slot would land in the past, fire immediately, and make the chain replay
  every missed tick back to back. Skipping whole intervals rather than restarting from `now` keeps
  the chain on its original cadence.
- `TriggerReminderUseCase.onAlarmFired(scheduledAtMillis)` — called by `ReminderAlarmReceiver` on
  every tick. Re-reads current settings (may have changed since this alarm was scheduled), fires the
  alert only if still enabled and within window, then always computes+schedules the next tick.
- `ReminderScheduleCoordinator.rescheduleFromNow()` — the *other* entry point into the chain, used
  whenever the chain needs to restart fresh from "now" rather than continue: on enable, on any
  settings change while enabled, on disable (cancels), and on boot / app update. Called from
  `SettingsViewModel.persist()` after every settings write, and from `BootCompletedReceiver`.
- `AndroidAlarmScheduler` wraps `AlarmManager.setExactAndAllowWhileIdle` behind a single stable
  `PendingIntent` (fixed request code) so there is never more than one alarm pending; it explicitly
  cancels before rescheduling rather than relying on `FLAG_UPDATE_CURRENT` alone.
- `ReminderAlarmReceiver` and `BootCompletedReceiver` run their work synchronously via `runBlocking`
  (not `goAsync()`), since the work per tick (one DataStore read + a Vibrator/NotificationManager/
  AlarmManager call) is fast and comfortably within the broadcast time budget. Keep this pattern if
  extending them — don't reach for coroutine-async receiver patterns without a reason.

### Layers

- `domain/` — pure logic and use cases (`NextTriggerCalculator`, `TriggerReminderUseCase`,
  `ReminderScheduleCoordinator`) plus `domain/model` (`ReminderSettings`,
  `VibrationPatternType`). No Android framework dependencies except where noted.
- `data/` — `SettingsRepository` interface + `data/datastore/SettingsRepositoryImpl`, backed by
  Preferences DataStore (`data/datastore/PreferencesKeys`, `SettingsMapper` convert between
  `Preferences` and `ReminderSettings`). `AlertModeMigration` is a one-shot `DataMigration` that
  translates the removed `alert_mode` preference into the two independent channels below; keep
  schema changes to one-shot migrations rather than fallbacks in `SettingsMapper`, which would
  re-apply on every read.
- `alerting/` — `AlertPlayer`/`AndroidAlertPlayer` (Vibrator + RingtoneManager, played
  programmatically; no notification channel or tray notification is involved — the alarm chain is a
  plain `BroadcastReceiver` and needs neither), `VibrationPatterns`. Vibration and sound are two
  **independent** channels — a tick can buzz, ring, or both. `VibrationPatternType.SILENT` (whose
  `waveformFor` is `null`) silences the first, a null `ringtoneUri` the second; silencing both is a
  legal state that the Settings screen warns about and `TriggerReminderUseCase` reports as
  `AlarmFiredOutcome.NO_ALERT_SELECTED` while still rescheduling.
- `scheduling/` — `AlarmScheduler`/`AndroidAlarmScheduler`, plus `ExactAlarmPermission` and
  `BatteryOptimization` helpers for the two runtime-permission-like states the Settings screen has to
  surface (SCHEDULE_EXACT_ALARM grant, battery optimization exemption) since both are granted via a
  system settings screen rather than a normal permission dialog.
- `receivers/` — `ReminderAlarmReceiver` (alarm chain tick) and `BootCompletedReceiver` (restarts the
  chain after reboot/app update, since `AlarmManager` alarms don't survive reboot).
- `ui/settings/` — single-screen Compose UI. `SettingsViewModel` holds `SettingsUiState`, collects
  `SettingsRepository.settings` and writes through `persist { ... }`, which always follows a
  settings write with `scheduleCoordinator.rescheduleFromNow()` so a change takes effect immediately
  instead of waiting for the in-flight chain to finish its current interval. `ui/settings/components/`
  holds the individual controls (interval picker, time window picker, vibration pattern picker,
  ringtone picker launcher, permission banners).

  The screen follows the platform sound-and-vibration settings idiom: rounded grouped cards of rows,
  each row a title over its current value in the accent colour, the whole row tappable to open a
  picker. Build new settings out of `components/SettingsList.kt` (`SettingsGroup`, `SettingsRow`,
  `SettingsDivider`) and `components/RadioChoiceDialog.kt` rather than adding bespoke inline
  controls — `RadioChoiceDialog` keeps the choice as a draft until OK so `onPreview` can audition an
  option that Cancel then discards, mirroring the system ringtone picker.

### Permission model

No dangerous runtime permissions requiring a request dialog. `SCHEDULE_EXACT_ALARM` and battery
optimization exemption are both granted through a system settings screen outside the app, so
`MainActivity` re-checks their state in `onResume` (`SettingsViewModel.onExactAlarmPermissionResumeCheck`)
rather than via an `ActivityResultContract` callback.

## Git workflow

Don't commit automatically after making changes. Only commit when the user explicitly asks for it,
and when they do, use a short, single-sentence, imperative style, descriptive commit message.