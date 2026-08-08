# Flow 연산자 동등성 인벤토리

Issue #1297의 범위는 RxJava 3/Reactor `Flux` 용어 중 Kotlin Flow에서
호출자 가치가 높고 취소·메모리 계약을 명확히 검증할 수 있는 최소 집합으로
한정한다. 표준 `kotlinx.coroutines` 연산자가 이미 같은 계약을 제공하는
가족은 별도 wrapper를 만들지 않는다.

| 현재 API | 선택/제안 API | RxJava/Reactor 대응 | 표준 Flow 대응 또는 비목표 |
|---|---|---|---|
| `chunked`, `windowed` | `bufferTimeout`, `windowTimeout` | `Observable.buffer(timespan, count)`, `Flux.bufferTimeout` | 신규 count-or-time 계약 |
| 없음 | `timeout`, `timeoutOrFallback` | `Observable.timeout`, `Flux.timeout` | 신규 idle-timeout 계약 |
| 제한 없는 `concatMapEager` | 제한된 overload | 순서 보장 eager concat 계열 | 명시적 concurrency/inner queue 계약 |
| 없음 | `switchMap` 용어 결정 | `switchMap`, `switchOnNext` | `flatMapLatest`; 이 이슈에서는 wrapper를 추가하지 않음 |
| `merge`, `concat` | delay-error 후속 작업 | `mergeDelayError`, `concatDelayError` | [#1300](https://github.com/bluetape4k/bluetape4k-projects/issues/1300) 후속 |
| `onBackpressureDrop` | overflow 대응 | Reactor/Rx overflow 계열 | `buffer`/`conflate`; 명시적 overflow 정책은 #1300 |
| `withLatestFrom` | 기존 API | `withLatestFrom` | 기존 단일 secondary 계약 |
| 없음 | — | `combine`, `zip`, `retryWhen` | 표준 Flow 비목표 |

## 선택한 계약

- `bufferTimeout`와 `windowTimeout`은 `maxSize` 개수 또는 첫 원소부터의
  `timeout` 중 먼저 도달한 경계로 닫힌다. timer는 subscription 시점이
  아니라 첫 원소가 들어온 시점에 시작하며 빈 batch/window는 방출하지 않는다.
- 정상 완료 시 비어 있지 않은 마지막 부분 batch/window를 한 번 방출한다.
  upstream 실패 시 진행 중인 부분 값은 버리고 원래 예외를 전달한다.
- `windowTimeout`이 방출하는 각 `Flow`는 완료 snapshot을 감싼 repeatable
  cold `Flow`다. live single-consumer Reactor window로 취급하면 안 된다.
- `timeout`은 collection 시작 후 idle 기간을 감시하고 각 원소 뒤에 timer를
  재설정한다. `timeoutOrFallback`은 upstream 취소와 cleanup이 끝난 뒤
  fallback을 정확히 한 번 수집한다.
- `concatMapEager(maxConcurrency, bufferCapacity, transform)`은 source 순서를
  유지하면서 inner 동시성과 inner별 queue 용량을 제한한다. 기존
  `concatMapEager(transform)` 시그니처와 ordering은 유지한다.
- 모든 신규 연산자는 `CancellationException`을 data-plane 오류로 바꾸지
  않으며, dispatcher를 바꾸거나 detached child를 만들지 않는다.

## 벤치마크 근거

`CoroutinesFlowBenchmark`는 `bufferTimeoutThroughput`,
`windowTimeoutThroughput`, `boundedConcatMapEagerThroughput`를 포함한다.
count 경계를 즉시 만족하는 `1.days` timeout은 wall-clock 대기 없이 timer
등록과 리스트/queue allocation 비용을 측정한다. 실제 timeout 발생과 동률
우선순위는 `runTest` virtual-time 테스트가 증명한다.

## 의도적 비목표

`switchMap`, `buffer`, `conflate`, `combine`, `zip`, `retryWhen`은 표준 Flow
연산자를 사용한다. delay-error 조합, explicit overflow/error 정책,
event-boundary window 계열은 호출자 근거와 별도 메모리·취소 계약을 먼저
확정해야 하므로 #1300에서 다룬다.
