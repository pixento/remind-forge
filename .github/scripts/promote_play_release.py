#!/usr/bin/env python3
"""Promote a release already on the Play internal track to production.

Promotion is a track reassignment, not an upload: re-uploading a bundle whose version code Play
already holds fails with apkUpgradeVersionConflict, so the upload action can't do this. Driven by
.github/workflows/play-promote.yml.
"""

import json
import os
import sys

import google.auth.transport.requests
from google.oauth2 import service_account
import requests

API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
SOURCE_TRACK = "internal"
TARGET_TRACK = "production"


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


def main():
    package_name = require_env("PACKAGE_NAME")
    rollout = require_env("ROLLOUT_PERCENTAGE")
    wanted_version_code = os.environ.get("VERSION_CODE", "").strip()

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
    check(session.post(f"{edits}/{edit_id}:commit"), "committing the edit")

    print(
        f"Promoted version code {version_code} from {SOURCE_TRACK} to {TARGET_TRACK} "
        f"at {percentage:g}% rollout."
    )


if __name__ == "__main__":
    main()
