# 이슈 474 Cache Redis Resilience4j Deprecated API 제거

## 배경

Issue #474는 compatibility window 이후 deprecated infra/cache alias를 제거한다. 이 lane은
cache-core, cache-lettuce, lettuce, redisson, resilience4j만 다룬다.

## 결정

Compatibility shim을 유지하지 않고 forwarding alias를 제거한다:

- `AsyncCache.getSuspending`
- `LettuceSuspendNearCache.clearFrontCache`
- `RedisFuture.suspendAwait`
- `RedisFuture.coAwait`
- Redis 및 Redisson `DEFAULT_DELIMETER`
- `SuspendDecorators.decoreate`

Canonical API는 `suspendGet`, `clearLocal`, `awaitSuspending`, `DEFAULT_DELIMITER`, `decorate`로 유지한다.

## 결과

Compatibility test는 canonical API를 사용하도록 바뀌었고, touched module의 public deprecated alias
surface는 제거되었다.

## 검증

- `./gradlew :bluetape4k-cache-core:compileKotlin :bluetape4k-cache-core:compileTestKotlin :bluetape4k-cache-lettuce:compileKotlin :bluetape4k-cache-lettuce:compileTestKotlin :bluetape4k-lettuce:compileKotlin :bluetape4k-lettuce:compileTestKotlin :bluetape4k-redisson:compileKotlin :bluetape4k-redisson:compileTestKotlin :bluetape4k-resilience4j:compileKotlin :bluetape4k-resilience4j:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-cache-core:test --no-configuration-cache`: 455 passing.
- `./gradlew :bluetape4k-cache-lettuce:test --no-configuration-cache`: 427 passing.
- `./gradlew :bluetape4k-lettuce:test --no-daemon --no-configuration-cache`: daemon disappearance retry 1회 후 331 passing.
- `./gradlew :bluetape4k-redisson:test --no-daemon --no-configuration-cache`: 287 passing.
- `./gradlew :bluetape4k-resilience4j:test --no-daemon --no-configuration-cache`: 280 passing.
- `git diff --check`
- `rg -n "@Deprecated|getSuspending|clearFrontCache|suspendAwait\\b|coAwait\\b|DEFAULT_DELIMETER|decoreate" cache/cache-core cache/cache-lettuce infra/lettuce infra/redisson infra/resilience4j`

## 향후 가이드

#474 cleanup PR은 독립적으로 유지한다. `docs/infra-deprecated-inventory.md`가 Kafka 또는
OpenTelemetry cleanup PR과 conflict 나면 각 lane의 completed row를 모두 보존하고, 실제 remaining
deprecated API만 table에 남긴다.
