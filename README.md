# RemindForge

An Android app that periodically nudges you with a vibration or a sound — no notification to read,
no reason to take your phone out of your pocket. Set an interval and the hours it should run, and it
buzzes you every so often until you turn it off. Like a forge: one hit at a time shapes the steel.

## Features

- **Interval reminders** — every *n* minutes, with optional randomness so the next one is harder to
  anticipate.
- **Active hours** — a daily start/end window (overnight windows included), or follow the phone's
  Do Not Disturb instead, which pauses reminders whenever DND is on.
- **Alerts** — vibration pattern and ringtone are independent; pick either, both, or neither.
- **Survives reboots** — the alarm chain restarts after a reboot or an app update.
- Localised in English, Dutch, German, Spanish and French.

## Building

Requires JDK 21 and the Android SDK; everything else comes from the Gradle wrapper.

```bash
./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew connectedDebugAndroidTest   # instrumented tests, needs a device or emulator
./gradlew build                # assemble + lint + unit tests
```

`minSdk` 24, `targetSdk`/`compileSdk` 37. Kotlin, Jetpack Compose (Material 3), DataStore
Preferences, no backend and no third-party services.

## How it works

Rather than a repeating `AlarmManager` alarm, each fired alarm computes and schedules exactly one
next alarm, keyed off the previously *scheduled* time so the chain doesn't drift. Settings are
re-read on every tick, so a change to the interval or window takes effect immediately. State lives
entirely on the device in DataStore Preferences.

Contributor-facing details — architecture, layer boundaries, testing strategy — are in
[CLAUDE.md](CLAUDE.md).

## Permissions

`VIBRATE`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM` (so reminders stay on time) and the
optional battery-optimisation exemption. None of them are dangerous runtime permissions; the last
two are granted from system settings screens the app links to.
