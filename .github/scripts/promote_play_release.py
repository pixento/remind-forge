#!/usr/bin/env python3
"""Promote a release already on the Play internal track to production, listing and all.

Promotion is a track reassignment, not an upload: re-uploading a bundle whose version code Play
already holds fails with apkUpgradeVersionConflict, so the upload action can't do this. Driven by
.github/workflows/play-promote.yml.

The same edit also carries the store listing when LISTINGS_DIR is set - the copy from
distribution/listings and, with IMAGES_ROOT, the graphics rendered by the store-graphics job. One
edit, one commit: the binary and the words describing it reach Google's review together rather than
as two submissions that can end up out of step.
"""

import hashlib
import json
import os
import sys

import google.auth.transport.requests
from google.oauth2 import service_account
import requests

API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
# Media bodies go to the /upload/ host; the metadata endpoints reject a raw image body.
UPLOAD_API = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
SOURCE_TRACK = "internal"
TARGET_TRACK = "production"

# Play's listing fields, keyed by the file each one is read from.
LISTING_FIELDS = {
    "title": "title.txt",
    "shortDescription": "short_description.txt",
    "fullDescription": "full_description.txt",
}

# Play image types, in the order they're written. phoneScreenshots is a directory of files; the
# other two are a single file named after the type - the layout the Roborazzi renderers already
# produce under metadata/android/<locale>/images.
IMAGE_TYPES = ("phoneScreenshots", "featureGraphic", "icon")


def fail(message):
    print(f"::error::{message}", file=sys.stderr)
    sys.exit(1)


def require_env(name):
    value = os.environ.get(name, "").strip()
    if not value:
        fail(f"{name} is not set")
    return value


def check(response, what):
    if not response.ok:
        fail(f"{what} failed with HTTP {response.status_code}: {response.text}")
    return response.json() if response.text else {}


def pick_release(track, wanted_version_code):
    """Return the internal-track release to promote, and the version code being promoted."""
    releases = track.get("releases", [])
    if not releases:
        fail(f"the {SOURCE_TRACK} track has no releases")

    if wanted_version_code:
        for release in releases:
            if wanted_version_code in [str(code) for code in release.get("versionCodes", [])]:
                return release, wanted_version_code
        available = [
            str(code)
            for code in sorted(
                int(code) for release in releases for code in release.get("versionCodes", [])
            )
        ]
        fail(
            f"version code {wanted_version_code} is not on the {SOURCE_TRACK} track "
            f"(found: {', '.join(available) or 'none'})"
        )

    # No version code asked for: take the highest one on the track.
    latest = max(
        (
            (int(code), release)
            for release in releases
            for code in release.get("versionCodes", [])
        ),
        key=lambda pair: pair[0],
        default=None,
    )
    if latest is None:
        fail(f"the {SOURCE_TRACK} track has releases but no version codes")
    return latest[1], str(latest[0])


def listing_locales(listings_dir):
    """The locales to publish, taken from the directory itself.

    ListingFilesTest is what keeps this set equal to the app's shipped locales; reading the
    directory means the two can't disagree about which locales exist here.
    """
    if not os.path.isdir(listings_dir):
        fail(f"LISTINGS_DIR does not exist: {listings_dir}")
    locales = sorted(
        name
        for name in os.listdir(listings_dir)
        if os.path.isdir(os.path.join(listings_dir, name))
    )
    if not locales:
        fail(f"no locale directories in {listings_dir}")
    return locales


def update_listing(session, edits, edit_id, listings_dir, locale):
    """Write one locale's title, short and full description, and report their lengths."""
    body = {"language": locale}
    for field, filename in LISTING_FIELDS.items():
        path = os.path.join(listings_dir, locale, filename)
        if not os.path.isfile(path):
            fail(f"missing listing copy: {path}")
        with open(path, encoding="utf-8") as handle:
            body[field] = handle.read().strip()
    check(
        session.put(f"{edits}/{edit_id}/listings/{locale}", json=body),
        f"writing the {locale} listing",
    )
    return ", ".join(f"{field} {len(body[field])} chars" for field in LISTING_FIELDS)


def local_images(images_root, locale, image_type):
    """The PNGs to publish for one locale and image type, in the order Play should show them."""
    base = os.path.join(images_root, locale, "images")
    if image_type == "phoneScreenshots":
        directory = os.path.join(base, image_type)
        if not os.path.isdir(directory):
            return []
        # Play orders screenshots by the order they're uploaded, which is what the renderers'
        # 01_..05_ filename prefixes are for.
        return [
            os.path.join(directory, name)
            for name in sorted(os.listdir(directory))
            if name.endswith(".png")
        ]
    path = os.path.join(base, f"{image_type}.png")
    return [path] if os.path.isfile(path) else []


def sha256_of(path):
    with open(path, "rb") as handle:
        return hashlib.sha256(handle.read()).hexdigest()


def sync_images(session, edits, edit_id, package_name, locale, image_type, files):
    """Replace one locale's images of a type, unless Play already holds exactly these bytes."""
    listed = check(
        session.get(f"{edits}/{edit_id}/images/{locale}/{image_type}"),
        f"listing the {locale} {image_type}",
    ).get("images", [])

    # Only an optimisation, to keep unchanged artwork out of Google's review queue. If Play ever
    # reports a hash of its own re-encoded copy instead of the uploaded bytes this simply never
    # matches, and every run replaces the images - correct, just noisier.
    remote = [image.get("sha256") for image in listed]
    if all(remote) and remote == [sha256_of(path) for path in files]:
        return f"{len(files)} unchanged"

    check(
        session.delete(f"{edits}/{edit_id}/images/{locale}/{image_type}"),
        f"clearing the {locale} {image_type}",
    )
    for path in files:
        with open(path, "rb") as handle:
            check(
                session.post(
                    f"{UPLOAD_API}/{package_name}/edits/{edit_id}/images/{locale}/{image_type}",
                    params={"uploadType": "media"},
                    headers={"Content-Type": "image/png"},
                    data=handle.read(),
                ),
                f"uploading {os.path.basename(path)} to the {locale} {image_type}",
            )
    return f"{len(files)} uploaded"


def publish_listing(session, edits, edit_id, package_name, listings_dir, images_root):
    """Write every locale's copy, and its graphics when a rendered tree was handed over."""
    for locale in listing_locales(listings_dir):
        print(f"{locale}: {update_listing(session, edits, edit_id, listings_dir, locale)}")
        if not images_root:
            continue
        for image_type in IMAGE_TYPES:
            files = local_images(images_root, locale, image_type)
            if not files:
                # Nothing rendered for this type: leave whatever Play already shows alone rather
                # than clearing it.
                print(f"{locale}: {image_type} not rendered, left as it is")
                continue
            outcome = sync_images(
                session, edits, edit_id, package_name, locale, image_type, files
            )
            print(f"{locale}: {image_type} {outcome}")


def main():
    package_name = require_env("PACKAGE_NAME")
    rollout = require_env("ROLLOUT_PERCENTAGE")
    wanted_version_code = os.environ.get("VERSION_CODE", "").strip()
    listings_dir = os.environ.get("LISTINGS_DIR", "").strip()
    images_root = os.environ.get("IMAGES_ROOT", "").strip()
    dry_run = os.environ.get("DRY_RUN", "").strip().lower() == "true"

    try:
        percentage = float(rollout)
    except ValueError:
        fail(f"ROLLOUT_PERCENTAGE must be a number, got {rollout!r}")
    if not 0 < percentage <= 100:
        fail(f"ROLLOUT_PERCENTAGE must be in (0, 100], got {rollout!r}")

    credentials = service_account.Credentials.from_service_account_info(
        json.loads(require_env("PLAY_SERVICE_ACCOUNT_JSON")), scopes=[SCOPE]
    )
    credentials.refresh(google.auth.transport.requests.Request())
    session = requests.Session()
    session.headers["Authorization"] = f"Bearer {credentials.token}"

    edits = f"{API}/{package_name}/edits"
    edit_id = check(session.post(edits), "creating an edit")["id"]

    track = check(
        session.get(f"{edits}/{edit_id}/tracks/{SOURCE_TRACK}"),
        f"reading the {SOURCE_TRACK} track",
    )
    source_release, version_code = pick_release(track, wanted_version_code)

    release = {
        "versionCodes": [version_code],
        # Carry the notes across - a bare track update would publish with empty release notes.
        "releaseNotes": source_release.get("releaseNotes", []),
    }
    if percentage >= 100:
        release["status"] = "completed"
    else:
        release["status"] = "inProgress"
        release["userFraction"] = percentage / 100
    if "name" in source_release:
        release["name"] = source_release["name"]

    check(
        session.put(
            f"{edits}/{edit_id}/tracks/{TARGET_TRACK}",
            json={"track": TARGET_TRACK, "releases": [release]},
        ),
        f"assigning version code {version_code} to {TARGET_TRACK}",
    )

    if listings_dir:
        publish_listing(session, edits, edit_id, package_name, listings_dir, images_root)

    if dry_run:
        # Play checks the whole edit - track, listings and images - and nothing is published.
        check(session.post(f"{edits}/{edit_id}:validate"), "validating the edit")
        print(
            f"Dry run: Play accepted an edit promoting version code {version_code} from "
            f"{SOURCE_TRACK} to {TARGET_TRACK} at {percentage:g}% rollout. Nothing was committed."
        )
        return

    check(session.post(f"{edits}/{edit_id}:commit"), "committing the edit")

    print(
        f"Promoted version code {version_code} from {SOURCE_TRACK} to {TARGET_TRACK} "
        f"at {percentage:g}% rollout."
    )


if __name__ == "__main__":
    main()
