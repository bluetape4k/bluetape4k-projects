# 이슈 #1297 Flow 연산자 동등성 교훈

## 결정

이번 변경은 기존 Flow 확장 패턴을 재사용하면서도 연산자별 lifecycle 계약을
명시하는 방향으로 수렴했다. 새 의존성이나 별도 dispatcher를 추가하지 않고
structured concurrency와 virtual-time 테스트를 기준으로 경계를 고정했다.

## 재사용할 규칙

1. count/time 연산자는 수신 clause를 timeout clause보다 먼저 등록한다. Kotlin
   `select`의 동일 시각 tie가 바뀌지 않도록 이 순서를 테스트로 고정한다.
2. `windowTimeout`은 live channel을 노출하지 않고 완료된 list snapshot을
   `asFlow()`로 감싼다. 그래야 window를 여러 번 수집해도 repeatable cold
   semantics를 유지한다.
3. timeout fallback은 `produceIn`만으로 cleanup을 추정하지 않는다. 명시적인
   upstream `Job`과 `Channel`을 두고 `cancelAndJoin`을 fallback/예외보다 먼저
   수행해 cleanup 완료를 관찰 가능하게 만든다.
4. eager mapping의 bounded overload는 `Semaphore`와 inner별 bounded
   `Channel`을 조합한다. 기존 무제한 호환 overload의 의미를 바꾸지 않고 새
   overload에서만 overflow/backpressure 경계를 공개한다.
5. benchmark timer는 `days` 같은 명시적 `Duration` 단위를 사용한다. 현재
   benchmark는 등록/소규모 list allocation 비용을 측정하며 외부 client의 실제
   네트워크 성능을 대표하지 않는다.

## 검증 명령

- `./gradlew :bluetape4k-coroutines:test --no-configuration-cache --console=plain`
  — 610 tests PASS.
- `./gradlew :bluetape4k-coroutines:check --no-configuration-cache --console=plain`
  — BUILD SUCCESSFUL, exit 0.
- `./gradlew :bluetape4k-coroutines:testCoroutinesFlowBenchmark
  --no-configuration-cache --console=plain` — 10 benchmark methods PASS.

## 후속 리스크

delay-error와 명시적 overflow 정책, 그리고 실제 RxJava/Reactor 또는 외부
클라이언트 interoperability는 #1300에서 별도로 결정한다. 이 이슈의 local
Flow 증거만으로 production runtime 호환성을 주장하지 않는다.
