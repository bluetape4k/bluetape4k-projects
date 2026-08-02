# Flow 연산자 동등성 설계

## 문제

Issue #1297은 `bluetape4k-coroutines`의 Flow 확장을 대상으로 RxJava 3 및
Reactor `Flux`와의 동등성을 집중 점검하도록 요구한다. 이 모듈에는 이미
개수 기반 windowing, debounce batching, eager ordered mapping, 단일 drop
정책이 있지만 개수 또는 시간 기반 경계, 유휴 timeout fallback, 제한된
eager ordered mapping 계약은 없다. 이번 작업은 `kotlinx.coroutines`가 이미
제공하는 연산자를 다시 구현하지 않으면서 유용한 공개 API를 추가해야 한다.

## 현재 근거

- `windowed.kt`는 개수/간격 기반 window를 구현하고, `windowedFlow`는 각
  window를 cold `Flow`로 구체화한다.
- `bufferingDebounce.kt`는 `produceIn`과 `whileSelect`를 사용하지만 debounce
  간격을 `System.nanoTime`으로 측정한다. 이 방식은 결정적인 가상 시간
  테스트에 적합하지 않다.
- `concatMapEager.kt`는 원본 항목마다 내부 collector를 하나씩 시작하고 값을
  용량 제한이 없는 `ConcurrentLinkedQueue`에 저장한다.
- `onBackpressureDrop.kt`는 backpressure를 이름에 포함한 유일한 사용자 정의
  연산자다.
- `bluetape4k-coroutines`에는 이미 `kotlinx-coroutines-test`, JUnit 5,
  bluetape assertion, kotlinx benchmark target이 있다.
- Reactor `Flux.bufferTimeout(maxSize, maxTime)`은 개수 또는 시간 경계를
  정의한다. RxJava `Observable.buffer(timespan, count)`도 개수와 시간 중 먼저
  충족되는 경계를 동일하게 명시한다. Kotlin Flow는 기본적으로 순차 실행되며
  `buffer`와 `flatMapMerge`가 표준 동시성 구성 요소다.
- Kotlin `select`는 먼저 준비된 절에 편향된다. 구현에서는 입력 수신 절을
  timeout 절보다 먼저 등록하므로 제한 시각과 같은 가상 시각에 준비된 값이
  경합에서 우선한다. 다음 반복에서는 아직 열린 batch에 timeout을 적용한다.

참고 자료:

- https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#bufferTimeout(int,java.time.Duration)
- https://reactivex.io/RxJava/3.x/javadoc/3.0.10/io/reactivex/rxjava3/core/Observable.html
- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html
- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/buffer.html

## 제약 사항

1. 공개 GitHub 산출물은 영어를 유지한다. KDoc과 README 예제는 저장소의
   한국어 우선 KDoc 및 양국어 README 규칙을 따른다.
2. `CancellationException`을 일반 연산자 오류로 변환하거나 data-plane
   실패와 합산하지 않는다.
3. 새 timer 동작은 coroutine suspension(`onTimeout`/`delay`)을 사용하여
   `runTest` 가상 시간으로 증명할 수 있어야 한다.
4. 기존 `concatMapEager(transform)`의 소스 호환성과 순서 계약을 유지한다.
5. dependency, module, 생성 catalog를 변경하지 않는다.
6. 개수 또는 시간 timer는 구독 시점이 아니라 첫 항목이 batch/window에
   들어올 때 시작한다. 빈 window는 방출하지 않으며, 외부에 제공하는 각
  window는 완료된 스냅숏을 기반으로 반복 수집 가능한 cold `Flow`다.
7. 유휴 timeout은 수집 시작 시점에 시작하고 항목을 방출할 때마다 재설정한다.
   `FlowTimeoutException`은 `java.util.concurrent.TimeoutException`을 상속한다.

## 대안 비교

### A. 모든 Rx/Reactor 계열을 하나의 대규모 연산자 계층에 추가한다

용어 동등성은 극대화되지만 표준 Flow 연산자를 중복 구현하고 독립적인 오류
정책을 여러 개 만들며 cancellation 의미 검토를 어렵게 한다. 이번 배포에서는
채택하지 않는다.

### B. 문서 alias와 inventory matrix만 추가한다

위험은 낮지만 issue에서 식별한 P1 개수/시간 경계와 유휴 시간 공백을 해결하지
못한다. 제공 범위가 불충분하므로 채택하지 않는다.

### C. 계약 우선의 고가치 부분집합만 구현한다(채택)

개수 또는 시간 기반 batching/window, 명시적 fallback을 지원하는 유휴 timeout,
제한된 eager ordered mapping을 구현한다. `switchMap`은 표준 `flatMapLatest`에,
`buffer`/`conflate`/overflow는 표준 Flow에 대응시킨다. delay-error와 사용자
정의 backpressure 정책 계열은 연결된 후속 작업으로 분리한다. 공개 surface를
작게 유지하면서 요구된 수명주기와 concurrency 계약을 증명할 수 있다.

## 선택한 설계

### 1. 개수 또는 시간 기반 연산자

다음 API를 추가한다.

```kotlin
fun <T> Flow<T>.bufferTimeout(maxSize: Int, timeout: Duration): Flow<List<T>>
fun <T> Flow<T>.windowTimeout(maxSize: Int, timeout: Duration): Flow<Flow<T>>
```

두 API 모두 `maxSize > 0`과 `timeout.isPositive()`를 검증한다. 새
buffer/window는 첫 항목이 도착할 때 시작하며 다음 조건 중 먼저 충족되는
시점에 닫힌다.

- 항목 수가 `maxSize`에 도달한 경우
- buffer/window 시작 후 `timeout`이 경과한 경우

upstream이 정상 완료되면 비어 있지 않은 마지막 부분 buffer/window를 한 번
방출한다. upstream 실패는 처리 중인 부분 값을 방출하지 않고 그대로 전파한다.
이는 모듈의 `windowed` 동작과 일치하며 실패한 batch의 데이터가 공개되는 것을
막는다. Cancellation은 producer와 timer를 함께 취소한다. 공용 내부 개수/시간
collector가 timer와 list 할당을 소유하며, window API는 완료된 각 list를
`asFlow()`로 제공한다. 공개 전에 스냅숏이 완료되므로 반환된 window를 여러
번 수집해도 같은 값이 재생된다. 이 동작은 실시간 Reactor window와 의도적으로
다르며 이전 안내에 명시한다.

### 2. 유휴 timeout과 fallback

다음 API를 추가한다.

```kotlin
class FlowTimeoutException(val timeout: Duration) : java.util.concurrent.TimeoutException

fun <T> Flow<T>.timeout(timeout: Duration): Flow<T>
fun <T> Flow<T>.timeoutOrFallback(timeout: Duration, fallback: Flow<T>): Flow<T>
```

timer는 수집을 시작할 때 시작하고 upstream 항목을 받을 때마다 재설정한다.
정상 upstream 완료는 대기 중인 timer보다 우선한다. Timeout이 발생하면 먼저
upstream을 취소한다. fallback이 없는 overload는 `FlowTimeoutException`을
던지고, fallback overload는 fallback을 정확히 한 번 수집한다. Upstream과
fallback 실패는 변경하지 않고 전파하며 `CancellationException`은 항상
cancellation으로 전파한다.

### 3. 제한된 eager ordered mapping

현재 overload를 유지하고 다음 overload를 추가한다.

```kotlin
fun <T : Any, R : Any> Flow<T>.concatMapEager(
    maxConcurrency: Int,
    bufferCapacity: Int = maxConcurrency,
    transform: suspend (T) -> Flow<R>,
): Flow<R>
```

기존 overload는 제한이 없는 호환 경로에 위임한다. 새 경로는 내부 collector를
만들기 전에 `Semaphore(maxConcurrency)`를 획득하고 내부 항목별
`Channel<R>(bufferCapacity)`을 사용한다. 정렬된 출력 queue가 가득 차면 내부
producer가 suspend된다. Downstream drain은 계속 원본 순서대로 내부 queue를
소비한다. Permit 해제와 channel 종료는 내부 `finally` 블록에서 수행한다.
Child 실패는 구조화된 `channelFlow`를 취소하며 분리되거나 누수되는 child는 없다.

### 4. Inventory와 제외 범위

Inventory matrix에는 `switchMap`/`switchOnNext`를 표준 `flatMapLatest` 대응으로
기록하고 `buffer`, `conflate`, `retryWhen`, `combine`, `zip`은 표준 Flow 또는
비목표로 기록한다. Delay-error 조합, 사용자 정의 buffer/latest overflow 정책,
event-boundary window는 이 issue에서 구현하지 않는다. 코드 구현을 시작하기
전에 연결된 후속 issue 하나에 해당 계열을 기록한다.

## 실패 형태와 완화책

1. **Timer와 개수 경합:** timeout과 같은 가상 시각에 값이 도착할 수 있다.
   수신 절을 먼저 등록하므로 Kotlin의 편향된 `select`에서 값이 우선한다.
   timeout만 발생한 반복은 현재의 비어 있지 않은 batch를 닫는다. `runTest`로
   두 순서 조합을 모두 검증한다.
2. **Upstream 실패 시 부분 데이터:** channel이 원인과 함께 닫히면 처리 중인
   list를 의도적으로 폐기한다. 예외 전에 부분 값이 방출되지 않음을 검증한다.
3. **Timeout fallback 누수:** fallback을 수집하기 전에 upstream producer를
   취소한다. `finally` marker로 정리를 증명한다.
4. **Eager queue 증가:** 앞선 내부 작업이 느린 동안 제한된 `Channel` 용량이
   내부 producer를 suspend시킨다. 활성 동시성과 제한된 queue 구성을 검증한다.
5. **Cancellation 오분류:** 모든 연산자 catch는 `CancellationException`을 다시
   던진다. 원래 cancellation이 collector에 도달하고 fallback 또는 오류 합산이
   일어나지 않음을 검증한다.

## 호환성과 이전

- 기존 함수를 제거하거나 이름을 바꾸지 않는다.
- 기존 `concatMapEager(transform)`는 signature와 원본 순서를 유지한다. 메모리와
  동시성 제한이 필요한 호출자는 새 overload를 선택한다.
- 새 type과 함수는 추가형이며 `io.bluetape4k.coroutines.flow.extensions`에 둔다.
- `bufferTimeout`/`windowTimeout` timer는 첫 항목에서 시작하며 빈 window를
  공개하지 않는다. Reactor에서 이전하는 호출자는 구독 시 timer가 만든 빈
  window를 가정해서는 안 된다.
- `windowTimeout` 결과는 실시간 단일 소비자 window가 아니라 반복 가능한 cold
  스냅숏(`asFlow()`)이다. `timeoutOrFallback`은 upstream 정리가 완료된 뒤에만
  fallback을 구독한다.
- README 예제는 새 계약을 보여 주고 제외된 계열에는 표준 Flow 연산자를
  사용하도록 명시한다.

## 수용 기준

- Inventory matrix는 현재 API, 선택한 API, Rx/Reactor 대응 항목, 표준 Flow 또는
  비목표 대응을 포함한다.
- KDoc과 두 README locale은 선택한 모든 연산자의 완료, 실패, cancellation,
  순서, buffering, concurrency 의미를 문서화한다.
- `kotlinx-coroutines-test`는 개수 경계, timeout 경계, 부분 완료, upstream 실패,
  fallback, cancellation, 가상 시간, 제한된 eager 동시성과 순서를 검증한다.
- Benchmark 근거는 개수/시간 timer 등록 및 list 할당과 제한된 eager queue
  동작을 다룬다. Timer 발생과 제한 시각의 의미는 benchmark가 아니라 가상 시간
  테스트로 증명한다.
- 코드를 작성하기 전에 중복 검색과 연결된 후속 issue를 기록한다.
- `git diff --check`, targeted test, module check, 저장소에 적용되는 Kotlin 및
  정적 검사가 통과한다.

## 완료 정의

Stacked PR train이 모두 green이고, 각 PR 본문이 `## DoD Status`로 끝나며,
최종 inventory와 후속 링크가 게시되고, exact-head CI/review/mergeability 근거가
최신일 때만 merge-ready로 판정한다. 최종 보고서는 확인하지 않은 release 또는
merge gate를 추정하지 않고 `PENDING`으로 표시해야 한다.
