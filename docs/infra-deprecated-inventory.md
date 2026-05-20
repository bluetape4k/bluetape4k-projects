# infra Deprecated API Inventory

Snapshot: 2026-05-09 KST
Updated: 2026-05-20 KST
Issue: [#110](https://github.com/bluetape4k/bluetape4k-projects/issues/110)

This inventory records tracked `infra/` and related cache source files that
still declare `@Deprecated`. Completed cleanup waves are removed from the active
inventory.

## Summary

| Decision | Count | Meaning |
|---|---:|---|
| Delete | 7 files | Deprecated API has a direct replacement and should not remain in the next cleanup line. |
| Replace call sites first | 2 files | Repo-local tests or samples still exercise the deprecated API and must move first. |
| Keep | 0 files | No deprecated `infra/` API needs long-term retention. |

`infra/kafka4` mirrors several `infra/kafka` deprecated APIs, so the current
scope is larger than the original 12-file issue comment.

2026-05-20 update: `kafka`/`kafka4` send aliases, metric aliases, and JDK-backed
Kafka codecs were removed from the active inventory in the issue #474 PR A
cleanup wave.

## Inventory

| # | Module | File | Deprecated API | Current usage | Decision | Replacement |
|---:|---|---|---|---|---|---|
| 1 | `cache-core` | `cache/core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt` | `AsyncCache.getSuspending(...)` | KDoc sample only | Delete | `AsyncCache.suspendGet(...)` |
| 2 | `cache-lettuce` | `cache/lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceSuspendNearCache.kt` | `clearFrontCache()` | Definition only | Delete | `clearLocal()` |
| 3 | `lettuce` | `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceConst.kt` | `DEFAULT_DELIMETER` typo | Definition only | Delete | `DEFAULT_DELIMITER` |
| 4 | `lettuce` | `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt` | `suspendAwait()`, `coAwait()` | `RedisFutureSupportTest` compatibility tests | Replace tests, then delete | `awaitSuspending()` |
| 5 | `opentelemetry` | `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt` | `SpanBuilder.useSuspendSpan(...)` overloads | One compatibility test path | Replace tests, then delete | `useSpanSuspending(...)` |
| 6 | `opentelemetry` | `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/trace/SpanExporterSupport.kt` | `spanExportOf(...)` | Definition only | Delete | `spanExporterOf(...)` |
| 7 | `opentelemetry` | `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/trace/SpanProcessorSupport.kt` | `batchSpanProcess(...)` | Definition only | Delete | `batchSpanProcessorOf(...)` |
| 8 | `redisson` | `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt` | `DEFAULT_DELIMETER` typo | Definition only | Delete | `DEFAULT_DELIMITER` |
| 9 | `resilience4j` | `infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt` | `decoreate()` typo overloads | Definition only | Delete | `decorate()` |

## Follow-Up PR Split

### PR A: Kafka deprecated surface cleanup

Status: Done in issue #474 PR A.

Scope:

- `infra/kafka`
- `infra/kafka4`

Tasks:

- Remove `sendAndAwait`, `awaitSend`, `awaitSendDefault`, and `sendSuspending`
  aliases after confirming no repo-local callers remain.
- Remove `getMetricValue(...)` aliases.
- Remove `JdkKafkaCodec` and compressed JDK-backed public factory/registry
  entries after the `@BluetapeObsoleteApi` + `DeprecationLevel.ERROR` staging
  period.

Validation:

- `./gradlew :bluetape4k-kafka:test :bluetape4k-kafka4:test --no-daemon`

### PR B: Cache, Redis, OpenTelemetry, and Resilience4j aliases

Scope:

- `cache/core`
- `cache/lettuce`
- `infra/lettuce`
- `infra/redisson`
- `infra/opentelemetry`
- `infra/resilience4j`

Tasks:

- Remove typo aliases: `DEFAULT_DELIMETER`, `decoreate()`.
- Remove coroutine alias APIs: `getSuspending`, `clearFrontCache`,
  `suspendAwait`, `coAwait`, `useSuspendSpan`.
- Remove naming aliases: `spanExportOf`, `batchSpanProcess`.
- Rewrite compatibility tests to exercise the canonical APIs only.

Validation:

- `./gradlew :bluetape4k-cache-core:test :bluetape4k-cache-lettuce:test :bluetape4k-lettuce:test :bluetape4k-redisson:test :bluetape4k-opentelemetry:test :bluetape4k-resilience4j:test --no-daemon`

## Notes For Follow-Up Work

- Bucket4j `RateLimitResult(consumedTokens, availableTokens)` cleanup was
  completed by issue #434.
- This inventory intentionally does not delete the remaining code.
- Keep each cleanup PR small enough to review API removal and test migration
  separately.
- When a cleanup PR removes public API, add the migration note to
  `CHANGELOG.md` under `Unreleased / Changed`.
- Prefer removal over keeping compatibility aliases because this repository is
  already preparing breaking 2.0+ cleanup work.
