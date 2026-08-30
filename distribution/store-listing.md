# Play Store listing copy

The copy itself lives in [`listings/`](listings), one directory per Play locale, three plain files
each — `title.txt`, `short_description.txt`, `full_description.txt`. That is the layout `fastlane
supply` uses and the layout `.github/scripts/promote_play_release.py` uploads from, so the text that
reaches Play is the text in those files, verbatim. This file holds only the reasoning behind them.

Play's limits are **title 30**, **short description 80**, **full description 4000** characters,
enforced per locale; `ListingFilesTest` fails the unit test run before a promotion can hit them.

The title is the same English brand line in every locale — "Better Habits" is a proper name, not
something to translate. Short and full descriptions stay in each language. Note the title sits
exactly on the 30-character limit, so nothing can be appended to it.

Each language is written as copy in that language, not translated line-by-line from the English, so
the wording deliberately differs between locales.

Play preserves the line breaks in a full description, so the paragraphs and bullet lists in those
files arrive on the listing exactly as they are written.
