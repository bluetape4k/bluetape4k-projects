# Coroutine 및 동기 API timeout 설계

## 목표

`feature/coroutines-timeout`에 추가된 timeout API 전체를 검증하고, 호출자가 결과를 받을 수 없거나 JDK 계약과 다른 부분을 바로잡는다. 핵심은 제한 시간 내 결과 획득, timeout과 업무 예외의 구분, 대기 취소와 원본 작업의 소유권 경계, 기존 `SuspendLazy` 구현체와의 JVM 호환성을 테스트로 고정하는 것이다.

## 범위

- `CompletableFuture`의 `Duration` 기반 `get`, `getOrNull`과 같은 파일의 `join`, `joinOrNull`, 기본값 함수 계약
- `ExecutorService.invokeAll` 및 `invokeAny`의 함수형 작업과 제한 시간 오버로드
- `SuspendLazy.getUntil` 및 `getUntilOrNull`
- `Deferred.awaitUntil` 및 `awaitUntilOrNull`
- `Job.joinUntil`
- 영향받은 공개 API의 한국어 KDoc과 core/coroutines 한·영 README 대응 문서

테스트와 공개 API 계약을 함께 고친다. 새 의존성이나 새 모듈은 추가하지 않는다. 현재 `feature/coroutines-timeout` 변경은 공개되기 전이므로, `ExecutorService.invokeAll/invokeAny`의 반환형 정정은 이 미출시 변경분 안에서 수행한다.

## 승인된 설계 결정

### 대안

1. 현재 서명은 유지하고 회귀 테스트만 추가한다. 구현 결함을 고치더라도 `invokeAll`/`invokeAny`가 결과를 반환하지 않는 공개 계약은 그대로 남는다.
2. **승인됨:** `invokeAll`은 JDK와 같은 `List<Future<T>>`, `invokeAny`는 성공한 작업의 `T`를 반환하고, 함수형 작업을 실제 호출한다. `SuspendLazy`의 timeout 멤버는 인터페이스 기본 구현으로 제공한다. 이 변경은 결과 손실을 바로잡고 기존 구현체가 새 멤버를 직접 구현하지 않아도 되게 한다.
3. `SuspendLazy`의 새 기능을 인터페이스 멤버가 아닌 top-level 확장 함수로 제공한다. ABI 부담은 줄지만 승인된 멤버 API와 달라지고 인터페이스 타입을 통한 멤버 호출 경험이 사라진다.

대안 2를 선택한다. 빌드는 `-jvm-default=enable`을 사용하므로 공통 timeout 멤버는 인터페이스 기본 메서드로 구현한다. `SuspendLazyImpl`은 기본 메서드를 그대로 사용하고, `SuspendBlockingLazyImpl`만 초기화된 캐시의 빠른 반환을 위해 동등한 timeout 계약의 최적화 override를 유지한다. 이전 인터페이스에 대해 컴파일한 구현체 fixture를 새 라이브러리와 함께 로드하고 기본 메서드를 호출하는 검증도 둔다. `checkBinaryCompatibility`/`checkProductionAbi`와 JVM default method 확인은 이 런타임 검증을 대신하지 않는다.

## 용어와 공통 경계

- `Duration`은 호출 시점부터 계산하는 상대 timeout이다. `Instant`는 특정 시각을 나타내는 절대 deadline이며 이 변경에서 서로 바꾸지 않는다.
- 동기 JDK 경로의 `TimeoutException`과 coroutine 경로의 `TimeoutCancellationException`은 다른 계약이다. coroutine timeout은 현재 대기 coroutine의 취소로 전달된다.
- 이 API들은 자체적으로 timeout 값을 검증하거나 반올림하지 않고 호출 대상의 Duration/JDK 동작을 따른다. JDK 경로는 `Duration.inWholeNanoseconds`의 포화 변환을 사용하므로 `Duration.INFINITE`는 `Long.MAX_VALUE` 나노초의 매우 긴 유한 제한 시간이 된다. 표준 `kotlinx.coroutines` dispatcher에서는 coroutine 경로의 `Duration.INFINITE`가 timeout 없는 대기로 동작한다. 실제 시간 처리는 dispatcher의 `Delay` 구현에 위임되므로 사용자 정의 dispatcher의 동작까지 보장하지 않는다. JDK 경로에서 0 또는 음수는 기다리지 않지만 이미 완료된 future/task는 위임된 JDK 계약에 따라 결과를 반환할 수 있고, 미완료 작업은 즉시 timeout/취소 경계에 놓인다. coroutine `withTimeout`/`withTimeoutOrNull` 경로에서 0 또는 음수는 블록을 실행하지 않고 즉시 timeout 된다. 단, `suspendBlockingLazy`는 초기화된 캐시 경로에서 현재 호출 coroutine의 취소를 `ensureActive()`로 확인한 뒤 값을 반환한다. 이 빠른 경로는 timeout 타이머를 만들지 않으므로 호출 coroutine이 활성 상태라면 0 또는 음수 timeout도 캐시 반환을 막지 않는다. coroutine timeout의 실제 발화 시각은 dispatcher/scheduler 정밀도에 좌우되므로 실제 시간 기준의 엄격한 제한 시간은 보장하지 않는다.
- timeout이 대기자를 반환시켜도 원본 작업은 계속 실행될 수 있다. 원본을 만든 호출자, scope, executor 또는 future의 소유자가 필요하면 별도로 취소하고 리소스를 정리한다.

## 공개 API 계약

| API | 성공 | 제한 시간 초과 | 원본 작업과 예외 |
|---|---|---|---|
| `CompletableFuture.get(Duration)` | 완료된 `V` 반환 | `TimeoutException` 전파 | 원본 future를 취소하지 않는다. JDK `get`의 `ExecutionException` 래핑과 대기 중 interrupt의 `InterruptedException`을 유지한다. |
| `CompletableFuture.get(Duration, defaultValue)` | 완료된 `V` 반환 | 기본값 반환 | timeout만 처리하며 업무 실패와 대기 중 interrupt는 전파한다. |
| `CompletableFuture.getOrNull(Duration)` | 완료된 `V` 반환 | `null` 반환 | timeout만 처리하며 업무 실패와 대기 중 interrupt는 전파한다. nullable `V`의 실제 `null`과 timeout은 구분되지 않는다. |
| `CompletableFuture.join(Duration)` | 완료된 `V` 반환 | `TimeoutException` 전파 | `ExecutionException`의 원인을 unwrap하고 대기 중 interrupt는 전파한다. future 자체는 취소하지 않는다. |
| `CompletableFuture.join(Duration, defaultValue)` | 완료된 non-null `V` 반환 또는 완료 결과가 `null`이면 기본값 반환 | 실제 대기 timeout에도 기본값 반환 | unwrap된 업무 실패와 대기 중 interrupt는 전파한다. 업무 예외의 원인이 `TimeoutException`이어도 기본값으로 바꾸지 않는다. |
| `CompletableFuture.joinOrNull(Duration)` | 완료된 `V` 반환 | `null` 반환 | timeout만 처리하며 업무 실패와 대기 중 interrupt는 전파한다. nullable 결과의 `null`과 timeout은 구분되지 않는다. |
| `ExecutorService.invokeAll(tasks, Duration)` | 입력 순서에 대응하는 `List<Future<T>>` 반환 | JDK timed overload처럼 미완료 future가 취소된 목록 반환 | 실행을 시작한 `Callable` 래퍼는 대응하는 `() -> T`를 호출하며, timeout/interrupt 전에 시작되지 않은 함수는 호출되지 않을 수 있다. 작업 예외는 해당 future의 `get()`에서 전파된다. 호출자 interrupt는 `InterruptedException`, 종료 executor의 거부는 `RejectedExecutionException` 등 JDK 예외 계약을 따른다. |
| `ExecutorService.invokeAny(tasks, Duration)` | 먼저 성공한 작업의 `T` 반환 | `TimeoutException` 전파 | 실행을 시작한 작업은 대응하는 함수를 호출하지만, 성공 결과 확정 또는 timeout/interrupt 전에 시작되지 않은 함수는 호출되지 않을 수 있다. 모든 작업 실패는 `ExecutionException`, 빈 입력은 `IllegalArgumentException`이다. 대기 중 interrupt는 `InterruptedException`, 거부된 제출은 `RejectedExecutionException` 등 JDK 계약을 따르며, 정상적으로 반환하거나 예외로 종료할 때 미완료 작업은 취소된다. |
| `SuspendLazy.getUntil(Duration)` | `invoke()` 결과 반환 | `TimeoutCancellationException` 전파 | 기본 구현은 호출 coroutine에 timeout을 적용한다. 사용자 구현체의 내부 작업 취소 여부는 `invoke()` 구현 계약을 따른다. |
| `SuspendLazy.getUntilOrNull(Duration)` | `invoke()` 결과 반환 | `null` 반환 | `withTimeoutOrNull` 의미를 따른다. nullable 결과의 `null`과 timeout은 구분되지 않으며 바깥 취소와 내부 업무 실패는 전파한다. |
| `Deferred.awaitUntil(Duration = 5.seconds)` | deferred 값 반환 | `TimeoutCancellationException` 전파 | helper가 원본 deferred를 직접 취소하지 않는다. 원본 실패와 바깥 취소는 전파한다. |
| `Deferred.awaitUntilOrNull(Duration = 5.seconds)` | deferred 값 반환 | `null` 반환 | helper가 원본 deferred를 직접 취소하지 않는다. timeout 외 실패·바깥 취소는 전파하며 nullable 결과의 `null`과 timeout은 구분되지 않는다. |
| `Job.joinUntil(Duration)` | 대상 job이 성공·실패·취소 중 어떤 이유로든 완료되면 대기자가 활성 상태인 한 정상 반환 | `TimeoutCancellationException` 전파 | timeout은 대기 coroutine만 취소하며 대상 job을 직접 취소하지 않는다. 호출자 취소도 전파한다. |

`join(Duration, defaultValue)`는 timed JDK `get`을 직접 호출해 실제 대기 `TimeoutException`과 완료 결과를 판별한다. 기존 계약에 따라 실제 대기 timeout과 완료 결과 `null`은 기본값으로 바꾼다. 업무 예외는 `ExecutionException`으로 감싼 상태에서 판별하므로 원인이 `TimeoutException`이어도 기본값과 혼동하지 않고 원인을 unwrap해 전파한다.

`ExecutorService`의 timeout은 함수형 작업을 `Callable`로 감싸는 O(n) 입력 변환 뒤 JDK timed overload에 전달된다. 따라서 변환 비용을 포함한 전체 래퍼 호출에 실제 시간 기준의 엄격한 제한 시간을 약속하지 않는다. timed overload는 미완료 작업을 취소하지만 실제 인터럽트 전달은 executor 구현에 달려 있다. 취소 상태는 작업 종료를 뜻하지 않으며, 실제 작업 중단과 리소스 해제에는 작업의 협조와 executor 소유자의 정리가 필요하다. 소유자는 executor 사용을 마치면 `shutdown`/`awaitTermination`으로 서비스 수명을 정리한다.

제공 구현체의 차이도 공개한다. `CoroutineScope.suspendLazy`는 생성 scope가 소유한 공유 `Deferred`를 대기자 timeout만으로 취소하지 않는다. scope가 취소되면 그 작업도 취소된다. `SuspendBlockingLazyImpl`은 초기화된 값을 반환할 때 현재 호출 coroutine이 취소됐는지 확인하고 timeout timer를 만들지 않도록 캐시 빠른 경로 override를 유지한다. 미초기화 timeout 경로는 waiter와 분리된 공유 initializer 작업을 사용한다. `suspendBlockingLazy`의 동기 initializer는 협력 취소 지점이 없으면 timeout 뒤에도 dispatcher thread에서 실행될 수 있다. 이 helper는 initializer를 강제 중단하지 않으며 실제 시간 기준의 정확한 반환도 보장하지 않는다. 사용자 정의 `SuspendLazy`는 기본 `withTimeout { invoke() }`에 따른 호출 coroutine 취소와 구현체의 작업 소유권 규칙을 따른다.

구현 검증에서 단순히 `withTimeout { invoke() }`를 적용하면 동기 initializer가 thread를 점유한 동안 waiter도 timeout 시점에 반환하지 못하는 것이 확인됐다. 따라서 `SuspendBlockingLazyImpl`의 미초기화 timeout 경로는 호출자의 timeout `Job`과 분리된 하나의 공유 initializer `Deferred`를 시작해 기다린다. timeout/호출자 취소는 waiter만 끝내며 동기 initializer는 계속 실행될 수 있고, 성공 값은 이후 호출에서 재사용한다. 실패한 초기화는 기존 `lazy` 계약대로 재시도 가능하다. 초기화된 cache path는 이전처럼 timer 없이 `ensureActive()` 후 반환한다.

이름이 비슷한 기존 API와 계약을 혼합하지 않는다. `Future.awaitUntil(Duration)`은 timeout/호출자 취소 시 미완료 future에 `Future.cancel(false)`를 최선의 노력으로 요청한다. `StructuredTaskScope.joinUntil(Instant)`은 절대 deadline을 받고 `TimeoutException`을 던진다. 이 변경의 `Deferred.awaitUntil(Duration)`과 `Job.joinUntil(Duration)`은 상대 timeout이며 원본을 직접 취소하지 않는다.

## 테스트 설계

- core에서는 여섯 `get`/`join` 성공·기본값·nullable 함수의 성공값과 timeout 동작을 각각 검증한다. 0·음수에서 이미 완료된 값과 미완료 future를 나누고, `Duration.INFINITE`의 JDK 나노초 포화 변환, 일반 업무 실패, 업무 원인이 `TimeoutException`인 실패 future, 이미 취소된 future의 예외, 대기 중 호출자 interrupt, timeout 후 원본 future 생존을 별도로 확인한다. 특히 `join(Duration, defaultValue)`는 실제 대기 timeout과 완료 결과 `null`을 기본값으로 바꾸고 업무 `TimeoutException`은 원인을 풀어 전파하는지 확인한다.
- executor 테스트는 실행된 lambda의 결과와 `List<Future<T>>`/`T` 반환, 입력 순서, 빈 입력, 모든 작업 실패, task 예외, 종료된 executor의 `RejectedExecutionException`, timeout과 interrupt를 검증한다. 부분 제출 후 `RejectedExecutionException`을 발생시키는 executor도 사용해 앞서 제출된 미완료 작업이 취소되는지 확인한다. timeout이나 성공 결과가 확정되기 전에 모든 lambda가 실행된다고 가정하지 않고, 실행된 작업의 호출과 취소된 미완료 future 및 작업의 인터럽트 관찰을 따로 확인한다. 단일 worker를 latch로 점유해 0·음수 timeout의 미시작 작업 취소를 결정적으로 확인하고, 계측용 `AbstractExecutorService`로 두 Callable 래퍼 모두 `Duration.INFINITE`를 `Long.MAX_VALUE`/`NANOSECONDS`로 전달하는지 확인한다. executor를 `finally`에서 종료·대기해 thread를 누수시키지 않는다. 완료 순서는 임의 sleep 대신 latch로 제어한다.
- coroutine 테스트는 `runTest` 가상 시간으로 성공, timeout 예외/`null`, 0·음수에서 블록 미실행, `Duration.INFINITE`에서 timeout 미발생, 업무 실패, 중첩 timeout, 외부 취소 전파를 확인한다. 무한 timeout 검증은 별도 대기자 `Job`을 사용하고, 소유 작업을 완료하거나 `finally`에서 대기자를 취소해 테스트를 끝낸다. `Deferred.awaitUntil()`과 `awaitUntilOrNull()`을 timeout 인자 없이 호출해 기본 5초가 적용되는 경우도 검증한다. `Deferred`, `Job`, scope 기반 lazy의 원본 작업을 독립된 소유 scope에서 만들고 대기자 timeout 뒤 생존을 확인한 다음 소유자를 명시적으로 취소하거나 완료한다. `Job.joinUntil`은 대상 job의 성공·실패·취소 완료가 모두 대기자에게 정상 반환되는지, 대기자 timeout과 호출자 취소는 구분되어 전파되는지 확인한다.
- 사용자 정의 `SuspendLazy`가 `invoke()`만 구현해도 기본 멤버를 호출할 수 있음을 확인하고, 이전 `SuspendLazy` 정의에 대해 컴파일한 테스트 fixture 산출물에는 구현체 bytecode만 포함한다. 테스트는 현재 라이브러리의 인터페이스가 로드됐는지 code source를 확인하고, 구 인터페이스 클래스가 fixture classpath에서 로드되지 않은 상태로 구 구현체의 기본 메서드 디스패치를 검증한다.
- `suspendBlockingLazy`의 초기화된 캐시 값은 활성 호출 coroutine에서 0·음수 timeout에도 빠른 경로로 반환되고, 이미 취소된 호출 coroutine은 캐시 경로에서도 `CancellationException`을 받으며, 빠른 경로는 timeout 타이머를 생성하지 않음을 확인한다. `suspendBlockingLazyIO`의 미초기화 값은 블록 실행 전 timeout 되는 경계를 확인한다. blocking initializer의 취소 검증은 별도 대기자 Job, latch, 실제 dispatcher로 제한된 시간 안에 수행하고, `finally`에서 latch를 해제한 뒤 대기자를 join해 thread를 누수시키지 않는다.
- nullable timeout helper가 반환하는 `null`은 nullable 원본 결과와 구분되지 않음을 예제로 보인다. 실제 실행 정밀도에 의존한 sub-millisecond 보장은 두지 않는다.

## 문서화

- 새 공개 함수와 `SuspendLazy` 멤버에 한국어 KDoc을 추가한다. timeout 예외 형식, 기본값을 선언한 API의 기본 5초, 취소 범위, 작업 소유자와 정리 책임, executor 취소와 작업 종료의 차이 및 소유자의 `shutdown`/`awaitTermination`, nullable 결과와 timeout의 모호성, blocking initializer의 협력 취소 한계를 적는다.
- `bluetape4k/core/README.md`와 `README.ko.md`에는 `CompletableFuture`/executor timeout 예제를 같은 절에 추가한다. `bluetape4k/coroutines/README.md`와 `README.ko.md`에는 `SuspendLazy`, `Deferred`, `Job` helper만 다루는 대응 절을 추가한다. 각 예제에 필요한 import를 명시하고, 예제가 컴파일되거나 해당 README의 기존 예제 검증 방식으로 실행되는지 확인한다. executor 예제에는 `invokeAll`의 `List<Future<T>>` 결과와 `invokeAny`의 `T` 결과를 각각 보이며, nullable 함수 설명에는 반환 `null`과 timeout을 결과만으로 구분할 수 없음을 적는다. 신규 API 예제의 입력은 `5.seconds` 같은 상대 timeout으로 쓰고 변수명 `deadline`은 사용하지 않는다.
- coroutine README는 `Job.joinUntil(5.seconds)`를 상대 timeout으로 보여 주고, 기존 `StructuredTaskScope.joinUntil(Instant)`의 절대 deadline 및 `TimeoutException` 설명은 별도 API 계약으로 보존·구분한다. 같은 이름을 가진 두 API의 입력과 예외 차이를 한·영 README에서 각각 설명한다.
- 기존 설명이 이 API를 가리키면서 예외 타입, `Duration`과 deadline의 구분, 또는 반환 의미가 다르면 함께 바로잡는다.

## 근거

- 현재 구현과 이번 변경의 근거가 되는 파일은 `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/CompletableFutureSupport.kt`, `ExecutorServiceExtensions.kt`, `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/SuspendLazy.kt`, `support/DeferredSupport.kt`, `support/JobSupport.kt`, `support/FutureSupport.kt`이다. 인터페이스 기본 메서드 호환성 판단은 `build.gradle.kts`의 `-jvm-default=enable` 설정에 근거한다.
- JDK의 timed bulk 실행 반환형, 미완료 task 취소, 예외와 executor 수명 계약은 [ExecutorService API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html)를 따른다. timed `Future.get`의 timeout·interrupt·실패 예외와 취소 요청 의미는 [Future API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Future.html)를 따른다.
- `Duration.inWholeNanoseconds`의 범위 포화와 무한값 변환은 [Kotlin Duration API](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/in-whole-nanoseconds.html)에 근거한다. coroutine timeout의 0·음수 처리, dispatcher 시간 추적, 협력 취소는 [withTimeout](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout.html)과 [withTimeoutOrNull](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout-or-null.html), 완료된 대상 Job과 취소된 waiter의 구분은 [Job.join](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/join.html), 캐시 빠른 경로의 호출자 취소 확인은 [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html)에 근거한다.
- 사용자 정의 dispatcher의 `Delay` 시각 정밀도, 비협력적 blocking initializer의 강제 중단, executor가 실제 작업을 멈추는 시점은 이 명세가 보장하지 않는 경계다. 이 제한은 위 API 계약과 현재 구현의 취소·소유권 경계에 맞춰 둔다.

## 완료 기준

1. 모든 열거된 API의 성공·timeout·실패·취소 계약이 회귀 테스트로 확인된다.
2. 실행된 `invokeAll`/`invokeAny` 작업 래퍼는 원래 함수를 호출하고 JDK 결과 타입을 보존하며, 미시작 작업 실행을 보장하지 않는다.
3. `SuspendLazy`의 새 멤버는 인터페이스 기본 메서드로 제공되고, 기본 구현을 쓰는 `SuspendLazyImpl`, 캐시 빠른 경로를 유지하는 `SuspendBlockingLazyImpl`, `invoke()`만 구현한 사용자 구현체 및 이전 ABI로 사전 컴파일한 fixture의 런타임 dispatch 테스트가 통과한다.
4. 한국어 KDoc과 양 언어 README가 구현 및 테스트와 일치한다.
5. core/coroutines 테스트, 관련 Detekt, 공개 ABI 검증, `git diff --check`가 통과한다.
6. 변경사항을 독립 리뷰하고 한국어 PR을 `develop` 대상으로 생성한다. 이 설계는 병합을 승인하지 않는다.
