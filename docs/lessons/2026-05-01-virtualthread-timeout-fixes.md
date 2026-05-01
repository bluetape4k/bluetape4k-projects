# Lessons Learned — virtualthread timeout 버그 수정 (2026-05-01)

**관련 PR**: #272  
**수정 이슈**: #266, #268, #269  
**영향 모듈**: `virtualthread/api`, `virtualthread/jdk21`, `virtualthread/jdk25`, `testing/junit5`, `utils/workflow`

---

## L1: interface default impl이 deadline을 무시할 수 있다

### 문제

```kotlin
// 인터페이스 기본 구현
fun joinUntil(deadline: Instant): StructuredTaskScopeAll = join()
```

컴파일/실행 모두 정상이지만, deadline 인자가 완전히 무시된다.
구현체가 재정의하지 않으면 언제나 무한 대기 — 버그가 아니라 정상처럼 보인다.

### 교훈

- **deadline을 받는 메서드는 abstract 강제** — 기본 구현 제공 금지.
- 인터페이스에 `joinUntil` 같은 타임아웃 메서드 추가 시 **반드시 timeout 테스트** 추가.
- `= join()` 패턴은 "편의 기본값"처럼 보이지만 의미론적 버그다.

---

## L2: `runCatching` 안에서 던지는 예외는 outer `try-catch`에 도달하지 않는다

### 문제

```kotlin
return try {
    runCatching {
        scope.joinUntil(deadline)  // TimeoutException 던짐
        ...
    }.getOrElse { ... }           // TimeoutException이 여기서 삼켜짐
} catch (e: TimeoutException) {   // ← 절대 도달하지 않는 dead code
    WorkReport.Cancelled(context)
}
```

`TimeoutException`이 `runCatching`에 잡혀 `getOrElse`로 전달된다.
outer `catch (TimeoutException)`은 dead code가 되고, 잘못된 `WorkReport.Failure`가 반환된다.

### 수정

```kotlin
}.getOrElse { throwable ->
    if (throwable is TimeoutException) throw throwable  // outer catch로 전파
    // ... 기존 실패 처리
}
```

### 교훈

- `runCatching`과 `try-catch`를 **혼용할 때 예외 흐름을 명시적으로 추적**해야 한다.
- `runCatching` 블록 내부에서 특정 예외를 외부로 전파하려면 `getOrElse`에서 rethrow 필요.
- 테스트 없이는 이 버그가 발견되지 않는다 — **모든 `catch` 분기에 테스트 필수**.

---

## L3: JDK preview API는 stable 전환 시 바이너리 불호환이 발생한다

### 문제

```kotlin
// JDK21 preview API
ScopedValue.where(key, value).call { block() }
// → ScopedValue.Carrier.call(java.util.concurrent.Callable)
```

JDK24+에서 `call(java.util.concurrent.Callable)` → `call(ScopedValue.CallableOp)`으로 변경.
컴파일은 성공하지만 JDK25 런타임에서 `NoSuchMethodError` 발생.

### 수정

```kotlin
// run(Runnable)은 JDK21/25 모두 안정적
var captured: Result<R>? = null
ScopedValue.where(key, value).run { captured = runCatching { block() } }
return checkNotNull(captured) { "..." }.getOrThrow()
```

### 교훈

- **JDK 버전 경계를 가로지르는 코드는 stable API만 사용**.
- `--enable-preview` 플래그로 컴파일한 코드는 major 버전 업 시 바이너리 불호환 가능성 있음.
- preview API 사용 부분은 별도 모듈(`jdk21`, `jdk25`)로 격리하고, 공용 `api` 모듈은 stable API만.
- cross-JDK 테스트(`@EnabledForJreRange(min = JRE.JAVA_21)`)로 두 버전 모두 검증 필수.

---

## L4: 중복 구현은 버그 수정 시 N배 비용

### 문제

```kotlin
// Jdk25AllScope.joinUntil  — 25줄
// Jdk25AnyScope.joinUntil  — 25줄 (copy-paste)
// Jdk25SupervisedScope.joinUntil — 25줄 (copy-paste)
```

`toMillis()` 정밀도 버그 하나를 고치거나 race condition 주석을 추가하려면 3곳 모두 수정.

### 수정

```kotlin
companion object {
    internal fun interruptJoinUntil(
        deadline: Instant,
        threadName: String,
        joinAction: () -> Unit,  // InterruptedException을 전파해야 함
    ) { ... }
}
// 각 scope: 1줄
override fun joinUntil(deadline: Instant): StructuredTaskScopeAll {
    interruptJoinUntil(deadline, "jdk25-scope-timeout") { delegate.join() }
    return this
}
```

### 교훈

- **처음부터 헬퍼 추출**. 나중에 발견하면 refactor + 수정 2단계 필요.
- 동일한 패턴이 3곳 이상 반복되면 즉시 추상화.
- companion object 헬퍼는 nested class에서도 접근 가능 — Kotlin에서 유효한 패턴.

---

## L5: `@EnabledOnJre(JRE.JAVA_25)` vs `@EnabledForJreRange(min = JRE.JAVA_25)`

### 문제

```kotlin
@EnabledOnJre(JRE.JAVA_25)  // JDK25 전용 — JDK26에서 자동 skip
```

JDK26 출시 시 `@EnabledOnJre(JRE.JAVA_25)` 테스트는 자동으로 비활성화된다.
"JDK25 stable API 사용"이 의도인데, 이 테스트는 미래 버전에서도 실행되어야 한다.

### 수정

```kotlin
@EnabledForJreRange(min = JRE.JAVA_25)  // JDK25 이상 모두 실행
```

### 교훈

- 특정 버전의 **기능/API**를 테스트하는 경우 → `@EnabledForJreRange(min = ...)` 사용.
- 특정 버전의 **버그/동작**을 테스트하는 경우 → `@EnabledOnJre(...)` 사용.
- 새 JDK stable API 기반 테스트는 항상 `min =` 형식으로 작성.

---

## L6: interrupt 기반 타임아웃에서 외부 interrupt와 타이머 interrupt를 구분할 수 없다

### 문제

JDK25 `StructuredTaskScope`에는 `joinUntil(Instant)` API가 없다.
`ScheduledThreadPoolExecutor`로 owner thread를 interrupt하는 방식으로 구현했으나:

```kotlin
} catch (e: InterruptedException) {
    // 이것이 타이머 interrupt인지, 외부 interrupt인지 알 수 없다
    throw TimeoutException("joinUntil deadline exceeded")
}
```

외부 `Thread.interrupt()` 호출도 `TimeoutException`으로 변환된다.

### 완화책

```kotlin
// 타이머가 실제로 발동했는지 확인하는 volatile flag 사용 (개선 여지)
@Volatile var timedOut = false
val timeoutFuture = scheduler.schedule({
    timedOut = true
    ownerThread.interrupt()
}, ...)
```

### 교훈

- interrupt 기반 타임아웃은 **JDK 공식 API(`joinUntil`)가 없을 때만** 사용하는 workaround.
- volatile flag로 타이머 발동 여부를 기록하면 외부 interrupt와 구분 가능 (현재는 미적용).
- JDK가 공식 API를 제공하면 즉시 교체해야 한다 — 주석으로 TODO 마킹.

---

## L7: deadline 계산은 fork 루프 시작 전에 해야 한다

### 문제

```kotlin
repeat(roundsPerWorker) { testBlocks.forEach { scope.fork { it() } } }
// ← fork 루프 후 deadline 계산 → fork 소요 시간만큼 timeout 윈도우가 줄어든다
val joined = timeout?.let { scope.joinUntil(Instant.now().plusMillis(it.inWholeMilliseconds)) }
```

### 수정

```kotlin
// fork 루프 전에 deadline 계산
val deadline = timeout?.let { Instant.now().plusMillis(it.inWholeMilliseconds) }
repeat(roundsPerWorker) { testBlocks.forEach { scope.fork { it() } } }
val joined = deadline?.let { scope.joinUntil(it) } ?: scope.join()
```

### 교훈

- **deadline은 "작업 시작 전" 기준으로 계산** — "join 직전" 기준이 아니다.
- `ParallelWorkFlow.executeAll`이 이미 올바른 패턴을 사용하고 있었다 — 기존 코드에서 먼저 학습.

---

## 참고

- PR #272: <https://github.com/bluetape4k/bluetape4k-projects/pull/272>
- 관련 이슈: #266, #268, #269
- 기존 PR: #271 (TaskContext ScopedValue 구현 — #265, #267 처리)
