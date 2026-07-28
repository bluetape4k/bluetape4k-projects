# Issue 435 Resilience4j Coroutine Facade

## 배경

Issue #435는 Bucket4j/Resilience4j module boundary split 이후
`bluetape4k-resilience4j` coroutine helper를 강화했다. 위험 영역은 coroutine
cancellation, suspend retry semantic, cache facade behavior였다.

## 결정

- Resilience4j 2.4.0이 public backing JCache accessor를 노출하지 않으므로 Resilience4j
  `Cache<K, V>` suspend extension은 compatibility path로 유지한다.
- Strict JCache semantic은 `SuspendCache.of(jcache)`를 통해 라우팅한다.
- `CancellationException`은 resilience failure가 아니라 coroutine control signal로
  다룬다. retry가 이를 재시도하면 안 되고, fallback이 이를 recover하면 안 되며, cache
  wrapper가 이를 cache error로 publish하면 안 된다.

## 결과

- `RetryCoroutines`는 Resilience4j retry policy evaluation 전에 cancellation을 다시
  던진다.
- Nullable result는 작은 Java bridge를 통해 Resilience4j retry result predicate로 계속
  전달된다.
- `SuspendCacheImpl`은 logging 또는 error-event publication 전에 JCache와 loader
  cancellation을 다시 던진다.
- README와 README.ko는 module boundary, decorator ordering, cache path, Flow semantic,
  observability ownership을 명확히 한다.

## 검증

- `./gradlew :bluetape4k-resilience4j:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-resilience4j:test --no-configuration-cache --rerun-tasks`는 200개 test로 통과했다.
- `./gradlew :bluetape4k-resilience4j:koverVerify :bluetape4k-resilience4j:koverXmlReport --no-configuration-cache`
- `git diff --check`
- Final Claude blocker review는 `P0=0 P1=0`을 보고했다.

## 향후 지침

Upstream Resilience4j Kotlin suspend helper를 wrapping하기 전에는 helper가 `Exception`을
catch하는지 확인한다. catch한다면 policy callback, delay loop, logging, metrics,
fallback handling 전에 `CancellationException`을 명시적으로 다시 던진다.
