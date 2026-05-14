# infra Deprecated API Inventory

Snapshot: 2026-05-09 KST
Issue: [#110](https://github.com/bluetape4k/bluetape4k-projects/issues/110)

This inventory records every tracked `infra/` source file that currently declares
`@Deprecated`. It is a planning document only; code removal and replacement
work should happen in follow-up PRs.

## Summary

| Decision | Count | Meaning |
|---|---:|---|
| Delete | 17 files | Deprecated API has a direct replacement and should not remain in the next cleanup line. |
| Replace call sites first | 6 files | Repo-local tests or samples still exercise the deprecated API and must move first. |
| Keep | 0 files | No deprecated `infra/` API needs long-term retention. |

`infra/kafka4` mirrors several `infra/kafka` deprecated APIs, so the current
scope is larger than the original 12-file issue comment.

## Inventory

| # | Module | File | Deprecated API | Current usage | Decision | Replacement |
|---:|---|---|---|---|---|---|
| 1 | `cache-core` | `cache/core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt` | `AsyncCache.getSuspending(...)` | KDoc sample only | Delete | `AsyncCache.suspendGet(...)` |
| 2 | `cache-lettuce` | `cache/lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceSuspendNearCache.kt` | `clearFrontCache()` | Definition only | Delete | `clearLocal()` |
| 3 | `kafka` | `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt` | `Producer.getMetricValue(...)` | Definition only | Delete | `getMetricValueOrNull(...).asDouble()` |
| 4 | `kafka` | `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt` | `JdkKafkaCodec` | `KafkaCodecTest` still tests `KafkaCodecs.Jdk` | Replace tests, then delete | `ForyKafkaCodec` or `KryoKafkaCodec` |
| 5 | `kafka` | `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/KafkaOperationsExtensions.kt` | `sendAndAwait(...)`, `awaitSendDefault(...)` overloads | Definition only | Delete | `suspendSend(...)` / `suspendSendDefault(...)` |
| 6 | `kafka` | `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/KafkaOperationExtensions.kt` | `awaitSend(...)`, `sendSuspending(...)`, `getMetricValue(...)` | KDoc/definition only | Delete | `suspendSend(...)`, `getMetricValueOrNull(...).asDouble()` |
| 7 | `kafka4` | `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt` | `Producer.getMetricValue(...)` | Definition only | Delete | `getMetricValueOrNull(...).asDouble()` |
| 8 | `kafka4` | `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt` | `JdkKafkaCodec` | `KafkaCodecTest` still tests `KafkaCodecs.Jdk` | Replace tests, then delete | `ForyKafkaCodec` or `KryoKafkaCodec` |
| 9 | `kafka4` | `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/spring/KafkaOperationsExtensions.kt` | `sendAndAwait(...)`, `awaitSendDefault(...)` overloads | Definition only | Delete | `suspendSend(...)` / `suspendSendDefault(...)` |
| 10 | `kafka4` | `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/spring/core/KafkaOperationExtensions.kt` | `awaitSend(...)`, `sendSuspending(...)`, `getMetricValue(...)` | Definition only | Delete | `suspendSend(...)`, `getMetricValueOrNull(...).asDouble()` |
| 11 | `lettuce` | `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceConst.kt` | `DEFAULT_DELIMETER` typo | Definition only | Delete | `DEFAULT_DELIMITER` |
| 12 | `lettuce` | `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt` | `suspendAwait()`, `coAwait()` | `RedisFutureSupportTest` compatibility tests | Replace tests, then delete | `awaitSuspending()` |
| 13 | `opentelemetry` | `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt` | `SpanBuilder.useSuspendSpan(...)` overloads | One compatibility test path | Replace tests, then delete | `useSpanSuspending(...)` |
| 14 | `opentelemetry` | `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/trace/SpanExporterSupport.kt` | `spanExportOf(...)` | Definition only | Delete | `spanExporterOf(...)` |
| 15 | `opentelemetry` | `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/trace/SpanProcessorSupport.kt` | `batchSpanProcess(...)` | Definition only | Delete | `batchSpanProcessorOf(...)` |
| 16 | `redisson` | `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt` | `DEFAULT_DELIMETER` typo | Definition only | Delete | `DEFAULT_DELIMITER` |
| 17 | `resilience4j` | `infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt` | `decoreate()` typo overloads | Definition only | Delete | `decorate()` |

## Follow-Up PR Split

### PR A: Kafka deprecated surface cleanup

Scope:

- `infra/kafka`
- `infra/kafka4`

Tasks:

- Remove `sendAndAwait`, `awaitSend`, `awaitSendDefault`, and `sendSuspending`
  aliases after confirming no repo-local callers remain.
- Remove `getMetricValue(...)` aliases.
- Replace `KafkaCodecs.Jdk` compatibility tests with supported codecs or remove
  the JDK-only compatibility case.
- Remove `JdkKafkaCodec` and any public factory/registry entry that exposes it,
  while keeping compressed JDK-backed codecs only if they are intentionally still
  supported outside this issue.

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
