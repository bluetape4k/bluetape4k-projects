# 배운 점 — test coverage: kafka, resilience4j, cassandra (2026-05-15)

**관련 PR**: #452
**영향 모듈**: `infra/kafka`, `infra/resilience4j`, `spring-boot/cassandra`

## L1: MockK relaxed mock + generic Flux<T> = UninitializedPropertyAccessException

### 문제

`mockk<ReactiveCqlOperations>(relaxed = true)`로 생성된 mock에서 generic `Flux<T>`를 반환하는
method(예: `queryForRows`, `query`)를 호출하면, MockK proxy가 Reactor의 `Publisher.subscribe()` contract를
올바르게 구현하지 않아 `onSubscribe()`가 호출되지 않는다.

이후 `.asFlow().toList()`를 호출하면 `ReactiveSubscriber.subscription` lateinit var가
초기화되지 않은 상태로 `finally { subscriber.cancel() }`이 실행되어
`UninitializedPropertyAccessException`이 발생한다.

```
kotlin.UninitializedPropertyAccessException: lateinit property subscription has not been initialized
    at kotlinx.coroutines.reactive.ReactiveSubscriber.cancel(ReactiveFlow.kt:147)
```

### 교훈

**규칙**: generic `Flux<T>`를 반환하는 메서드를 relaxed mock으로 테스트할 때는
`.toList()`로 수집하지 말고 `shouldNotBeNull()`로 Flow 참조만 확인한다.

함수 본문은 여전히 실행되므로(Kover coverage 측정) coverage 목적은 달성된다.

```kotlin
// ❌ UninitializedPropertyAccessException 발생
val rows = mockOps.queryForRowsFlow(statement).toList()

// ✅ 정상 동작 — Flow 참조만 확인
val rows = mockOps.queryForRowsFlow(statement)
rows.shouldNotBeNull()
```

**예외**: `Flux.just(mockRow)` 또는 `Flux.just("mapped")`처럼 실제 Flux를 명시적으로
stub한 경우에는 `.toList()` 사용 가능.

---

## L2: runBlocking은 JUnit5 테스트 메서드 발견을 막는다

### 문제

```kotlin
@Test
fun `my test`() = runBlocking { expr }
```

`runBlocking { expr }`의 return type이 `expr`의 type(`Boolean`, `Unit` 아님)이 되어
JUnit5가 이 method를 발견하지 못한다(`void` return이 아니기 때문).

결과적으로 test는 BUILD SUCCESS로 통과되지만 실제로는 실행되지 않아
coverage 계산에서는 해당 code가 실행되지 않은 것으로 표시된다.

### 교훈

**규칙**: suspend 테스트에는 반드시 `runSuspendIO { }` 또는 `runTest { }` 사용.

`runSuspendIO`는 `inline fun runSuspendIO(block): Unit`으로 선언되어 있어
test method가 항상 `Unit`(= `void`)을 반환하도록 보장한다.

```kotlin
// ❌ JUnit5가 테스트를 발견하지 못함
@Test
fun `my test`() = runBlocking { someBoolean() }

// ✅ 정상 동작
@Test
fun `my test`() = runSuspendIO { someBoolean().shouldBeTrue() }
```

---

## L3: runBlocking은 test helper 함수에서도 피해야 한다

### 문제

`@RepeatedTest` method가 non-suspend helper `measureSendRecords`를 호출하고,
helper 내부에서 `runBlocking(Dispatchers.IO) { block() }`로 bridging하는 pattern은
project rule 위반이다.

```kotlin
// ❌ helper가 runBlocking 사용
private fun measureSendRecords(block: suspend CoroutineScope.() -> Unit) {
    runBlocking(Dispatchers.IO) { block() }
}

@RepeatedTest(3)
fun `send many messages`() {
    measureSendRecords { ... }
}
```

### 교훈

**규칙**: helper를 `suspend fun`으로 선언하고, 호출부 test method에서 `runSuspendIO`로 진입한다.

```kotlin
// ✅ suspend helper + runSuspendIO 진입
private suspend fun measureSendRecords(block: suspend CoroutineScope.() -> Unit) {
    coroutineScope { block() }
}

@RepeatedTest(3)
fun `send many messages`() = runSuspendIO {
    measureSendRecords { ... }
}
```

---

## L4: Kover XML 리포트를 integration test 없이 생성하는 방법

### 문제

`./gradlew koverXmlReport`를 실행하면 test task가 함께 실행되어
Docker/Cassandra가 필요한 integration test가 실패한다.

### 교훈

기존 `.ic` binary coverage data에서 report만 생성하려면 `-x test` option을 사용한다.

```bash
./gradlew :bluetape4k-spring-boot-cassandra:koverXmlReport -x test --no-configuration-cache
```

이전 test 실행 결과의 `.ic` file이 있어야 동작한다.

---

## L5: Gradle binary test result 손상 복구

### 문제

Test 실패 후 재실행 시 `java.io.EOFException`이 발생했다.
```
java.io.EOFException at Test.getPreviousFailedTestClasses()
```

Gradle이 이전 실패 test class를 Kryo serialized binary store에서 읽는데,
비정상 종료로 인해 file이 손상된 경우 발생한다.

### 교훈

**복구 방법**: `build/test-results/` directory 삭제 후 재실행한다.

```bash
rm -rf spring-boot/cassandra/build/test-results/
./gradlew :bluetape4k-spring-boot-cassandra:test --no-configuration-cache
```

---

## L6: Code review scope — 기존 file의 pre-existing 문제는 별도 PR로 분리

### 문제

Code review에서 `SuspendDecoratorsTest.kt`의 `runCatching { decorated() }` pattern이
CRITICAL로 지적되었으나, 이 file은 이번 PR에서 수정하지 않은 기존 file이었다.

### 교훈

Reviewer를 호출할 때 "이번 PR에서 수정된 file만 review" 범위를 명확히 지정한다.
기존 code의 pre-existing 문제는 별도 tech-debt issue로 추적한다.

`SuspendDecoratorsTest.kt`의 `runCatching` 문제는 follow-up issue로 추적해야 한다.
- 약 20개 test method에서 `runCatching { decorated() }` pattern 사용
- `decorated`가 `suspend () -> T` type이어서 `CancellationException` swallow 위험
