# Issue 435 Resilience4j Coroutine Facade

## Context

Issue #435 hardened `bluetape4k-resilience4j` coroutine helpers after the Bucket4j/Resilience4j module boundary split. The risky areas were coroutine cancellation, suspend retry semantics, and cache facade behavior.

## Decision

- Keep Resilience4j `Cache<K, V>` suspend extensions as a compatibility path because Resilience4j 2.4.0 exposes no public backing JCache accessor.
- Route strict JCache semantics through `SuspendCache.of(jcache)`.
- Treat `CancellationException` as a coroutine control signal, not a resilience failure: retry must not retry it, fallback must not recover it, and cache wrappers must not publish it as cache errors.

## Outcome

- `RetryCoroutines` now rethrows cancellation before Resilience4j retry policy evaluation.
- Nullable results are still passed to Resilience4j retry result predicates through a small Java bridge.
- `SuspendCacheImpl` rethrows JCache and loader cancellation before logging or error-event publication.
- README and README.ko clarify module boundaries, decorator ordering, cache paths, Flow semantics, and observability ownership.

## Verification

- `./gradlew :bluetape4k-resilience4j:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-resilience4j:test --no-configuration-cache --rerun-tasks` passed with 200 tests.
- `./gradlew :bluetape4k-resilience4j:koverVerify :bluetape4k-resilience4j:koverXmlReport --no-configuration-cache`
- `git diff --check`
- Final Claude blocker review reported `P0=0 P1=0`.

## Future Guidance

Before wrapping upstream Resilience4j Kotlin suspend helpers, inspect whether they catch `Exception`. If they do, explicitly rethrow `CancellationException` before policy callbacks, delay loops, logging, metrics, or fallback handling.
