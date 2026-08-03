# Flow 정책 후속 작업 설계

## 목적

Issue [#1300](https://github.com/bluetape4k/bluetape4k-projects/issues/1300)은
`bluetape4k-coroutines`의 `Flow` 연산자에 delay-error 조합과 명시적 overflow
정책을 도입할지 평가하도록 요청한다. 이번 설계는 새 public API를 추가하기
전에 Kotlin Flow의 고유 계약과 Reactive Streams 계열의 계약 차이를 고정하고,
호출자 근거가 부족한 기능을 공개 surface에 포함하지 않는 것을 목표로 한다.

## 현재 근거

### 저장소 호출자

- production 소스에서 #1300 대상인 `concatDelayError`, `mergeDelayError`,
  `flatMapDelayError`, `onBackpressureBuffer`, `onBackpressureLatest`,
  `bufferWhen`, `windowWhen`, `bufferWhile`, `windowWhile` 호출은 발견되지
  않았다.
- 현재 public 구현은 `concat`, `merge`, `onBackpressureDrop`이다.
  `concat`은 순차 `emitAll`, `merge`는 구조화된 child와 공용 queue,
  `onBackpressureDrop`은 collector가 준비되지 않은 동안 값을 버리는 단일
  슬롯 handoff를 사용한다.
- 해당 연산자의 직접 테스트는
  `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ConcatTest.kt`,
  `MergeFlowsTest.kt`, `OnBackpressureDropTest.kt`에 있으며, delay-error
  또는 overflow-error 계약을 요구하는 caller 테스트는 없다.
- `docs/flow-operator-inventory.md`와 양국어 coroutine README는 #1300을
  후속 범위로 연결하고 있다. 따라서 이번 작업은 기존 #1297 parity 범위를
  되풀이하지 않는다.

### 공식 계약

- Kotlin Flow는 기본적으로 순차적이며 `merge`는 순서를 보장하지 않고 모든
  입력을 동시에 수집한다. `buffer`는 channel capacity와
  `BufferOverflow.SUSPEND`, `DROP_OLDEST`, `DROP_LATEST`를 사용하고,
  `conflate`는 최신 값만 유지한다.
- Flow의 `catch`는 upstream 예외만 처리하고 cancellation을 일반 오류로
  변환하지 않는다. Flow 구현은 exception transparency와 cancellation 전파를
  지켜야 한다.
- RxJava `Flowable`의 `concatArrayDelayError`와 `mergeArrayDelayError`는
  Reactive Streams `Publisher`의 downstream demand와 source backpressure
  준수를 전제로 하며, 여러 오류를 하나의 terminal error로 전달한다.
- Reactor `Flux.concatDelayError`와 `mergeDelayError`는 순차/동시 구독과
  prefetch를 명시하고, `onBackpressureBuffer`/`onBackpressureLatest`는
  downstream demand가 부족할 때의 버퍼·drop·overflow-error를 별도 계약으로
  정의한다.

공식 참고 자료(2026-08-03 확인):

- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/
- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/buffer.html
- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/conflate.html
- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html
- https://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Flowable.html
- https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html

## 대안

### A. 계약 매트릭스와 회귀 증거만 확정한다 — 채택

새 public API를 추가하지 않고 현재 `concat`, `merge`,
`onBackpressureDrop` 및 표준 `buffer`/`conflate`의 의미를 문서화한다. 기존
계약을 결정적인 테스트로 잠그고, delay-error·explicit overflow-error·event
boundary window 계열은 caller evidence와 별도 메모리/오류 집계 설계가
확보될 때까지 보류한다.

장점은 Reactive Streams demand를 Kotlin Flow에 잘못 투영하지 않고, 현재
호출자에게 필요한 동작을 변경하지 않으며, 오류 집계와 cancellation 경계를
성급하게 공개하지 않는 것이다. 단점은 #1300의 모든 후보 API가 이번 릴리스에
추가되지 않는다는 점이다.

### B. `concatDelayError`만 추가한다

순차 수집 중 일반 오류를 저장하고 모든 source를 처리한 뒤 suppressed
exception으로 전달하는 API를 추가할 수 있다. 그러나 오류 집계 순서, 무한
source, cancellation 중 오류, public 이름의 import collision을 새로 결정해야
하며 현재 production caller가 없다.

### C. delay-error와 bounded overflow API를 함께 추가한다

`mergeDelayError`의 동시성/버퍼 상한, overflow-error/drop 정책, sibling
cancellation, 오류 집계를 모두 공개한다. RxJava/Reactor의 demand 계약을
Flow로 옮길 수 없고 검증 surface가 가장 커서 이번 범위에서는 채택하지 않는다.

## 선택한 계약

### 현재 API와 Flow 표준 대응

- `concat`: 입력 순서대로 순차 수집한다. source가 실패하면 이후 source를
  수집하지 않고 원래 예외를 전달한다.
- `merge`: 입력을 동시에 수집하고 도착 순서로 방출한다. source 간 순서는
  보장하지 않으며, 구조화된 child 하나가 실패하면 전체 collection이
  cancellation으로 정리된 뒤 원래 예외를 전달한다.
- `onBackpressureDrop`: Reactive Streams demand를 구현하지 않는다. collector가
  준비되지 않은 동안 단일 슬롯에 저장할 수 없는 값은 버린다. 사용자는
  손실 가능성을 명시적으로 수용해야 한다.
- `buffer`: 표준 Flow channel buffer를 사용한다. 기본 overflow는
  `SUSPEND`이며, capacity와 `DROP_OLDEST`/`DROP_LATEST`는 명시적으로 선택할
  수 있다.
- `conflate`: 최신 값 하나를 유지하는 표준 Flow shortcut이다. 이는
  `onBackpressureLatest`와 유사한 데이터 손실 결과를 만들 수 있지만,
  Reactive Streams demand 계약과 동일하다고 주장하지 않는다.

### 의도적 비목표

- `concatDelayError`, `mergeDelayError`, `flatMapDelayError`
- `onBackpressureBuffer`, `onBackpressureLatest`, overflow-error callback
- `bufferWhen`, `windowWhen`, `bufferWhile`, `windowWhile`
- RxJava/Reactor와의 외부 runtime interoperability 또는 demand-level parity

이 비목표들은 caller evidence가 생기고 다음 계약을 별도로 승인할 때 다시
열 수 있다.

1. 일반 오류를 몇 개까지 어떤 순서로 보존하고 suppressed/aggregate 하는가
2. `CancellationException`이 오류 집계에서 어떻게 제외되는가
3. 동시 source와 내부 queue의 최대 메모리 상한은 무엇인가
4. overflow 시 suspend, drop, error 중 어느 정책을 호출자가 명시하는가
5. downstream cancellation이 모든 child/source를 언제까지 정리하는가

## 검증 설계

1. 기존 `concat`의 fail-fast, `merge`의 sibling cancellation 및
   `onBackpressureDrop`의 손실 가능성을 `runTest` 기반 회귀 테스트로
   고정한다.
2. 표준 `buffer`와 `conflate`의 capacity/overflow 결과를 deterministic하게
   확인하고, `CancellationException`이 일반 오류로 변환되지 않음을 검증한다.
3. 새 production Kotlin API, dependency, module, generated catalog는
   변경하지 않는다.
4. 문서 검토가 끝난 뒤 실행 계획에서 README parity, matrix link, lesson,
   targeted test, `check`, `git diff --check` 순서를 확정한다.

## 완료 조건

- 계약 매트릭스와 caller evidence가 한국어 문서로 커밋된다.
- 테스트와 정적 검사는 기존 API 계약만 검증하며 production API diff는 없다.
- #1300의 미구현 후보와 재개 조건이 명확히 기록된다.
- PR/merge/release는 이번 승인 범위에 포함하지 않으며 최종 상태는 live
  GitHub 검토 전까지 `PENDING`으로 남긴다.
