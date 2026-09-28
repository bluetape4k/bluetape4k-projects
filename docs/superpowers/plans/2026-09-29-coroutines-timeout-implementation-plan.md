# Coroutine 및 동기 API timeout 구현 계획

> **작업자용:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**목표:** `CompletableFuture`, `ExecutorService`, `SuspendLazy`, `Deferred`, `Job`의 `Duration` timeout API 전체를 승인된 계약대로 테스트하고, 구현·KDoc·README를 고친 뒤 공개 ABI를 검증하고 `develop` 대상 PR을 만든다.

**구조:** core의 JDK interop API와 coroutines API를 별도 테스트 경계로 나눈다. 먼저 결정적 회귀 테스트를 작성해 실패를 확인하고, 최소 구현으로 통과시킨다. `SuspendLazy`는 공통 동작을 인터페이스 기본 메서드로 이동한다. `SuspendBlockingLazyImpl`의 미초기화 timeout getter는 waiter와 분리된 공유 initializer `Deferred`를 사용해 동기 작업이 timeout 반환을 막지 않게 하고, 캐시 경로는 timeout timer 없이 활성 상태를 확인한다. 마지막에 문서, 이전 ABI로 컴파일한 Java fixture, JVM 서명 검증을 통합한다.

**기술 스택:** Kotlin 2.4.20, JDK 25, Gradle 9.7.0, JUnit 5, Kluent assertions, kotlinx-coroutines-test, JDK `ExecutorService`/`CompletableFuture`.

---

## 기준 정보와 경계

- **유형/독자:** Type A public API 구현 계획. 구현자와 리뷰어가 승인된 timeout 동작, 검증 순서, PR 경계를 확인한다.
- **승인 기준:** `docs/superpowers/specs/2026-09-28-coroutines-timeout-api-design.md`의 대안 2 및 완료 기준. 명세는 사용자 승인을 받았고, 구현/merge 승인을 대신하지 않는다.
- **설계 리뷰:** `docs/superpowers/reviews/2026-09-28-coroutines-timeout-spec-review.md`의 6개 관점 통합 PASS. 리뷰는 테스트/Detekt/ABI 실행 증거가 아니다.
- **코드 기준점:** branch `feature/coroutines-timeout`, tracked `HEAD=51ec6cd8c146a541e8be7b794a3eb408268bcc8a`. 이 시점에 다섯 timeout production 파일은 사용자가 수정한 tracked dirty 상태다. 변경 내용은 작업 중 보존하고 이 계획/계획 검토 기록에 포함하지 않는다.
- **빌드/테스트 근거:** `build.gradle.kts`의 `-jvm-default=enable`, `gradle/libs.versions.toml`의 `kotlinx-coroutines-test`, 작업 지도에 적은 실제 테스트 파일과 모듈 경로를 확인했다.
- **ABI 근거:** 저장소 스크립트와 Gradle 선언 검색에서 전역 `checkBinaryCompatibility`/`checkProductionAbi` baseline task를 찾지 못했다. 그러므로 계획은 targeted `javap` 서명 확인과 구 ABI fixture 실행으로 좁은 ABI 증거를 수집하며 전역 baseline 통과를 주장하지 않는다.
- **공식 계약 출처:** 명세 §근거의 JDK 25 `ExecutorService`/`Future`, Kotlin `Duration.inWholeNanoseconds`, kotlinx.coroutines `withTimeout`, `withTimeoutOrNull`, `Job.join`, `ensureActive` 공식 문서 URL을 따른다.
- **최종 검증:** core 1,704건과 coroutines 667건 전체 테스트가 통과했다. 두 모듈 Detekt task 재실행은 성공했으며, XML 보고서의 379개 저장소 진단 중 변경 파일에 걸린 2개는 origin/develop에도 있던 `CompletableFutureSupport.kt`의 `TooManyFunctions`와 `futureWithTimeout`의 `MagicNumber`다. targeted `javap` 서명 및 `SuspendLazy` 구 ABI fixture가 통과했다. hosted CI는 PR 생성 후 실행한다. 커스텀 dispatcher 시간 정밀도와 비협력 blocking 작업의 강제 중단은 승인 명세의 보장 범위 밖이다.

각 작업은 테스트 실패를 확인한 뒤 최소 구현을 적용하고 같은 명령으로 재검증한다. 실패 시 현재 실패 테스트와 관련 compile/test task부터 다시 실행하고, 통과 후에만 다음 의존 작업으로 넘어간다. 오류를 고치기 위해 사용자의 변경을 버리는 reset/checkout을 사용하지 않는다.

## 변경 파일 지도

- `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/CompletableFutureSupport.kt`: `Duration` 기반 `get`/`join` 성공·timeout·업무 예외 변환.
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ExecutorServiceExtensions.kt`: 함수형 task 호출, JDK 결과형, timed delegation.
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/CompletableFutureSupportTest.kt`: 여섯 `CompletableFuture` timeout 함수의 계약.
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/ExecutorServiceExtensionsTest.kt`: 신규 `invokeAll`/`invokeAny` 함수형 overload 계약.
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/SuspendLazy.kt`: 기본 구현 및 `SuspendBlockingLazyImpl`의 활성 상태 확인.
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt`: `awaitUntil`/`awaitUntilOrNull` 계약.
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/JobSupport.kt`: `joinUntil` 계약.
- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/SuspendLazyTest.kt`: 신규 timeout 멤버 및 cache 경로.
- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/SuspendLazyBinaryCompatibilityTest.kt`: 구 인터페이스 모양에 맞춰 Java 구현체를 컴파일한 뒤 새 인터페이스에서 기본 메서드를 호출.
- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/support/DeferredSupportTest.kt`: 성공·timeout·원본 작업 보존·실패·호출자 취소.
- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/support/JobSupportTest.kt`: 성공·실패·취소 완료, timeout, 호출자 취소와 대상 job 소유권.
- `bluetape4k/core/README.md`, `bluetape4k/core/README.ko.md`: 동기 timeout API 설명과 예제.
- `bluetape4k/coroutines/README.md`, `bluetape4k/coroutines/README.ko.md`: coroutine timeout 설명과 예제. 기존 `StructuredTaskScope.joinUntil(Instant)` 설명은 상대 `Job.joinUntil(Duration)`과 구분해 유지.

기존 변경이 있는 다섯 production Kotlin 파일은 사용자의 작업 내용을 보존한 채 이 기능 구현으로 완성한다. 구현 계획과 함께 검토된 결과만 PR에 포함한다. 새 의존성이나 새 모듈은 추가하지 않는다.

## 작업 1: `CompletableFuture` timeout 테스트를 먼저 고정한다

**파일:**
- 테스트: `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/CompletableFutureSupportTest.kt`
- 확인 대상: `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/CompletableFutureSupport.kt`

- [x] 완료 future에 대해 `get(Duration)`, `get(Duration, defaultValue)`, `getOrNull(Duration)`, `join(Duration)`, `join(Duration, defaultValue)`, `joinOrNull(Duration)`가 모두 같은 결과를 반환하는 테스트를 추가한다. 값이 nullable인 완료 future도 성공 결과가 `null`일 수 있음을 확인한다.
- [x] 완료/미완료 future를 각각 두고 zero 및 negative Duration의 JDK 동작을 테스트한다. `Duration.INFINITE`는 `get(timeout, unit)`을 기록하고 즉시 완료값을 반환하는 `CompletableFuture` 테스트 double에 전달해 `Long.MAX_VALUE`/`NANOSECONDS`를 확인한다. 무기한 대기 중인 future에 이 값을 실제로 전달하지 않는다.
- [x] 미완료 future에 짧은 양수 Duration을 전달해 예외형 함수는 `TimeoutException`, 기본값 함수는 기본값, nullable 함수는 `null`을 반환하는지 확인한다. timeout 뒤 원본 future가 취소되지 않고 완료될 수 있는지 확인한다.
- [x] `get*`은 일반 업무 실패를 `ExecutionException`으로 유지하고, `join*`은 원인을 unwrap하며, 이미 취소된 future의 취소 의미가 유지되는지 확인한다. `joinOrNull`도 업무 실패를 원인 예외로 전달하고 실제 timed wait의 `TimeoutException`만 `null`로 바꾸는지 검증한다. `join(Duration, defaultValue)`가 업무 원인의 `TimeoutException`을 기본값으로 바꾸지 않는지도 별도로 고정한다.
- [x] 별도 waiter thread를 latch로 대기 상태에 둔 뒤 interrupt해 여섯 overload 모두 `InterruptedException`을 유지하는지 helper 기반으로 검증한다. waiter를 join하고 latch/executor를 `finally`에서 정리한다.
- [x] 기존 임의 `Thread.sleep` timeout 테스트는 `CompletableFuture` 완료/대기 상태와 즉시 만료되는 양수 Duration으로 바꾼다. 동기 JDK timeout의 실제 경계는 `runTest`로 대체하지 않는다.
- [x] 다음 명령으로 새 테스트가 기존 구현에서 실패하는지 확인한다.

```bash
./gradlew :bluetape4k-core:test --tests 'io.bluetape4k.concurrent.CompletableFutureSupportTest'
```

예상 결과: 신규 계약 중 현재 구현과 다른 assertion이 실패하고, 실패는 테스트가 지정한 반환값·예외·취소 상태에 한정된다.

완료된 future와 실제 대기 timeout을 분리하는 대표 검증은 다음처럼 작성한다.

```kotlin
val pending = CompletableFuture<Int>()
assertFailsWith<TimeoutException> { pending.get(1.nanoseconds) }
pending.isCancelled.shouldBeFalse()
pending.complete(42).shouldBeTrue()
pending.get() shouldBeEqualTo 42
```

## 작업 2: `CompletableFuture` 구현을 승인 계약에 맞춘다

**파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/CompletableFutureSupport.kt`

- [x] 여섯 timeout 함수가 JDK timed `get`을 기준으로 동작하게 한다. timeout 예외만 fallback/null로 변환하고 `ExecutionException`, 대기 중 `InterruptedException`, 취소를 업무/호출자 실패로 그대로 전달한다.
- [x] 모든 `join` 경로는 JDK `get`의 `ExecutionException` 원인을 unwrap한다. `joinOrNull`은 실제 timed wait의 `TimeoutException`만 `null`로 바꾸고 원인 예외를 보존한다. fallback overload는 실제 대기에서 발생한 timeout을 먼저 판별하고 나서 업무 예외 원인을 unwrap해, 업무 원인이 `TimeoutException`이어도 fallback과 혼동하지 않는다.
- [x] `Duration.inWholeNanoseconds`를 사용하고 원본 future를 취소하지 않는다. 실패하는 테스트 assertion은 구현만으로 통과시키고 불필요한 helper나 dependency는 추가하지 않는다.
- [x] 작업 1의 명령을 다시 실행해 `CompletableFutureSupportTest` 전체가 통과하는지 확인한다.

구현 의미는 아래처럼 실제 timed wait와 업무 실패 경계를 분리한다.

```kotlin
try {
    return get(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
} catch (e: TimeoutException) {
    return defaultValue
} catch (e: ExecutionException) {
    throw e.cause ?: e
}
```

위 코드는 `join(Duration, defaultValue)`의 예외 순서를 나타낸다. 예외형 overload는 `TimeoutException`을 잡지 않고, nullable overload만 실제 timed wait timeout을 `null`로 바꾼다.

## 작업 3: `ExecutorService` 함수형 overload의 JDK 계약을 테스트한다

**파일:**
- 신규 테스트: `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/ExecutorServiceExtensionsTest.kt`
- 확인 대상: `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ExecutorServiceExtensions.kt`

- [x] `invokeAll(tasks, timeout)`이 각 시작된 람다를 호출하고 입력 순서대로 `List<Future<T>>`를 반환하는 테스트를 추가한다. 각 future 결과와 호출 횟수를 확인한다.
- [x] `invokeAll` timed timeout은 실행이 끝나지 않은 future를 취소하고 완료된 future의 결과를 보존하며, 미시작 작업의 실행을 가정하지 않는다.
- [x] `invokeAll`의 빈 입력은 빈 목록을 반환하고 작업을 제출하지 않는지, 작업 예외는 대응 future에서 `ExecutionException`으로 관찰되는지 확인한다. zero/negative timeout은 실제 single-worker executor를 latch로 점유해 다음 작업이 시작되지 않고 미완료 future가 취소되는지 확인한다. 완료된 작업이 있으면 그 결과가 보존되는지도 확인한다.
- [x] `invokeAll`을 수행하는 waiter thread를 latch로 기다리게 한 뒤 interrupt해 `InterruptedException`, 제출된 미완료 future의 취소, 실행 중 task의 interrupt 관찰을 확인한다. 부분 제출 뒤 두 번째 작업을 거부하는 executor는 첫 작업이 시작된 뒤 `RejectedExecutionException`을 던지게 하고, 첫 future 취소와 task interrupt를 검증한다. 종료된 executor의 거부도 별도로 확인한다.
- [x] `invokeAll` 및 `invokeAny`의 `Duration.INFINITE` 전달은 timed overload를 기록하고 즉시 반환하는 `AbstractExecutorService` 테스트 double로 두 wrapper 모두 `Long.MAX_VALUE`/`TimeUnit.NANOSECONDS`를 전달하는지 확인한다. 실제 timed JDK overload에 무한 대기를 걸지 않는다.
- [x] `invokeAny(tasks, timeout)`의 첫 성공값 `T`, 모든 작업 실패 시 `ExecutionException`, 빈 입력의 `IllegalArgumentException`, 미완료 작업의 `TimeoutException`을 확인한다. timeout 상황은 실제 single-worker executor를 latch로 점유해 미시작 작업의 실행을 가정하지 않고 검증한다.
- [x] `invokeAny`의 빈 입력, 대기 중 interrupt, 종료 executor rejection, 정상/timeout/interrupt/rejection 종료 때 미완료 작업의 취소 요청을 확인한다. 부분 제출 뒤 거부된 경우 시작 작업의 취소 요청도 확인한다.
- [x] 동시성 경계는 `CountDownLatch`와 bounded timeout으로 제어하고 모든 executor는 `finally`에서 종료한다. 미시작 lambda의 실행을 가정하지 않는다.
- [x] 테스트를 먼저 실행해 현 래퍼의 `Unit` 반환과 callable이 함수를 호출하지 않는 결함을 드러낸다.

핵심 반환형/호출 검증은 다음 계약을 그대로 표현한다.

```kotlin
val invoked = AtomicInteger()
val results: List<Future<Int>> = executor.invokeAll(
    listOf({ invoked.incrementAndGet(); 10 }, { invoked.incrementAndGet(); 20 }),
    1.seconds,
)
results.map { it.get() } shouldBeEqualTo listOf(10, 20)
invoked.get() shouldBeEqualTo 2
```

실행: `./gradlew :bluetape4k-core:test --tests 'io.bluetape4k.concurrent.ExecutorServiceExtensionsTest'`.
예상 결과: 현재 `Unit` 서명 때문에 반환형 assertion이 컴파일되지 않거나, callable 동작 assertion이 실패한다.

## 작업 4: `ExecutorService` timed wrapper를 최소 수정한다

**파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ExecutorServiceExtensions.kt`

- [x] `invokeAll`의 선언 반환형을 `List<Future<T>>`로 바꾸고 변환한 각 `Callable`이 원래 람다를 호출하게 한다.
- [x] `invokeAny`의 선언 반환형을 `T`로 바꾸고 각 `Callable`이 원래 람다를 호출하게 한다.
- [x] `Duration.inWholeNanoseconds`와 `TimeUnit.NANOSECONDS`를 JDK timed overload에 그대로 전달한다. 시간 초과·interrupt·rejection 정책은 JDK 구현에 위임한다.
- [x] 작업 3의 명령을 다시 실행해 전체 결과·예외·취소 assertion이 통과하는지 확인한다.

구현 형태:

```kotlin
fun <T> ExecutorService.invokeAll(tasks: Collection<() -> T>, timeout: Duration): List<Future<T>> =
    invokeAll(tasks.map { task -> Callable { task() } }, timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)

fun <T> ExecutorService.invokeAny(tasks: Collection<() -> T>, timeout: Duration): T =
    invokeAny(tasks.map { task -> Callable { task() } }, timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
```

## 작업 5: `SuspendLazy` timeout 기본 구현과 호환성 테스트를 고정한다

**파일:**
- 테스트: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/SuspendLazyTest.kt`
- 호환성 테스트: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/SuspendLazyBinaryCompatibilityTest.kt`
- 확인 대상: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/SuspendLazy.kt`

- [x] 현재 소스에서 `invoke()`만 구현한 `object : SuspendLazy<Int> { override suspend fun invoke() = 42 }`가 두 timeout 기본 멤버를 호출하는지 먼저 검증한다. `suspendLazy`의 기본/timeout 성공, `getUntil`의 `TimeoutCancellationException`, `getUntilOrNull`의 `null`, 업무 실패와 바깥 취소 전파를 `runTest`와 `testScheduler`로 검증한다. zero/negative timeout에서 initializer가 실행되지 않는지, `Duration.INFINITE` 대기가 initializer 완료 또는 waiter 취소로 끝나는지도 확인한다.
- [x] 생성 scope가 소유한 공유 `Deferred`에 첫 waiter timeout을 적용한 뒤 initializer가 계속 실행되는지, 두 번째 waiter가 완료 값을 받는지 검증한다.
- [x] `suspendBlockingLazy`를 먼저 초기화한 뒤 같은 활성 호출자에서 zero/negative timeout이어도 cache hit가 즉시 값을 반환하는지, initializer가 한 번만 호출되는지 검증한다.
- [x] 이미 취소된 Job을 context로 둔 `Continuation`에서 초기화된 cache를 읽어도 `ensureActive()`가 취소를 보존하는지 테스트한다. 추가로 test-only `CoroutineDispatcher(), Delay` 구현의 `invokeOnTimeout` 호출 수를 기록하고, 초기화된 cache를 활성 coroutine에서 양수 timeout으로 읽을 때 scheduling 횟수가 0임을 확인한다. 이 검증은 cache hit이 실제로 타이머 생성을 건너뛰는지 관찰한다.
- [x] 미초기화 `suspendBlockingLazyIO` initializer를 latch로 막고 timeout된 waiter가 종료되는 시점을 검증한다. 테스트는 initializer가 취소에 협력하지 않을 때 실제 블록은 계속 실행될 수 있음을 확인하고, `finally`에서 latch를 해제한 뒤 initializer와 waiter를 join한다.
- [x] nested `getUntil` timeout에서 안쪽 timeout과 바깥 caller cancellation을 구분한다. 모든 미완료 waiter는 테스트가 소유한 Job으로 취소해 종료한다.
- [x] 이전 공개 계약의 Java interface stub은 `io.bluetape4k.coroutines.SuspendLazy<T>`로, 추상 메서드는 `Object invoke(Continuation<? super T>)` 하나만 선언한다. `ToolProvider.getSystemJavaCompiler()`로 임시 소스의 아래 세 파일을 `-proc:none -classpath System.getProperty("java.class.path") -d <temp>/classes` 옵션과 함께 컴파일한다: `kotlin.coroutines.Continuation<T>` 빈 interface, `SuspendLazy<T>` 구 stub, 그리고 해당 `invoke(Continuation<? super Integer>)`만 구현해 `Integer.valueOf(42)`를 반환하는 `LegacySuspendLazy`. 컴파일 후 `<temp>/classes/kotlin/coroutines/Continuation.class`와 `<temp>/classes/io/bluetape4k/coroutines/SuspendLazy.class`를 삭제해 구 타입 bytecode를 fixture 출력에서 제거한다. 런타임 `URLClassLoader`는 현재 테스트 classloader를 parent로 두고, `Class.forName("io.bluetape4k.coroutines.SuspendLazy", false, fixtureLoader)`가 현재 `SuspendLazy::class.java`와 동일한지 및 `SuspendLazy::class.java.protectionDomain.codeSource.location`이 fixture 디렉터리 밖인지 확인한다. fixture 인스턴스를 현재 `SuspendLazy<Int>`로 cast한 뒤 `runBlocking`에서 `getUntil`과 `getUntilOrNull` 기본 메서드를 호출한다. 임시 디렉터리는 테스트 종료 시 제거한다.
- [x] 테스트를 먼저 실행해 아직 기본 메서드가 없는 API/캐시 취소 동작/호환성 호출의 차이를 확인한다.

기본 timeout 경계는 가상 시간을 사용한다.

```kotlin
@Test
fun `getUntilOrNull returns null at timeout`() = runTest {
    val lazyValue = suspendLazy { delay(1.seconds); 42 }
    lazyValue.getUntilOrNull(100.milliseconds) shouldBeEqualTo null
}
```

일반 테스트 실행: `./gradlew :bluetape4k-coroutines:test --tests 'io.bluetape4k.coroutines.SuspendLazyTest'`.
호환성 실행: `./gradlew :bluetape4k-coroutines:test --tests 'io.bluetape4k.coroutines.SuspendLazyBinaryCompatibilityTest'`.

## 작업 6: `SuspendLazy` 기본 메서드를 구현한다

**파일:** `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/SuspendLazy.kt`

- [x] `SuspendLazy.getUntil(Duration)`에 `withTimeout(timeout) { invoke() }` 기본 구현을 둔다.
- [x] `SuspendLazy.getUntilOrNull(Duration)`에 `withTimeoutOrNull(timeout) { invoke() }` 기본 구현을 둔다.
- [x] `SuspendLazyImpl`의 두 중복 override를 제거해 인터페이스 기본 구현을 사용한다.
- [x] `SuspendBlockingLazyImpl`은 cache hit에서 `currentCoroutineContext().ensureActive()` 후 timer 없이 값을 돌려준다. 미초기화 상태는 호출자 Job과 분리된 공유 initializer `Deferred`를 시작해 `withTimeout`/`withTimeoutOrNull`로 기다린다. timeout 후 blocking initializer가 계속 실행되고, 다음 호출에서 완료값을 재사용하는지 latch로 검증하며 실패 후 재시도도 확인한다.
- [x] 작업 5의 두 명령을 다시 실행해 API/취소/cache/사전 컴파일 fixture 테스트를 통과시킨다.

인터페이스 기본 구현은 다음 형태로 유지한다.

```kotlin
public suspend fun getUntil(timeout: Duration): T = withTimeout(timeout) { invoke() }
public suspend fun getUntilOrNull(timeout: Duration): T? = withTimeoutOrNull(timeout) { invoke() }
```

## 작업 7: `Deferred.awaitUntil` 계약을 테스트하고 고정한다

**파일:**
- 테스트: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/support/DeferredSupportTest.kt`
- 구현: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt`

- [x] 완료 deferred의 명시적 timeout overload가 값을 반환하는지 테스트한다. 미완료 deferred에 인자를 생략해 `awaitUntil()`/`awaitUntilOrNull()`을 호출하고 testScheduler를 5초 진행해 기본 timeout을 검증한다.
- [x] 미완료 deferred에서 `awaitUntil`은 `TimeoutCancellationException`, `awaitUntilOrNull`은 `null`을 반환하는지 검증한다. timeout 후 원본 deferred는 취소되지 않았고 이후 값을 완료할 수 있어야 한다.
- [x] 완료된 실패 deferred의 원래 예외와 취소된 호출자 context의 cancellation이 그대로 전파되는지 검증한다. nullable 결과 `null`과 timeout을 API상 구별하지 않는다는 주석 계약도 KDoc에서 설명한다.
- [x] `Duration.INFINITE` 대기는 원본 값 완료 또는 waiter Job 취소로 끝낸다. nested timeout은 안쪽/바깥 timeout의 취소 경계를 구분한다.
- [x] 가상 시간 `runTest`를 사용하고 기존 `Future.awaitUntil` 테스트와 혼동하지 않는다.
- [x] 먼저 `./gradlew :bluetape4k-coroutines:test --tests 'io.bluetape4k.coroutines.support.DeferredSupportTest'`를 실행해 테스트 실패를 확인한다.

timeout 뒤 원본 deferred 보존 검증:

```kotlin
val deferred = CompletableDeferred<Int>()
assertFailsWith<TimeoutCancellationException> { deferred.awaitUntil(100.milliseconds) }
deferred.isCancelled.shouldBeFalse()
deferred.complete(42).shouldBeTrue()
deferred.await() shouldBeEqualTo 42
```

## 작업 8: `Deferred.awaitUntil` 구현과 KDoc을 정합시킨다

**파일:** `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt`

- [x] `awaitUntil(timeout: Duration = 5.seconds)`에서 waiter에 `withTimeout(timeout) { await() }`를 적용한다.
- [x] `awaitUntilOrNull(timeout: Duration = 5.seconds)`에서 waiter에 `withTimeoutOrNull(timeout) { await() }`를 적용한다.
- [x] 두 함수 KDoc에 기본 5초, 실제 null 결과와 timeout의 모호성, 원본 deferred 직접 취소 없음, 실패/외부 취소 전파를 한국어로 기록한다.
- [x] 작업 7의 명령을 다시 실행해 `DeferredSupportTest`가 통과하는지 확인한다.

## 작업 9: `Job.joinUntil` 대상과 waiter 소유권을 테스트하고 구현한다

**파일:**
- 테스트: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/support/JobSupportTest.kt`
- 구현: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/JobSupport.kt`

- [x] 성공·실패·취소 완료, timeout, caller cancellation, zero/negative, `Duration.INFINITE` 등 모든 미완료 대상 job을 waiter/test scope와 분리된 `CoroutineScope(SupervisorJob() + testDispatcher)` owner 아래 생성한다. 각 경우에 대상 완료 상태, waiter 예외/정상 반환, 대상 job 생존을 확인하고 owner와 scope는 `finally`에서 정리한다. 실패/취소 대상이 waiter scope까지 취소하지 않도록 이 독립 소유권을 유지한다.
- [x] 미완료 대상에 timeout을 적용해 waiter는 `TimeoutCancellationException`을 받고 대상은 취소되지 않고 활성 상태임을 확인한다.
- [x] 부모/caller cancellation은 waiter에 전파되고 대상 job은 직접 취소되지 않는지 확인한다. `runTest` scheduler로 시간 경계를 제어한다.
- [x] zero/negative timeout의 즉시 취소와 `Duration.INFINITE`의 무제한 대기를 testScheduler로 검증한다. 무제한 waiter는 대상 완료 또는 테스트가 소유한 waiter Job 취소로 끝낸다.
- [x] 테스트를 먼저 실행해 `Job.join` 정상 완료·부모 취소 계약과 timeout 구현을 검증하고, 실패 이유가 계약 assertion에 한정됨을 확인한다.
- [x] 구현은 `withTimeout(timeout) { join() }`를 유지하되 한국어 KDoc에 Duration 상대 제한, 대상 완료 시 정상 반환, waiter만 취소됨을 기록한다.
- [x] 다음 명령으로 통과시킨다.

```bash
./gradlew :bluetape4k-coroutines:test --tests 'io.bluetape4k.coroutines.support.JobSupportTest'
```

## 작업 10: 한국어 KDoc과 한·영 README를 API 계약에 맞춘다

**파일:**
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/CompletableFutureSupport.kt`
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ExecutorServiceExtensions.kt`
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/SuspendLazy.kt`
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt`
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/JobSupport.kt`
- `bluetape4k/core/README.md`, `bluetape4k/core/README.ko.md`
- `bluetape4k/coroutines/README.md`, `bluetape4k/coroutines/README.ko.md`

- [x] 각 public timeout 함수에 성공값, timeout 형태/기본값, 업무 실패·interrupt·외부 취소, 원본 작업 소유권 경계를 설명한다. executor lambda 변환은 O(n)이며 그 변환 비용을 포함한 호출에 엄격한 wall-clock 제한 시간을 약속하지 않는다는 점도 KDoc/README에 적는다.
- [x] core README 두 언어에 `get`/`join` 예외형과 fallback/null형, `invokeAll`/`invokeAny`의 값 반환 예제를 추가한다.
- [x] coroutines README 두 언어에 `SuspendLazy`, `Deferred`, `Job` 사용 예제와 기본값/timeout 취소 동작을 추가한다.
- [x] 기존 `Future.awaitUntil(Duration)`은 timeout/호출자 취소에서 미완료 future에 `cancel(false)`를 최선 노력으로 요청하지만, 신규 `Deferred.awaitUntil` 및 `Job.joinUntil`은 waiter만 제한하고 원본 작업을 직접 취소하지 않는 차이를 KDoc과 대응 README 문단에 적는다.
- [x] 기존 `StructuredTaskScope.joinUntil(Instant)`의 절대 deadline 설명을 유지하고, `Job.joinUntil(Duration)`의 상대 timeout과 다른 API임을 두 문서에 분명하게 적는다.
- [x] README 코드 예제와 같은 Kotlin 호출/타입을 모듈 test source의 컴파일 가능한 예제 테스트에 반영해 `compileTestKotlin`으로 검증한다. README와 fixture 코드의 공개 함수명·타입을 대조하고 한·영 문서가 같은 계약을 설명하는지 확인한다.

문서 예제의 타입 계약:

```kotlin
val values: List<Future<Int>> = executor.invokeAll(tasks, 1.seconds)
val firstValue: Int = executor.invokeAny(tasks, 1.seconds)
val result: Int? = deferred.awaitUntilOrNull(250.milliseconds)
```

검증: 예제 호출을 각 모듈 test source에 반영하고 `./gradlew :bluetape4k-core:compileTestKotlin :bluetape4k-coroutines:compileTestKotlin`을 실행한다. 이 compile 검증은 README 원문 parser를 대신하지 않으므로 예제와 fixture 간 타입·이름 대조도 함께 수행한다.

## 작업 11: 테스트, Detekt, ABI, diff를 순차 검증한다

**검증 명령:**

- [x] core timeout 테스트:

```bash
./gradlew :bluetape4k-core:test --tests 'io.bluetape4k.concurrent.CompletableFutureSupportTest' --tests 'io.bluetape4k.concurrent.ExecutorServiceExtensionsTest'
```

- [x] coroutines timeout 테스트:

```bash
./gradlew :bluetape4k-coroutines:test --tests 'io.bluetape4k.coroutines.SuspendLazyTest' --tests 'io.bluetape4k.coroutines.SuspendLazyBinaryCompatibilityTest' --tests 'io.bluetape4k.coroutines.support.DeferredSupportTest' --tests 'io.bluetape4k.coroutines.support.JobSupportTest'
```

- [x] 두 모듈 전체 테스트 및 정적 분석을 각 실행이 끝난 뒤 순차 수행한다.

```bash
./gradlew :bluetape4k-core:test
./gradlew :bluetape4k-coroutines:test
./gradlew :bluetape4k-core:detekt :bluetape4k-coroutines:detekt
```

- [x] 저장소에 전역 `checkBinaryCompatibility`/`checkProductionAbi` baseline task가 없으므로, production compile 후 아래 재현 명령으로 대상 class를 검사한다. `javap -p -s` descriptor에서 CompletableFuture facade timeout 함수가 `(CompletableFuture, long[, Object])Object`, executor `invokeAll`이 `(ExecutorService, Collection, long)List`, `invokeAny`가 `(ExecutorService, Collection, long)Object`, `SuspendLazy` timeout 멤버가 `(long, Continuation)Object`, `DeferredSupportKt`/`JobSupportKt` 함수가 receiver와 `long`, `Continuation`을 받고 `Object`를 반환하는지 확인한다. Kotlin `Duration` value class 때문에 메서드명이 mangled될 수 있으므로 descriptor와 공개/기본 메서드 여부를 확인하고 source 이름만 고정하지 않는다. `javap -p -c`에서 interface default body를 확인하고 작업 5의 구 ABI runtime fixture 결과를 별도 증거로 남긴다.

```bash
./gradlew :bluetape4k-core:compileKotlin :bluetape4k-coroutines:compileKotlin
javap -p -s -classpath bluetape4k/core/build/classes/kotlin/main io.bluetape4k.concurrent.CompletableFutureSupportKt io.bluetape4k.concurrent.ExecutorServiceExtensionsKt
javap -p -s -c -classpath bluetape4k/coroutines/build/classes/kotlin/main:bluetape4k/core/build/classes/kotlin/main io.bluetape4k.coroutines.SuspendLazy io.bluetape4k.coroutines.support.DeferredSupportKt io.bluetape4k.coroutines.support.JobSupportKt
```
- [x] `git diff --check`를 실행한다. 허용되지 않는 새 task 이름이나 기존 `Instant` deadline API 변경이 없는지 최종 diff에서 확인한다.

저장소 전체 baseline을 지원하지 않는 ABI task가 없는 경우, targeted `javap`와 실제 구 ABI fixture를 ABI 증거로 구분해서 기록한다. 둘 중 하나를 전역 binary compatibility plugin의 통과로 표현하지 않는다.


**실행 근거:** core 1,704/1,704, coroutines 667/667 테스트 성공, 실패·오류·skip 0. 두 Detekt task는 `--rerun-tasks`로 성공했고, XML의 379개 진단 중 변경 경로 진단 2개는 `origin/develop`에도 존재하는 파일 기준 `TooManyFunctions`와 `futureWithTimeout`의 `MagicNumber`다. `javap -p -s`에서 모든 신규 descriptor와 `SuspendLazy` default method를 확인했고, 구 ABI Java fixture 테스트도 통과했다. 저장소 전역 ABI baseline task는 없다.

## 작업 12: 변경 검토, Lore commit, PR 생성과 read-back

- [x] `git status --short`, `git diff --stat`, `git diff --check`, `git diff`로 전체 변경을 검토한다. 다섯 기존 user-edited production file의 의도하지 않은 변경을 제거하지 않고, 리뷰를 위해 staged file 목록을 따로 확인한다.
- [x] 독립 코드 리뷰를 요청했으나 현 시점에 판정이 없어 워크플로우의 inline fallback review를 수행했다. API/timeout/취소/테스트/ABI/문서 검토와 증거는 `docs/superpowers/reviews/2026-09-29-coroutines-timeout-code-review.md`에 기록했다. 이 결과는 독립 attestation을 대신한다고 주장하지 않는다.
- [ ] 검증된 코드와 문서만 stage하고, 한국어 intent line 및 Lore trailer(`Tested`, `Not-tested`, `Confidence`, `Scope-risk`, `Directive`)를 포함한 commit을 만든다. 구현 계획과 검토된 명세·코드·테스트를 함께 commit한다.
- [ ] `git push -u origin feature/coroutines-timeout`으로 승인된 head를 게시한다.
- [ ] 한국어 제목/본문과 `develop` base를 지정해 `bluetape4k/bluetape4k-projects` PR을 생성한다. PR 설명에 API별 변경, JDK 결과 타입 보존, 테스트·Detekt·ABI 증거와 알려진 제한을 기록한다.
- [ ] 생성 직후 `gh pr view`/`gh api`로 base/head SHA, 파일 목록, 본문, URL을 다시 읽어 실제 생성된 PR이 검증한 commit을 가리키는지 확인한다. 이 계획은 PR 생성까지만 승인하며 merge는 포함하지 않는다.

## 자체 점검

- [x] 명세의 다섯 API 영역 전체와 기본/nullable overload, timeout, 업무 실패, interrupt/cancellation, 원본 작업 소유권을 작업 1–9에 연결했다.
- [x] 사용자 승인 설계인 JDK 반환 타입 및 `SuspendLazy` 기본 메서드/구 ABI fixture를 작업 3–6 및 11에 연결했다.
- [x] 상대 `Duration`과 절대 `Instant` 구분을 작업 10–11에 포함했다.
- [x] 검증 전에 PR을 만들지 않으며, PR 생성과 merge를 분리했다.
- [x] 구현 범위에서 새 dependency를 추가하지 않는다.
- [x] 코드 변경 단계에는 실제 경로, 명령, 예상 실패/통과, 대표 구현/테스트 코드를 기재했다.
- [x] 사용자의 다섯 dirty production 파일은 계획 기록 commit과 독립 검토 과정에서 그대로 보존한다.
- [x] 각 작업의 실패 후 재실행 지점, 변경 경계, PR 생성과 별도인 merge 경계를 명시했다.
