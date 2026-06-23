# Issue 860 - deterministic gitleaks install

## Context

The `Secret Scan (gitleaks)` job in `CI` failed before scanning because the
installer queried `https://api.github.com/repos/gitleaks/gitleaks/releases/latest`
without authentication. GitHub returned HTTP 403, so the `Run gitleaks` step was
skipped.

The same latest-release lookup was present in the weekly `Security` workflow.
Earlier fixes had already moved from hand-built latest URLs to release metadata,
but that still left the CI path dependent on a rate-limited API call.

## Decision

Pin `GITLEAKS_VERSION` to `v8.30.1` and download the exact Linux x64 release
asset from the tag URL. This removes the unauthenticated releases API lookup
from the installer while preserving the existing `gitleaks detect` commands.

## Follow-up Guard

Keep the `CI` and `Security` gitleaks installers aligned. If the pinned scanner
version changes, update both workflow env blocks and verify the asset URL before
merging.
