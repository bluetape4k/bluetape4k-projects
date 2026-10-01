# CompletableFuture Duration 경로의 예외 계약

## 맥락

이슈 [#1805](https://github.com/bluetape4k/bluetape4k-projects/issues/1805)는 `CompletableFuture`의 Duration 기반 timeout 확장 함수에 직접 테스트와 KDoc 계약이 부족하고, 저장소 전체 ABI baseline 검증도 없다는 점을 다룬다.

JDK timed `CompletableFuture.get`은 기다리는 동안 만료된 timeout을 `TimeoutException`으로 던지고, 작업이 예외로 완료되면 원래 예외를 `cause`로 담은 `ExecutionException`을 던진다. [`CompletableFutureSupport.kt`](../../bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/CompletableFutureSupport.kt)의 `join(Duration)` 확장 함수는 원인을 unwrap하지만, `joinOrNull(Duration)`은 timed `get`을 호출한다. 회귀 조건은 [`CompletableFutureSupportTest.kt`](../../bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/CompletableFutureSupportTest.kt)에 고정했다.

## 발견

PR [#1806](https://github.com/bluetape4k/bluetape4k-projects/pull/1806)의 [`f4acc71f`](https://github.com/bluetape4k/bluetape4k-projects/commit/f4acc71f85)가 `joinOrNull(Duration)`에 `ExecutionException` unwrap을 추가했다. 그 결과 작업 실패 시 JDK `get`의 wrapper가 사라지고 원래 예외가 직접 노출됐다. [`2.0.0` 태그의 구현](https://github.com/bluetape4k/bluetape4k-projects/blob/2.0.0/bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/CompletableFutureSupport.kt)은 대기 timeout만 처리하고 `ExecutionException`은 보존했다.

대기 timeout과 작업 자체의 `TimeoutException`도 구분해야 한다. 전자는 timed `get`이 직접 던져 `null`로 바뀌지만, 후자는 실패 완료의 원인이므로 `ExecutionException.cause`로 남는다.

## 결정과 결과

`joinOrNull(Duration)`은 실제 대기 timeout만 `null`로 바꾸고, `ExecutionException`을 그대로 전파하도록 했다. `join(Duration)` 계열의 기존 원인 unwrap 계약은 유지했다. 테스트는 일반 업무 예외와 작업 자체가 던진 `TimeoutException`의 wrapper 및 `cause`, timeout, cancellation, interrupt, 0·음수·무한 Duration을 확인한다.

KDoc과 양쪽 README에서 Duration 변환 단위, 경계값, timeout 동작, 예외 전파 차이를 구분했다.

## 검증

- 회귀 테스트는 수정 전 `IllegalStateException`을 직접 던져 실패했고, 수정 뒤 `ExecutionException`과 원래 `cause`를 확인하며 통과했다.
- `CompletableFutureSupportTest`: 29개 통과.
- `:bluetape4k-core:test`: 1,711개 통과, 실패·오류·skip 0.
- core Detekt는 성공했다. 변경 파일을 `develop`과 비교했을 때 테스트 파일 진단은 없고, `CompletableFutureSupport.kt`의 기존 `TooManyFunctions`와 `MagicNumber` 두 진단만 동일하게 남았다.

## ABI 검증과 릴리스 판단

저장소와 `bluetape4k-core` Gradle task 목록에는 `checkBinaryCompatibility`, `checkProductionAbi`, `apiCheck`가 없고 저장소 전체 API dump도 없다. 따라서 저장소 전체 ABI baseline 비교는 완료됐다고 주장할 수 없다.

대신 `CompletableFutureSupportKt`의 공개 JVM 메서드를 `javap -public -s`로 비교했다. `2.0.0` artifact에는 47개, 현재 `develop`에는 50개가 있으며, 차이는 Duration 기반 `get`, `get(defaultValue)`, `getOrNull` 추가 3개이고 제거는 없다. 이 변경 branch와 기준 `develop`은 공개 메서드 50개로 서명 차이가 없다. 이는 core Future facade 범위의 증거이며 저장소 전체 baseline을 대신하지 않는다.

이번 수정은 `2.1.0` 공개 ABI를 바꾸지 않으므로 해당 버전에 포함해도 안전하다고 판단한다. 저장소 전체 API baseline 부재는 별도의 검증 공백으로 남긴다.

## 향후 지침

- JDK API를 감싸는 extension은 대기 중 발생한 timeout과 작업 실패의 예외를 분리해 테스트한다.
- `get`과 `join`처럼 원래 API가 다른 예외 계약을 제공하면, 새 overload도 이름만 보고 계약을 통일하지 말고 호출하는 JDK API의 동작을 보존한다.
- public API 검증 task나 baseline이 없으면 해당 검증을 통과했다고 기록하지 않는다. 변경된 facade의 실제 JVM 서명과 기준 artifact 비교, 미검증 범위, 릴리스 판단을 함께 남긴다.
