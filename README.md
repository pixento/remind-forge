# Better Habits

An Android app that periodically nudges you with a vibration or a sound — no notification to read,
no reason to take your phone out of your pocket. Set an interval and the hours it should run, and it
buzzes you every so often until you turn it off. One nudge at a time, until the habit sticks.

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

**Dispatch that workflow from the tag being promoted.** The same run also publishes the store
listing — the five locales' title, short and full description from `distribution/listings`, and the
screenshots, feature graphic and icon downloaded from the tag's `store-graphics-<tag>.zip` — all
inside the edit that assigns the version code, committed once, so the binary and the copy describing
it go to Google's review together. The copy comes from the checked-out ref and the graphics from the
tag, which is why the two should be the same tag; `graphics_tag` overrides the second half when they
can't be. Set `update_listing: false` to promote the binary alone, and `dry_run: true` to have Play
validate the whole edit and commit nothing — worth doing before the first listing publish, since a
locale the Console has never had is the one thing that fails at this point.

Store text lives in `distribution/`: release notes in `whatsnew/whatsnew-<locale>`, the listing copy
in `listings/<locale>/{title,short_description,full_description}.txt` (with the reasoning behind it
in [store-listing.md](distribution/store-listing.md)), and the screenshot captions and
feature-graphic tagline in `screenshots/captions/<locale>` and `screenshots/feature-graphic/<locale>`
— all covering the same five shipped languages. Keep them in step — the upload fails on a whatsnew
locale the Play listing doesn't have. Play preserves the newlines in a whatsnew file and in a full
description, so write each paragraph as one long line rather than wrapping it. `ListingFilesTest`
holds the listing copy to Play's 30/80/4000-character limits and to the five locales.

The release notes are **generated, not written by hand**. All five are rewritten from the commits
since the latest `v*` tag as part of preparing a pull request, so by the time a tag is pushed they
already describe exactly the changes that release contains, and the set resets itself for the next
cycle rather than accumulating. `WhatsNewFilesTest` fails the ordinary unit test run if a locale is
missing, a file is empty, one exceeds Play's 500-character limit, or something that isn't a locale
file has appeared in the directory — none of which lint or the Play upload would tell you about
before tag time. Editing a file by hand is still fine; the next regeneration simply rewrites it.

Release builds are **unsigned locally by design**: the signing config only materialises when
`RELEASE_KEYSTORE_PATH` points at a keystore, so nobody needs the upload key on their machine.

CI expects these in the `google-play` GitHub Environment:

| Secret | |
|---|---|
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Developer API service-account key — needs the **store listing** permission as well as release access, or the promotion's listing write is rejected |
| `RELEASE_KEYSTORE_BASE64` | base64 of the upload keystore |
| `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | keystore credentials |

plus the environment **variable** (not secret — `secrets` can't be read from a step `if:`)
`PLAY_PUBLISH_ENABLED`, which gates the upload step: anything other than `true` builds the bundle and
publishes it as the `app-release-aab` run artifact without touching Play. That is also the escape
hatch if Play ever has to be fed a build by hand.

Regardless of that gate, the workflow also attaches the signed bundle to the tag's GitHub Release as
`better-habits-<tag>.aab`, creating the release first if the tag doesn't have one yet.

### Native debug symbols

Play warns on every upload that the bundle carries native code without debug symbols. That native
code is entirely prebuilt AndroidX — `libandroidx.graphics.path.so` (via Compose) and
`libdatastore_shared_counter.so` — and AndroidX publishes both already stripped, so AGP's
`ndk.debugSymbolLevel` extracts nothing from them and the warning stays whatever it is set to. The
`packageReleaseNativeDebugSymbols` task repackages the very `.so` files the bundle ships as
`<abi>/<lib>.so.sym` instead, which is the file Play asks for; their exported symbol table is all the
symbol information that exists for those libraries.

The release workflow builds it alongside the bundle, hands it to the Play upload step, and attaches
it to the GitHub Release as `native-debug-symbols-<tag>.zip`. When Play is being fed by hand, upload
it in the Console under **App bundle explorer → Downloads → Upload native debug symbols** for that
version code. To build it locally:

```bash
./gradlew :app:packageReleaseNativeDebugSymbols
```

### Listing graphics

All three Play graphics are drawn from the app rather than captured by hand, so they can't drift from
what shipped: five phone screenshots (1080×1920) per language, the feature graphic (1024×500) per
language, and the 512×512 icon. The renderers live in `app/src/test/.../screenshots/` and use
Roborazzi to draw the real composables — and, for the icon, the same vector drawables the launcher
icon is built from — on the JVM under Robolectric. No emulator is involved. To see them:

```bash
./gradlew :app:testDebugUnitTest -PrecordStoreGraphics=true
open app/build/outputs/store-graphics/metadata/android/en-GB/images
```

Without that flag an ordinary test run skips the renderers entirely and writes nothing. The output is
laid out the way `fastlane supply` and the Play Console expect, and the release workflow attaches the
whole tree to the tag's GitHub Release as `store-graphics-<tag>.zip`. The Play upload step can only
carry the bundle and the release notes — it has no input for images — so putting the graphics on the
listing is still a Console action.

Captions are one line per screenshot, in order, and the render fails if a locale is short a line.

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

Better Habits collects nothing and sends nothing; every setting stays in the app's private storage on
the device. The full policy, in all five shipped languages, is in [PRIVACY.md](PRIVACY.md).
