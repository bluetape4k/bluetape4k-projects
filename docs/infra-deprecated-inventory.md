# infra Deprecated API Inventory

Snapshot: 2026-05-09 KST
Updated: 2026-05-20 KST
Issues: [#110](https://github.com/bluetape4k/bluetape4k-projects/issues/110),
[#474](https://github.com/bluetape4k/bluetape4k-projects/issues/474)

This inventory records tracked `infra/` and related cache source files that
still declare `@Deprecated`. Completed cleanup waves are removed from the active
inventory.

## Summary

| Decision | Count | Meaning |
|---|---:|---|
| Delete | 0 files | No tracked deprecated API remains in the current cleanup inventory. |
| Replace call sites first | 0 files | No repo-local compatibility tests or samples remain on tracked deprecated APIs. |
| Keep | 0 files | No deprecated `infra/` API needs long-term retention. |

`infra/kafka4` mirrors several `infra/kafka` deprecated APIs, so the current
scope is larger than the original 12-file issue comment.

2026-05-20 update: `kafka`/`kafka4` send aliases, metric aliases, and JDK-backed
Kafka codecs were removed from the active inventory in the issue #474 PR A
cleanup wave.

2026-05-20 update: OpenTelemetry aliases `SpanBuilder.useSuspendSpan`,
`spanExportOf`, and `batchSpanProcess` were removed; use `useSpanSuspending`,
`spanExporterOf`, and `batchSpanProcessorOf`.

2026-05-20 update: cache, Redis, and Resilience4j aliases were removed:
`AsyncCache.getSuspending`, `LettuceSuspendNearCache.clearFrontCache`,
`RedisFuture.suspendAwait`, `RedisFuture.coAwait`, Redis/Redisson
`DEFAULT_DELIMETER`, and `SuspendDecorators.decoreate`.

## Inventory

| # | Module | File | Deprecated API | Current usage | Decision | Replacement |
|---:|---|---|---|---|---|---|
| - | - | - | - | - | - | - |

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

### PR B: OpenTelemetry aliases

Status: Done in issue #474 PR B.

Scope:

- `infra/opentelemetry`

Tasks:

- Remove coroutine alias APIs: `useSuspendSpan`.
- Remove naming aliases: `spanExportOf`, `batchSpanProcess`.
- Rewrite compatibility tests to exercise the canonical APIs only.

Validation:

- `./gradlew :bluetape4k-opentelemetry:test --no-daemon`

### PR C: Cache, Redis, and Resilience4j aliases

Status: Done in issue #474 PR C.

Scope:

- `cache/cache-core`
- `cache/cache-lettuce`
- `infra/lettuce`
- `infra/redisson`
- `infra/resilience4j`

Tasks:

- Remove typo aliases: `DEFAULT_DELIMETER`, `decoreate()`.
- Remove coroutine alias APIs: `getSuspending`, `clearFrontCache`,
  `suspendAwait`, `coAwait`.
- Rewrite compatibility tests to exercise the canonical APIs only.

Validation:

- `./gradlew :bluetape4k-cache-core:test :bluetape4k-cache-lettuce:test :bluetape4k-lettuce:test :bluetape4k-redisson:test :bluetape4k-resilience4j:test --no-daemon`

## Notes For Follow-Up Work

- Bucket4j `RateLimitResult(consumedTokens, availableTokens)` cleanup was
  completed by issue #434.
- This inventory now records the completed cleanup waves; no tracked active
  rows remain.
- Keep each cleanup PR small enough to review API removal and test migration
  separately.
- When a cleanup PR removes public API, add the migration note to
  `CHANGELOG.md` under `Unreleased / Changed`.
- Prefer removal over keeping compatibility aliases because this repository is
  already preparing breaking 2.0+ cleanup work.
