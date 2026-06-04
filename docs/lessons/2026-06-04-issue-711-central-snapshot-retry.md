# Issue #711 Central snapshot retry

## Context
Downstream CI and Nightly runs can fail when GitHub runners receive transient
HTTP 403 responses from Central Portal snapshot metadata.

## Decision
Wrap the top-level Gradle build and Nightly detekt gates in bounded three-attempt
retry loops without changing the Gradle command semantics.

## Verification
- `git diff --check`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`

## Next Time
If a bluetape4k SNAPSHOT dependency fails with Central metadata 403, check
upstream publish status first, then prefer a bounded workflow retry over
dependency or catalog churn.
