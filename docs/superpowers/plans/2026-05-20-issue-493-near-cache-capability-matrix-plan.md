# Issue #493 Near-Cache Capability Matrix Plan

## Step 1 - Matrix Documentation

- Add `docs/cache/near-cache-capability-matrix.md`.
- Cover native NearCache, JCache NearCache, sync near-cache, suspend
  near-cache, event listener registration, propagation, `removeAll`, and
  distributed/local back-cache behavior.
- Mark Caffeine and Cache2k local-only/provider-only combinations explicitly.
- Link the matrix from `cache-core`, `cache-lettuce`, `cache-hazelcast`, and
  `cache-redisson` README pairs.

## Step 2 - Conformance Fixture Strengthening

- Add operation-level tests that verify `put`, `replace`, `remove`, and
  `removeAll` effects survive `clearLocal()` and therefore reached the back
  tier.
- Keep the tests in `cache-core` test fixtures so supported backends inherit the
  same suite.

## Step 3 - Unsupported Behavior Tests

- Replace disabled Hazelcast JCache tests with active tests.
- Assert direct listener-backed `NearJCache.invoke` /
  `SuspendNearJCache.invoke` is unsupported for Hazelcast JCache.
- Assert Hazelcast factory methods still create listener-free near caches for
  read-through/write-through use.
- Replace the Cache2k silent `removeAll()` skip with an explicit unsupported
  assertion or a passing conformance implementation if behavior has changed.
- Keep unsupported combinations active in tests or documented as local-only
  exclusions in the matrix.

## Step 4 - Verification

- Run IDE reformat/imports and diagnostics for touched Kotlin files.
- Run targeted Gradle tests for:
  - `:bluetape4k-cache-core`
  - `:bluetape4k-cache-hazelcast`
  - `:bluetape4k-cache-lettuce` near-cache conformance classes that inherit the
    changed fixtures
  - `:bluetape4k-cache-redisson` near-cache conformance classes that inherit the
    changed fixtures
- Run `git diff --check`.

## Step 5 - Review, Lesson, PR

- Current-session review only; no Claude/Codex CLI review.
- Resolve P0/P1 findings before PR.
- Add a concise lesson under `docs/lessons/`.
- Commit with Lore trailers, push, create PR assigned to `debop`.
