# Issue 434 Bucket4j Facade 강화

## 배경

Issue #434는 `infra/bucket4j`를 집중된 Kotlin/JVM Bucket4j facade로 강화했다.
위험은 broad API drift였다. `RateLimitResult`에는 안정적인 retry diagnostic이
필요했고, distributed suspend behavior에는 명시적인 cancellation/timeout semantic이
필요했으며, distributed provider에는 더 안전한 key/policy lifecycle documentation이
필요했다.

## 결정

- Module boundary를 좁게 유지한다. token-bucket rate limiting만 담당하고 retry,
  timeout policy composition, circuit breaker, bulkhead, fallback은
  Resilience4j.
- `RateLimitResult`는 Bucket4j probe type을 노출하지 않고 안정적인 bluetape4k
  diagnostic을 직접 가진다.
- local 또는 distributed bucket resolution 전에 512-byte prefixed bucket key limit을
  강제한다.
- Optional async-store timeout을 추가하면서도 `@JvmOverloads`로
  `DistributedSuspendRateLimiter`의 Java ergonomics를 유지한다.

## 결과

- `RateLimitDiagnostics`, `RateLimitRejectionReason`, `retryAfter`가 rejection
  diagnostic을 담당한다.
- Deprecated `RateLimitResult(consumedTokens, availableTokens)` constructor를 제거하고
  deprecated inventory를 갱신했다.
- README 파일은 provider lifecycle, Redis expiration ownership, configuration
  replacement용 bandwidth ID, async support, module boundary를 문서화한다.

## 검증

- `./gradlew :bluetape4k-bucket4j:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-bucket4j:test --no-configuration-cache` (121 tests)
- `./gradlew :bluetape4k-bucket4j:koverVerify --no-configuration-cache`
- Claude review는 P0/P1 finding이 없다고 보고했다. low-risk P2 redaction, docs,
  key-boundary test 제안은 반영했다.

## 향후 에이전트 메모

- 이 module에 retry나 circuit-breaker policy behavior를 추가하지 않는다. integration
  point를 문서화하고 해당 policy는 Resilience4j에 둔다.
- Bucket4j configuration replacement example을 바꿀 때는 English/Korean README 양쪽의
  안정적인 `Bandwidth.id(...)` 값을 유지한다.
- Data class에 public result field를 추가할 때는 Kotlin `copy` / `componentN`과 Java
  constructor compatibility를 명시적으로 확인한다.
