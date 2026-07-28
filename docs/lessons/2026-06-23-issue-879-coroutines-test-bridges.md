# 이슈 #879 Coroutines test bridge removal

issue #879는 production coroutines source에서 deprecated
`io.bluetape4k.coroutines.tests` bridge package를 제거했다. repository는 이제 owner
module을 직접 사용한다. Flow assertion은 `bluetape4k-assertions`에서, dispatcher test
helper는 `bluetape4k-junit5`에서 가져온다.

## 결정

- `bluetape4k-coroutines` 아래 bridge source를 삭제한다.
- `withSingleThread`와 `withParallels`를 `io.bluetape4k.junit5.coroutines`로 이동한다.
- coroutines test와 example을 `io.bluetape4k.assertions.coroutines`로 migration한다.
- 필요한 곳에서는 `assertFailsWith`로 `CancellationException`을 assert해 newer assertion
  module의 cancellation contract를 보존한다.

## 교훈

- deprecated bridge API는 owner-module semantic을 숨길 수 있다. old bridge는
  `assertError`를 통해 `CancellationException`을 받아들였지만 assertion module은 의도적으로
  cancellation을 rethrow한다. migration은 bridge behavior를 재생성하지 말고 owner module
  contract를 보존해야 한다.
- shared test helper를 이동할 때 새 module dependency를 추가하지 않는다. `bluetape4k-junit5`는
  `parallelism` 검증이나 executor shutdown failure 무시를 위해 `bluetape4k-core`가 필요하지
  않다.
- full-package scan은 active Kotlin source와 historical planning note를 구분해야 한다. old
  package는 archived `docs/superpowers` context에만 남아 있다.

## 검증

- `./gradlew :bluetape4k-junit5:compileKotlin`이 통과했다.
- `./gradlew :bluetape4k-junit5:test :bluetape4k-assertions:test`가 통과했다.
- `./gradlew :bluetape4k-coroutines:compileTestKotlin :bluetape4k-coroutines:test`가 통과했다.
- `./gradlew :bluetape4k-examples-coroutines-demo:compileTestKotlin`이 통과했다. `TimeoutExamples.kt`의 기존 unused-expression warning은 남아 있다.
- `./gradlew :bluetape4k-coroutines:compileTestKotlin --warning-mode all`에는 old bridge package, `FlowAssertions`, `TestSupport` warning hit가 없었다.
- `rg "io\\.bluetape4k\\.coroutines\\.tests" -g '*.kt' -g '*.kts'`는 active Kotlin/KTS match를 반환하지 않았다.
- test result XML totals: junit5 269 tests, assertions 689 tests, coroutines 566 tests, failures/errors 0.
- `git diff --check`가 통과했다.
