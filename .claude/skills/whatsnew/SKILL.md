---
name: whatsnew
description: Regenerate the Play release notes in distribution/whatsnew for all five shipped locales from the commits since the latest v* tag. Use before opening or updating a pull request in this repo whenever the branch changes anything a user can notice, and whenever the user asks to write, update or refresh the release notes / whatsnew / Play "What's new" text.
---

# Regenerate the Play release notes

`distribution/whatsnew/whatsnew-<play-locale>` is what Play shows testers as "What's new". The
release workflow uploads the checked-in directory verbatim
(`whatsNewDirectory: distribution/whatsnew`), so whatever is committed when a `v*` tag is pushed is
what ships. These files are **generated, not hand-written** — this skill is how.

## Rewrite the whole set, every time

Regenerate all five files from scratch on every run. Never append to what is already there.

That is what keeps the notes relative to the latest tag and makes them self-resetting: once a tag is
pushed, the next branch's commit range starts empty, and the next run rewrites the files for the new
release cycle rather than carrying the shipped release's text forward. It also means running this
twice on the same branch converges instead of accumulating.

## 1. Find the range

```bash
base=$(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || git rev-list --max-parents=0 HEAD)
git fetch origin main --quiet
git log --no-merges --format='%h %s' "$base"..HEAD "$base"..origin/main
```

Union the branch with current `origin/main` deliberately: a branch cut before another PR merged
would otherwise regenerate the files without that PR's change and silently drop it from the release.

Then look at what actually changed, not just the subjects:

```bash
git diff --stat "$base"...HEAD
```

Commit messages in this repo are developer-facing ("Attach a native debug symbols zip to each
release", "Draw the settings screens genuinely edge to edge"). Read the diff of anything that touches
`app/src/main` and describe what a user would notice, in their words, not the commit's.

## 2. Decide what belongs in the notes

Keep only what a user of the app can perceive: new or changed settings, changed reminder behaviour,
visual changes, bug fixes, new translations.

Drop everything else — CI and workflow changes, build configuration, dependency bumps, tests, docs,
store copy, listing graphics, release plumbing. A change to `.github/`, `app/build.gradle.kts`,
`app/src/test`, `app/src/androidTest`, `distribution/` or a Markdown file is almost never something
to tell a user about.

If nothing in the range is user-visible, don't leave the previous release's text standing and don't
stop to ask: write a short honest maintenance line — "Under-the-hood improvements and updated
components." and its four translations.

## 3. Write the five files

Write English (`whatsnew-en-GB`) first, then translate it into `nl-NL`, `de-DE`, `es-ES` and
`fr-FR`. Real translations, phrased as that language would phrase it — not word-for-word
transliterations of the English.

Reuse the app's own terminology from `app/src/main/res/values-*/strings.xml` so the notes match the
UI the reader is about to open: "Niet storen", "Nicht stören", "No molestar", "Ne pas déranger" for
Do Not Disturb, and likewise for interval, time window, vibration pattern and ringtone. Match the
voice of `distribution/store-listing.md` — second person, present tense, plain and quiet, no
marketing exclamation.

Format:

- One paragraph per line, blank line between paragraphs. Play preserves the newlines, so never wrap
  a paragraph across lines.
- Lead with the change that matters most; two or three short paragraphs is plenty.
- **Under 500 characters per file** — Play's hard limit, and an over-long file fails the upload at
  tag time. `WhatsNewFilesTest` enforces this, along with all five locales being present.
- No version numbers, no dates, no commit hashes, no issue or PR references.

Overwrite all five files. Never leave a locale behind — the Play upload fails on a whatsnew locale
the listing doesn't have, and a stale locale is worse than none.

## 4. Check and report

```bash
./gradlew :app:testDebugUnitTest --tests "*.WhatsNewFilesTest"
```

Then tell the user the base tag and the commit range you used, the English text you wrote, and which
commits in the range you deliberately left out and why.
