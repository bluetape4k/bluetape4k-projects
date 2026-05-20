# Issue 474 Cache Redis Resilience4j Deprecated API Removal

## Context

Issue #474 removes deprecated infra/cache aliases after the compatibility window. This lane covers cache-core, cache-lettuce, lettuce, redisson, and resilience4j only.

## Decision

Remove forwarding aliases instead of keeping compatibility shims:

- `AsyncCache.getSuspending`
- `LettuceSuspendNearCache.clearFrontCache`
- `RedisFuture.suspendAwait`
- `RedisFuture.coAwait`
- Redis and Redisson `DEFAULT_DELIMETER`
- `SuspendDecorators.decoreate`

Canonical APIs remain `suspendGet`, `clearLocal`, `awaitSuspending`, `DEFAULT_DELIMITER`, and `decorate`.

## Outcome

Compatibility tests now exercise canonical APIs, and the public deprecated alias surface is removed from the touched modules.

## Verification

- `./gradlew :bluetape4k-cache-core:compileKotlin :bluetape4k-cache-core:compileTestKotlin :bluetape4k-cache-lettuce:compileKotlin :bluetape4k-cache-lettuce:compileTestKotlin :bluetape4k-lettuce:compileKotlin :bluetape4k-lettuce:compileTestKotlin :bluetape4k-redisson:compileKotlin :bluetape4k-redisson:compileTestKotlin :bluetape4k-resilience4j:compileKotlin :bluetape4k-resilience4j:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-cache-core:test --no-configuration-cache`: 455 passing.
- `./gradlew :bluetape4k-cache-lettuce:test --no-configuration-cache`: 427 passing.
- `./gradlew :bluetape4k-lettuce:test --no-daemon --no-configuration-cache`: 331 passing after one daemon-disappearance retry.
- `./gradlew :bluetape4k-redisson:test --no-daemon --no-configuration-cache`: 287 passing.
- `./gradlew :bluetape4k-resilience4j:test --no-daemon --no-configuration-cache`: 280 passing.
- `git diff --check`
- `rg -n "@Deprecated|getSuspending|clearFrontCache|suspendAwait\\b|coAwait\\b|DEFAULT_DELIMETER|decoreate" cache/cache-core cache/cache-lettuce infra/lettuce infra/redisson infra/resilience4j`

## Future Guidance

Keep #474 cleanup PRs independent. If `docs/infra-deprecated-inventory.md` conflicts with Kafka or OpenTelemetry cleanup PRs, preserve all completed rows from each lane and keep only truly remaining deprecated APIs in the table.
