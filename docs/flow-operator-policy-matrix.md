# Flow 연산자 정책 매트릭스

Issue [#1300](https://github.com/bluetape4k/bluetape4k-projects/issues/1300)의
delay-error 및 overflow 후보를 현재 `bluetape4k-coroutines` 계약과 비교한다.
이 문서는 Reactive Streams demand 의미를 Kotlin Flow public API에 암묵적으로
투영하지 않도록 하는 기준선이다.

| 현재/후보 API | 현재 계약 또는 판정 | RxJava/Reactor 참고 | 이번 범위 |
|---|---|---|---|
| `concat` | 입력 순서대로 순차 수집, source 오류에서 fail-fast | `concat`, `concatArray` | 회귀 테스트로 고정 |
| `merge` | 모든 source 동시 수집, 도착 순서 방출, source 간 순서 없음 | `merge` | 회귀 테스트로 고정 |
| `onBackpressureDrop` | collector가 준비되지 않은 동안 단일 슬롯 handoff 밖의 값 손실 | `onBackpressureDrop` | Flow 전용 손실 계약 문서화 |
| `buffer(capacity, onBufferOverflow)` | channel capacity, 기본 `SUSPEND`, 선택적 `DROP_OLDEST`/`DROP_LATEST` | `onBackpressureBuffer`와 결과 일부가 유사하나 demand 계약은 다름 | 표준 API 검증 |
| `conflate` | 최신 값만 유지하는 표준 Flow shortcut | `onBackpressureLatest`와 결과가 유사 | 표준 API 검증 |
| `concatDelayError` | 미제공. 복수 오류 집계·무한 source·취소 계약 미결정 | RxJava `concatArrayDelayError`, Reactor `concatDelayError` | 비목표 |
| `mergeDelayError` | 미제공. 동시성·prefetch·오류 집계·메모리 상한 미결정 | RxJava `mergeArrayDelayError`, Reactor `mergeDelayError` | 비목표 |
| `flatMapDelayError` | 미제공. 제한된 concurrency와 오류 보존 정책 미결정 | RxJava/Reactor flat-map delay-error 계열 | 비목표 |
| `onBackpressureBuffer`/`Latest` error | 미제공. Flow에는 Reactive Streams demand signal이 없음 | Reactor/Rx overflow families | 비목표 |
| `bufferWhen`/`windowWhen`/`bufferWhile`/`windowWhile` | caller 없음, 경계 source lifecycle 미결정 | RxJava/Reactor boundary families | 비목표 |

## Flow 표준 계약

- Flow는 기본적으로 순차 실행되며 exception transparency를 지킨다.
- `buffer`는 capacity가 가득 차면 기본적으로 producer를 suspend한다.
  `BufferOverflow.DROP_OLDEST`는 이전 값을, `DROP_LATEST`는 새 값을 버린다.
- `conflate`는 `buffer(capacity = 0, onBufferOverflow = DROP_OLDEST)`와 같은
  최신 값 유지 정책이다.
- `catch`는 upstream 예외만 처리하고 cancellation을 처리 대상 data error로
  바꾸지 않는다.
- `merge`/`onBackpressureDrop` 구현은 coroutine scope 안에서 동작하지만,
  Reactive Streams `request(n)` 또는 `MissingBackpressureException`을
  제공하지 않는다.

## 호출자 근거

2026-08-03 isolated worktree에서 production 영역을 검색한 결과, #1300 후보
호출은 없었다. 직접 검증 가능한 기존 caller/test는 다음과 같다.

- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ConcatTest.kt`
- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/MergeFlowsTest.kt`
- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/OnBackpressureDropTest.kt`
- `bluetape4k/coroutines/README.md` 및 `README.ko.md`의 기존 merge 설명
- `docs/flow-operator-inventory.md`의 #1300 후속 링크

따라서 이번 milestone 단위에서는 새 후보 API의 구현 호출자를
추정하지 않는다.

## 재개 조건

다음 조건을 모두 충족하는 별도 승인 없이는 비목표 API를 추가하지 않는다.

1. 실제 production caller 또는 명시적인 downstream 요구가 issue에 연결된다.
2. 오류 집계 순서와 `CancellationException` 보존 규칙이 문서화된다.
3. 동시성·prefetch·buffer 상한을 deterministic test로 증명한다.
4. Kotlin Flow와 Reactive Streams의 의미 차이를 public KDoc과 README에
   명시한다.
