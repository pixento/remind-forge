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

## Releasing

Releases go to Google Play from CI. Pushing a `v*` tag runs the full test gate
(`.github/workflows/ci.yml`, reused via `workflow_call`), builds a signed App Bundle, and uploads it
to the **internal testing** track.

```bash
git tag v1.2.0 && git push origin v1.2.0
```

`versionCode` is the workflow run number and `versionName` is the tag without its `v`; both reach the
build through `VERSION_CODE` / `VERSION_NAME` environment variables.

Production is a separate, deliberate step: run the **Promote to production** workflow, which
reassigns a version code that is already on the internal track rather than rebuilding, so what ships
is bit-for-bit what testers installed. It takes a `version_code` (blank picks the highest one on the
internal track) and a `rollout_percentage` for a staged rollout.

Store text lives in `distribution/`: release notes in `whatsnew/whatsnew-<locale>`, and the listing
copy for all five languages in [store-listing.md](distribution/store-listing.md). Only add a whatsnew
locale that exists in the Play listing, or the upload fails.

Release builds are **unsigned locally by design**: the signing config only materialises when
`RELEASE_KEYSTORE_PATH` points at a keystore, so nobody needs the upload key on their machine.

CI expects these in the `google-play` GitHub Environment:

| Secret | |
|---|---|
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Developer API service-account key |
| `RELEASE_KEYSTORE_BASE64` | base64 of the upload keystore |
| `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | keystore credentials |

plus the repository variable `PLAY_PUBLISH_ENABLED`, which gates the upload step: anything other than
`true` builds the bundle and publishes it as the `app-release-aab` run artifact without touching
Play. That is also the escape hatch if Play ever has to be fed a build by hand.

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
two are granted from system settings screens the app links to. Notably absent: `INTERNET` — the app
cannot make network requests at all.

## Privacy

RemindForge collects nothing and sends nothing; every setting stays in the app's private storage on
the device. The full policy, in all five shipped languages, is in [PRIVACY.md](PRIVACY.md).
