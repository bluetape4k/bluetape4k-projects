# [virtualthread] Issue #255 API 통합 — 구현 계획

> **Spec**: docs/superpowers/specs/2026-05-01-vt-api-cleanup-design.md
> **Branch**: feat/vt-api-cleanup
> **Date**: 2026-05-01

---

## 현황 요약

| 항목                             | 현재 값                                       |
|----------------------------------|-----------------------------------------------|
| `StructuredSubtask.getOrNull()`  | 미제공 — FAILED/UNAVAILABLE 상태에서 ISE 위험 |
| `StructuredTaskScopes.all/any()` | factory 기본값 없음, 이름 모호                |
| `structuredTaskScopeAll/Any()`   | core 편의 함수, deprecated 대상               |
| Provider SPI `withAll/withAny()` | 이름 모호하나 SPI 특성상 deprecated 제외      |

---

## 태스크 목록

### T1: `StructuredSubtask.get()` KDoc 보강 + `getOrNull()` default 메서드 추가

- **complexity**: high
- **파일**: `virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt`
- **변경 내용**:
    - `StructuredSubtask.get()` KDoc을 상태별 동작 명세로 교체:
        - `SUCCESS`: 결과 반환
        - `FAILED`: `IllegalStateException` throw (실패 원인 예외 자체를 던지지 않음 — 원인은 `exceptionOrNull()`로 조회)
        - `UNAVAILABLE` (미완료/취소/join 이전): `IllegalStateException` throw
        - `@see exceptionOrNull`, `@see getOrNull` 참조 추가
    - `StructuredSubtask` 인터페이스에 `getOrNull(): T?` default 메서드 추가:
      ```kotlin
      fun getOrNull(): T? {
          return if (state() == StructuredTaskScope.Subtask.State.SUCCESS) {
              try { get() } catch (_: IllegalStateException) { null }
          } else null
      }
      ```
    - 구현 주의: `state() == SUCCESS`이더라도 scope owner thread의 `join()` 이전 호출 시 JDK `ensureJoinedIfOwner()` 검사로 ISE 발생 가능 → try-catch로 감쌈
- **완료 기준**:
    - [ ] `StructuredSubtask.get()` KDoc에 SUCCESS/FAILED/UNAVAILABLE 상태별 동작 명시
    - [ ] `StructuredSubtask.getOrNull()` default 메서드 인터페이스에 추가
    - [ ] `getOrNull()` KDoc에 전제 조건 (join () 이후 호출), 상태별 동작, 코드 예제 포함
    - [ ] jdk21/jdk25 구현체 변경 불필요 확인 (default 메서드이므로)
- **의존성**: 없음 *(T2와 같은 파일 — T1 완료 후 T2 시작)*

---

### T2: `typealias` 추가 (`StructuredTaskScopeFailFast`, `StructuredTaskScopeFirstSuccess`)

- **complexity**: medium
- **파일**: `virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt`
- **변경 내용**:
    - 파일 상단 또는 인터페이스 선언 이후에 typealias 두 개 추가:
      ```kotlin
      /** fail-fast 동작을 하는 [StructuredTaskScopeAll]의 의도 명확 별칭입니다. */
      typealias StructuredTaskScopeFailFast = StructuredTaskScopeAll
  
      /** first-success 동작을 하는 [StructuredTaskScopeAny]의 의도 명확 별칭입니다. */
      typealias StructuredTaskScopeFirstSuccess<T> = StructuredTaskScopeAny<T>
      ```
    - `StructuredTaskScopeAll` KDoc에 `@see StructuredTaskScopeFailFast` 마이그레이션 안내 추가
    - `StructuredTaskScopeAny` KDoc에 `@see StructuredTaskScopeFirstSuccess` 마이그레이션 안내 추가
    - `StructuredTaskScopeAll.join()` KDoc에 타임아웃 경고 추가:
      > **주의**: 타임아웃 없이 호출하면 subtask가 무한 차단될 수 있습니다. 프로덕션 코드에서는 [joinUntil]을 사용하세요.
    - 주의: typealias에는 멤버 KDoc을 붙일 수 없으므로 모든 KDoc은 실제 인터페이스 (`StructuredTaskScopeAll`, `StructuredTaskScopeAny`)에 추가
- **완료 기준**:
    - [ ] `typealias StructuredTaskScopeFailFast = StructuredTaskScopeAll` 추가
    - [ ] `typealias StructuredTaskScopeFirstSuccess<T> = StructuredTaskScopeAny<T>` 추가
    - [ ] `StructuredTaskScopeAll` KDoc에 `@see StructuredTaskScopeFailFast` 포함
    - [ ] `StructuredTaskScopeAny` KDoc에 `@see StructuredTaskScopeFirstSuccess` 포함
    - [ ] `StructuredTaskScopeAll.join()` KDoc에 타임아웃 경고 포함
- **의존성**: T1 완료 후 *(동일 파일 순차 편집)*

---

### T3: `StructuredTaskScopeProvider`에 `withFailFast()`/`withFirstSuccess()` default 메서드 추가

- **complexity**: medium
- **파일**: `virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt`
- **변경 내용**:
    - `StructuredTaskScopeProvider` 인터페이스에 default 메서드 두 개 추가:
      ```kotlin
      /**
       * 실패 전파형(fail-fast) 블록을 실행합니다.
       * 기본 구현은 [withAll]에 위임합니다.
       * @see withAll
       */
      fun <T> withFailFast(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory(),
          block: (scope: StructuredTaskScopeFailFast) -> T,
      ): T = withAll(name, factory, block)
  
      /**
       * 성공 우선형(first-success) 블록을 실행합니다.
       * 기본 구현은 [withAny]에 위임합니다.
       * @see withAny
       */
      fun <T> withFirstSuccess(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory(),
          block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
      ): T = withAny(name, factory, block)
      ```
    - **[수정 #7] factory 기본값 통일**: `Thread.ofVirtual().factory()` → `VirtualThreads.threadFactory()` 사용
        - `VirtualThreads.threadFactory()`는 fallback 환경에서 플랫폼 스레드 팩토리를 안전하게 반환
        - `Thread.ofVirtual().factory()`는 가상 스레드 미지원 환경에서 throw할 수 있음
        - `StructuredTaskScopes.failFast()`(T4)도 동일 이유로 `VirtualThreads.threadFactory()` 사용 — 두 진입점의 기본값 일관성 유지
    - `withAll()` KDoc에 `@see withFailFast` 안내 추가 (deprecated 하지 않음 — SPI 안정성 유지)
    - `withAny()` KDoc에 `@see withFirstSuccess` 안내 추가 (deprecated 하지 않음 — SPI 안정성 유지)
    - **SPI 결정 재확인**: `withAll`/`withAny`는 jdk21/jdk25 구현체가 `override`하는 SPI 메서드이므로 deprecated 표시하지 않음
- **완료 기준**:
    - [ ] `withFailFast()` default 메서드 추가 (`withAll` 위임)
    - [ ] `withFirstSuccess()` default 메서드 추가 (`withAny` 위임)
    - [ ] `withAll()` KDoc에 `@see withFailFast` 포함
    - [ ] `withAny()` KDoc에 `@see withFirstSuccess` 포함
    - [ ] jdk21/jdk25 구현체 변경 없음 확인 (default 메서드이므로)
- **의존성**: T2 완료 후 *(typealias가 먼저 정의되어야 `StructuredTaskScopeFailFast`를 파라미터 타입으로 사용 가능)*

---

### T4: `StructuredTaskScopes` object에 `failFast()`/`firstSuccess()` 추가 + `all()`/`any()` `@Deprecated`

- **complexity**: medium
- **파일**: `virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt`
- **변경 내용**:
    - `StructuredTaskScopes` object에 새 진입 함수 두 개 추가 (factory 기본값 포함):
      ```kotlin
      /**
       * 실패 전파형(fail-fast) scope 블록을 실행합니다.
       * 하나의 subtask라도 실패하면 나머지를 즉시 중단하고 예외를 전파합니다.
       *
       * @param name scope 이름 (디버깅용)
       * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
       * @param block scope 실행 블록
       */
      fun <T> failFast(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory(),
          block: (scope: StructuredTaskScopeFailFast) -> T,
      ): T = provider().withFailFast(name, factory, block)
  
      /**
       * 성공 우선형(first-success) scope 블록을 실행합니다.
       * 첫 번째 성공한 subtask 결과를 반환하고 나머지를 취소합니다.
       *
       * @param name scope 이름 (디버깅용)
       * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
       * @param block scope 실행 블록
       */
      fun <T> firstSuccess(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory(),
          block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
      ): T = provider().withFirstSuccess(name, factory, block)
      ```
    - 기존 `all()` 함수에 `@Deprecated` 추가 (기존 시그니처 유지 — factory 기본값 없음):
      ```kotlin
      @Deprecated(
          message = "failFast()를 사용하세요.",
          replaceWith = ReplaceWith(
              "failFast(name, factory, block)",
              "io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes"
          )
      )
      fun <T> all(
          name: String? = null,
          factory: ThreadFactory,
          block: (scope: StructuredTaskScopeAll) -> T,
      ): T = provider().withAll(name, factory, block)
      ```
    - 기존 `any()` 함수에 `@Deprecated` 추가:
      ```kotlin
      @Deprecated(
          message = "firstSuccess()를 사용하세요.",
          replaceWith = ReplaceWith(
              "firstSuccess(name, factory, block)",
              "io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes"
          )
      )
      fun <T> any(
          name: String? = null,
          factory: ThreadFactory,
          block: (scope: StructuredTaskScopeAny<T>) -> T,
      ): T = provider().withAny(name, factory, block)
      ```
    - **[수정 #2 + LOW] `ReplaceWith` 표현식 + import 경로
      완성**: IDE quick-fix가 외부 호출 지점에서 `StructuredTaskScopes`를 자동 임포트할 수 있도록 두 번째 인자에 FQN 추가. 또한 표현식 자체도 `StructuredTaskScopes.failFast(...)` 형태로 명시해야 IDE가 `StructuredTaskScopes` 컨텍스트를 정확히 인식:
      ```kotlin
      // 수정 전
      replaceWith = ReplaceWith("failFast(name, factory, block)", "io...StructuredTaskScopes")
      // 수정 후
      replaceWith = ReplaceWith("StructuredTaskScopes.failFast(name, factory, block)", "io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes")
      ```
      `any()` → `firstSuccess()`도 동일하게 `StructuredTaskScopes.firstSuccess(...)` 형태로 변경
    - 새 `failFast()`/`firstSuccess()` 함수에 KDoc 예제 포함 (fork → join → get 패턴)
- **완료 기준**:
    - [ ] `failFast()` 추가 (factory 기본값: `VirtualThreads.threadFactory()`)
    - [ ] `firstSuccess()` 추가 (factory 기본값: `VirtualThreads.threadFactory()`)
    - [ ] `all()`에 `@Deprecated(message, ReplaceWith)` 추가
    - [ ] `any()`에 `@Deprecated(message, ReplaceWith)` 추가
    - [ ] KDoc 예제 포함 (fork → join → throwIfFailed/result → get 패턴)
- **의존성**: T3 완료 후

---

### T5: `StructuredTaskScopeAll`/`StructuredTaskScopeAny` KDoc 보강 (join 타임아웃 경고, @see 안내)

- **complexity**: low
- **파일**: `virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt`
- **변경 내용**:
    - `StructuredTaskScopeAll` 인터페이스 KDoc 상단 개요에 `@see StructuredTaskScopeFailFast` 추가
    - `StructuredTaskScopeAll.join()` 함수 KDoc에 타임아웃 경고 추가 (T2에서 이미 처리하므로 중복 체크)
    - `StructuredTaskScopeAny` 인터페이스 KDoc 상단 개요에 `@see StructuredTaskScopeFirstSuccess` 추가
    - 각 인터페이스 KDoc의 코드 예제를 새 API (`failFast`, `firstSuccess`)로 업데이트
    - 주의: T2에서 이미 일부 KDoc 보강을 수행하므로 T5는 남은 보강 항목 처리
- **완료 기준**:
    - [ ] `StructuredTaskScopeAll` KDoc에 `@see StructuredTaskScopeFailFast` 및 `failFast {}` 사용 안내 포함
    - [ ] `StructuredTaskScopeAny` KDoc에 `@see StructuredTaskScopeFirstSuccess` 및 `firstSuccess {}` 사용 안내 포함
    - [ ] KDoc 예제에서 `factory = Thread.ofVirtual().factory()` 명시적 지정 제거 (기본값 사용 가이드)
- **의존성**: T2 완료 후 *(T4와 병렬 가능)*

---

### T6: API 모듈 테스트 추가 (`getOrNull()`, `failFast()`, `firstSuccess()`)

- **complexity**: medium
- **파일**:
    - `virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopesTest.kt`
    - `virtualthread/api/build.gradle.kts` ← **[수정 신규] testRuntimeOnly 추가 필수**
- **[수정 HIGH] build.gradle.kts 선행
  변경**: `StructuredTaskScopes.provider()`는 ServiceLoader 기반이므로 test runtime에 구현체가 없으면 `IllegalStateException` 발생. 테스트가 실행 가능하려면:
  ```kotlin
  // virtualthread/api/build.gradle.kts에 추가
  dependencies {
      ...
      testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))  // ServiceLoader provider 등록
  }
  ```
    - 기존 `StructuredScopesTest.kt`의 주석 "test 런타임에는 jdk21 provider가 등록되어 있으므로"도 이 변경으로 비로소 보장됨
    - `testRuntimeOnly`이므로 컴파일 타임/배포 classpath에는 포함되지 않음 — 순환 의존성 없음
- **변경 내용**:
    - `failFast` 정상 경로 테스트: 모든 subtask 성공 → 결과 수집
      ```kotlin
      @Test
      fun `failFast scope 으로 두 subtask 를 합산해야 한다`() { ... }
      ```
    - `failFast` 실패 전파 테스트: 하나 실패 시 나머지 취소 + 예외 전파
      ```kotlin
      @Test
      fun `failFast scope 내 subtask 실패 시 예외가 전파되어야 한다`() { ... }
      ```
    - `failFast` factory 기본값 테스트: `factory` 파라미터 생략 가능 확인 (P3 해결 검증)
      ```kotlin
      @Test
      fun `failFast scope factory 기본값으로 실행되어야 한다`() { ... }
      ```
    - `firstSuccess` 정상 경로 테스트: 첫 성공 반환 + 나머지 취소
      ```kotlin
      @Test
      fun `firstSuccess scope 가 가장 먼저 완료된 subtask 결과를 반환해야 한다`() { ... }
      ```
    - `firstSuccess` 전체 실패 테스트: 모든 subtask 실패 시 예외 전파
      ```kotlin
      @Test
      fun `firstSuccess scope 모든 subtask 실패 시 mapper 예외가 발생해야 한다`() { ... }
      ```
        - **[수정 #4 재수정] 예외 타입
          수정**: `StructuredTaskScopeAny.result(mapper)` 계약상 모든 subtask 실패 시 `mapper`가 반환하는 `RuntimeException`을 throw. `StructuredTaskScope.FailedException`이 아님. 기존 `any scope 모든 subtask 실패 시 mapper 예외가 발생해야 한다` 테스트 (line 133)가 `assertFailsWith<IllegalStateException>`을 사용하는 것과 동일한 패턴 적용:
          ```kotlin
          // 잘못된 assertion (수정 전)
          shouldThrow<StructuredTaskScope.FailedException> { ... }
          // 올바른 assertion (수정 후)
          assertFailsWith<IllegalStateException> {
              StructuredTaskScopes.firstSuccess<String> { scope ->
                  scope.fork<String> { throw RuntimeException("task1 fail") }
                  scope.fork<String> { throw RuntimeException("task2 fail") }
                  scope.join().result { IllegalStateException("all failed: ${it.message}") }
              }
          }
          ```
    - `getOrNull()` SUCCESS 상태 테스트: join () 이후 정상 결과 반환
      ```kotlin
      @Test
      fun `getOrNull 은 SUCCESS 상태에서 결과를 반환해야 한다`() { ... }
      ```
    - `getOrNull()` FAILED 상태 테스트: join () 이후 FAILED subtask → null 반환 (ISE 아님)
      ```kotlin
      @Test
      fun `getOrNull 은 FAILED 상태에서 null 을 반환해야 한다`() { ... }
      ```
    - `getOrNull()` UNAVAILABLE 상태 테스트: `ShutdownOnFailure` scope에서 shutdown으로 취소된 subtask를 `join()` 이후 확인 → null 반환
        - **[수정 #1][CRITICAL] 결정론적 재현 패턴**: `CountDownLatch(2)`로 타이밍 제어
        - **[수정 HIGH] 타입 불일치
          수정**: `failFast<Int>` 블록의 마지막 표현식이 `cancelledTask!!.getOrNull()`이면 `Int?`를 반환 → 타입 불일치. `failFast<Unit>` + `try-catch` 구조로 수정:
          ```kotlin
          @Test
          fun `getOrNull 은 UNAVAILABLE 상태에서 null 을 반환해야 한다`() {
              val subtaskStarted = CountDownLatch(1)   // subtask가 실행 시작했음을 신호
              val proceedToFail  = CountDownLatch(1)   // 실패 subtask에게 진행 허가
              var cancelledTask: StructuredSubtask<Int>? = null
    
              // failFast<Unit>으로 블록 반환 타입 문제 회피
              runCatching {
                  StructuredTaskScopes.failFast<Unit> { scope ->
                      // subtask1: 실패하여 scope shutdown 트리거
                      scope.fork<Unit> { 
                          subtaskStarted.countDown()        // subtask2가 준비됨을 신호
                          proceedToFail.await()             // 실패 시점 제어
                          throw RuntimeException("forced failure")
                      }
                      // subtask2: block 상태로 유지되다가 shutdown에 의해 취소됨
                      cancelledTask = scope.fork { 
                          subtaskStarted.await()            // subtask1이 시작한 후
                          proceedToFail.countDown()         // subtask1에게 실패 신호
                          Thread.sleep(10_000)              // scope shutdown 전까지 block
                          42
                      }
                      scope.join().throwIfFailed()          // subtask1 실패 → 예외 전파
                  }
              }
              // join() 이후 scope shutdown으로 취소된 subtask의 상태 확인
              cancelledTask.shouldNotBeNull()
              cancelledTask!!.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.UNAVAILABLE
              cancelledTask!!.getOrNull().shouldBeNull()
          }
          ```
        - **구현
          주의**: `Jdk21AllScope.delegate`(private) 직접 접근 불가 → scope shutdown을 통해 간접 취소 유도. `CountDownLatch`로 실패 subtask와 block subtask 간 타이밍 결정론적 제어
    - `getOrNull()` join 이전 호출 안전성 테스트: `state() == SUCCESS`이나 join () 전 → try-catch로 null 반환
        - **[수정 #5] 분류 주의**: 이 테스트는 **내부 방어 (internal
          defense)** 테스트이며 공개 API 계약 (public contract) 테스트가 아님. `getOrNull()`의 KDoc 전제 조건은 "join () 이후 호출"이고 join () 이전 호출은 미정의 동작. 테스트 이름에 `(내부 방어)` 명시
      ```kotlin
      @Test
      fun `getOrNull 은 join 이전 호출에서도 null 을 안전하게 반환해야 한다 (내부 방어)`() { ... }
      ```
    - scope 리소스 해제 테스트: `close()` 보장 (use{} 블록 정상 종료 확인)
    - 기존 `all scope`/`any scope` 테스트는 그대로 유지 (회귀 방지)
    - bluetape4k-assertions 매처 사용: `shouldBeEqualTo`, `shouldBeNull`, `shouldNotBeNull`, `shouldBeInstanceOf`
- **완료 기준**:
    - [ ] `virtualthread/api/build.gradle.kts`에 `testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))` 추가
    - [ ] `failFast` 정상/실패/기본값 테스트 3건 추가
    - [ ] `firstSuccess` 정상/전체실패 테스트 2건 추가 (전체실패 시 **mapper가 반환한 예외
      타입** assertion — `assertFailsWith<IllegalStateException>` 패턴)
    - [ ] `getOrNull` SUCCESS/FAILED/UNAVAILABLE (CountDownLatch + `failFast<Unit>` 구조)/join전 (내부방어) 테스트 4건 추가
    - [ ] **[수정 #3] `joinUntil()` 타임아웃 테스트 1건
      추가**: scope 내 subtask가 제한 시간 내 완료되지 않을 때 `TimeoutException` 발생 확인 (Spec 섹션 4 명시 요구사항)
    - [ ] 기존 테스트 전부 통과 (회귀 없음)
- **의존성**: T1, T4 완료 후

---

### T7: `bluetape4k/core` — `structuredTaskScopeFailFast {}`/`structuredTaskScopeFirstSuccess {}` 추가 + 기존 deprecated

- **complexity**: medium
- **파일**: `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredTaskScopeSupport.kt`
- **변경 내용**:
    - 새 편의 함수 두 개 추가:
      ```kotlin
      /**
       * 실패 전파형(fail-fast) 구조화된 동시성 블록을 실행합니다.
       *
       * @param name scope 이름 (디버깅용, 기본값: null)
       * @param factory Virtual Thread 팩토리 (기본값: `VirtualThreads.threadFactory("sts-failfast-")`)
       * @param block scope 실행 블록
       * @return [block]의 실행 결과
       */
      fun <T> structuredTaskScopeFailFast(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory("sts-failfast-"),
          block: (scope: StructuredTaskScopeFailFast) -> T,
      ): T = StructuredTaskScopes.failFast(name, factory, block)
  
      /**
       * 성공 우선형(first-success) 구조화된 동시성 블록을 실행합니다.
       *
       * @param name scope 이름 (디버깅용, 기본값: null)
       * @param factory Virtual Thread 팩토리 (기본값: `VirtualThreads.threadFactory("sts-first-")`)
       * @param block scope 실행 블록
       * @return 가장 먼저 성공한 서브 작업의 결과
       */
      fun <T> structuredTaskScopeFirstSuccess(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory("sts-first-"),
          block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
      ): T = StructuredTaskScopes.firstSuccess(name, factory, block)
      ```
    - 기존 `structuredTaskScopeAll()` 함수에 deprecated 처리:
      ```kotlin
      @Suppress("DEPRECATION")
      @Deprecated(
          message = "structuredTaskScopeFailFast()를 사용하세요.",
          replaceWith = ReplaceWith(
              "structuredTaskScopeFailFast(name, factory, block)",
              "io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFailFast"
          )
      )
      fun <T> structuredTaskScopeAll(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory("sts-all-"),
          block: (scope: StructuredTaskScopeAll) -> T,
      ): T = StructuredTaskScopes.all(name, factory, block)
      ```
    - 기존 `structuredTaskScopeAny()` 함수에 deprecated 처리:
      ```kotlin
      @Suppress("DEPRECATION")
      @Deprecated(
          message = "structuredTaskScopeFirstSuccess()를 사용하세요.",
          replaceWith = ReplaceWith(
              "structuredTaskScopeFirstSuccess(name, factory, block)",
              "io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFirstSuccess"
          )
      )
      fun <T> structuredTaskScopeAny(
          name: String? = null,
          factory: ThreadFactory = VirtualThreads.threadFactory("sts-any-"),
          block: (scope: StructuredTaskScopeAny<T>) -> T,
      ): T = StructuredTaskScopes.any(name, factory, block)
      ```
    -
  **주의**: deprecated 함수 내부에서 `StructuredTaskScopes.all()`/`StructuredTaskScopes.any()` 호출 시 해당 함수도 deprecated이므로 `@Suppress("DEPRECATION")`이 반드시 필요
- **완료 기준**:
    - [ ] `structuredTaskScopeFailFast {}` 추가 (KDoc 포함)
    - [ ] `structuredTaskScopeFirstSuccess {}` 추가 (KDoc 포함)
    - [ ] `structuredTaskScopeAll {}` `@Deprecated(message, ReplaceWith)` 추가
    - [ ] `structuredTaskScopeAny {}` `@Deprecated(message, ReplaceWith)` 추가
    - [ ] deprecated 함수에 `@Suppress("DEPRECATION")` 추가
    - [ ] 컴파일 경고 없음 확인
- **의존성**: T4 완료 후

---

### T8: `bluetape4k/core` 모듈 테스트 — 새 편의 함수 테스트 추가

- **complexity**: medium
- **파일**: `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopeSupportTest.kt`
- **변경 내용**:
    - `WithAll` Nested 클래스 안에 `structuredTaskScopeFailFast {}` 기반 테스트 추가:
      ```kotlin
      @Test
      fun `structuredTaskScopeFailFast 으로 모든 SubTask 들이 완료될 때 결과를 반환한다`() { ... }
  
      @Test
      fun `structuredTaskScopeFailFast 에서 Subtask 예외 발생 시 예외를 던진다`() { ... }
      ```
    - `WithAny` Nested 클래스 안에 `structuredTaskScopeFirstSuccess {}` 기반 테스트 추가:
      ```kotlin
      @Test
      fun `structuredTaskScopeFirstSuccess 로 첫번째 완료된 작업의 결과를 얻는다`() { ... }
  
      @Test
      fun `structuredTaskScopeFirstSuccess 로 첫번째 성공한 결과를 반환한다`() { ... }
      ```
    - 기존 `structuredTaskScopeAll {}`/`structuredTaskScopeAny {}` 테스트는 그대로 유지 (회귀 방지 + deprecated API 동작 검증)
    - `@EnabledForJreRange(min = JRE.JAVA_21)` 어노테이션 유지
    - bluetape4k-assertions 매처 사용: `shouldBeEqualTo`, `shouldBeInstanceOf` 등
    - **[수정 #8] `@Suppress("DEPRECATION")` 대상 파일 명시**:
        - `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredTaskScopeSupport.kt`
          → `structuredTaskScopeAll` 내부에서 `StructuredTaskScopes.all()` 호출 시 `@Suppress("DEPRECATION")` 필요
        - `testing/junit5` 모듈: `VirtualFuture`/`structuredTaskScopeAll` 등 deprecated API 사용 여부 사전 확인 → 사용 중이면 `@Suppress("DEPRECATION")` 추가 또는 새 API로 마이그레이션
        - `utils/workflow` 모듈: `virtualFuture {}`, `structuredTaskScopeAll` 등 사용 여부 확인 후 동일 처리
    - **[수정 MEDIUM] deprecated 경고 정책 명시**:
        - **프로덕션 소스 (`src/main`)**: 경고 없음 목표 — `@Suppress("DEPRECATION")` 필수 적용
        - **테스트 소스 (`src/test`)**: 기존 deprecated API 동작 검증을 위해 `@Deprecated` 함수 직접 호출하는 경우 경고 허용 (`@Suppress` 선택 적용)
        - **Downstream
          모듈** (StructuredScopesTest, StructuredScopeSupportTest, utils/workflow, testing/junit5): 경고 허용 — 이번 PR 범위 내 마이그레이션 대상 아님. T9에서 컴파일 오류만 없으면 통과
        - **Examples** (`examples/virtualthreads-demo`): 이번 PR 범위 외 (out-of-scope). 후속 PR에서 처리
- **완료 기준**:
    - [ ] `structuredTaskScopeFailFast` 테스트 2건 추가 (정상/실패)
    - [ ] `structuredTaskScopeFirstSuccess` 테스트 2건 추가 (정상/첫성공)
    - [ ] 기존 `structuredTaskScopeAll`/`structuredTaskScopeAny` 테스트 전부 통과
    - [ ] T7 파일 `@Suppress("DEPRECATION")` 필요 위치 확인 + 적용
- **의존성**: T7 완료 후

---

### T9: 전체 빌드 검증

- **complexity**: low
- **파일**: 없음 (빌드 명령 실행)
- **변경 내용**:
    - **[수정 #6] 검증 범위 확장**: deprecated API를 사용 중인 downstream 모듈의 컴파일 경고 발생 여부 확인 필수
    - 6개 모듈 통합 빌드/테스트 실행:
      ```bash
      ./gradlew :bluetape4k-virtualthread-api:test \
                :bluetape4k-virtualthread-jdk21:test \
                :bluetape4k-virtualthread-jdk25:test \
                :bluetape4k-core:test \
                :bluetape4k-junit5:compileTestKotlin \
                :bluetape4k-workflow:compileTestKotlin
      ```
    - `testing/junit5`와 `utils/workflow` 모듈은 **테스트 실행 없이 컴파일만** 확인 (deprecated 경고 발생 여부 확인)
        - 경고 발생 시: 해당 파일에 `@Suppress("DEPRECATION")` 추가하거나 새 API로 마이그레이션 (T8에서 식별)
    - **[수정 MEDIUM] examples out-of-scope 리스크
      명시**: `examples/virtualthreads-demo`는 deprecated core helper (`structuredTaskScopeAll` 등)를 직접 사용할 가능성 있음. 이번 PR에서는 examples 컴파일을 검증 범위에 포함하지 않음 — 후속 PR에서 처리. 리스크: examples가 deprecated warning으로 인해 빌드 실패할 경우 별도 `@Suppress` 적용 필요
    - 실패 시 오류 메시지 분석 후 T1~T8 중 해당 태스크 재작업
    - jdk21/jdk25 구현체 변경 없음 확인 (기존 테스트 통과 확인)
    - 브레이킹 체인지 없음 확인 (`@Deprecated`만 추가, 삭제 없음)
- **완료 기준**:
    - [ ] `bluetape4k-virtualthread-api` 테스트 전부 통과
    - [ ] `bluetape4k-virtualthread-jdk21` 테스트 전부 통과 (변경 없음)
    - [ ] `bluetape4k-virtualthread-jdk25` 테스트 전부 통과 (변경 없음)
    - [ ] `bluetape4k-core` 테스트 전부 통과
    - [ ] `bluetape4k-junit5` 컴파일 성공 (deprecated 경고 처리 확인)
    - [ ] `bluetape4k-workflow` 컴파일 성공 (deprecated 경고 처리 확인)
    - [ ] deprecated 경고만 발생, 컴파일 오류 없음
- **의존성**: T6, T8 완료 후

---

### T10: README 업데이트 — 결정 트리 추가 및 API 예제 교체 (8개 파일)

- **complexity**: low
- **파일**:
    - `virtualthread/api/README.md`
    - `virtualthread/api/README.ko.md`
    - `virtualthread/jdk21/README.md`
    - `virtualthread/jdk21/README.ko.md`
    - `virtualthread/jdk25/README.md`
    - `virtualthread/jdk25/README.ko.md`
    - `virtualthread/README.md`
    - `virtualthread/README.ko.md`
- **변경 내용**:
    - `virtualthread/api/README.md`, `README.ko.md`: 결정 트리 (Decision Tree) 섹션 추가 (Spec 섹션 3.4.2 내용 그대로), API 레퍼런스에서 `all`/`any` → `failFast`/`firstSuccess` 교체
    - `virtualthread/jdk21/README.md`, `README.ko.md`: 예제 코드 `StructuredTaskScopes.all(factory = ...)` → `StructuredTaskScopes.failFast {}` 교체 (각 line 187 / line 186)
    - `virtualthread/jdk25/README.md`, `README.ko.md`: 예제 코드 `StructuredTaskScopes.all(factory = ...)` → `StructuredTaskScopes.failFast {}` 교체 (각 line 214 / line 212)
    - `virtualthread/README.md`, `README.ko.md`: 상위 모듈 개요에서 새 API 이름 (`failFast`, `firstSuccess`) 반영
    - 결정 트리 Mermaid 다이어그램 또는 텍스트 형식으로 삽입 (Spec의 ASCII 트리 그대로 사용 가능)
    - 모든 영문 README 수정 후 한국어 README 동일하게 적용
- **완료 기준**:
    - [ ] `virtualthread/api/README.md` — 결정 트리 추가 + `all`/`any` → `failFast`/`firstSuccess` 교체
    - [ ] `virtualthread/api/README.ko.md` — 동일 (한국어)
    - [ ] `virtualthread/jdk21/README.md` — 예제 코드 교체
    - [ ] `virtualthread/jdk21/README.ko.md` — 예제 코드 교체 (한국어)
    - [ ] `virtualthread/jdk25/README.md` — 예제 코드 교체
    - [ ] `virtualthread/jdk25/README.ko.md` — 예제 코드 교체 (한국어)
    - [ ] `virtualthread/README.md` — 새 API 이름 반영
    - [ ] `virtualthread/README.ko.md` — 새 API 이름 반영 (한국어)
- **의존성**: T4 완료 후 *(T6, T7, T8과 병렬 가능)*

---

## 실행 순서

| 순서 | 태스크 | 설명                                                            | 병렬 가능 여부              |
|------|--------|-----------------------------------------------------------------|-----------------------------|
| 1    | T1     | `getOrNull()` default 메서드 + `get()` KDoc 보강                | - (시작점)                  |
| 2    | T2     | `typealias` 추가 + `join()` 타임아웃 경고 KDoc                  | T1 완료 후 (동일 파일 순차) |
| 3    | T3     | `withFailFast()`/`withFirstSuccess()` default 메서드 추가       | T2 완료 후                  |
| 4    | T4     | `failFast()`/`firstSuccess()` 추가 + `all()`/`any()` deprecated | T3 완료 후                  |
| 5    | T5     | `StructuredTaskScopeAll`/`Any` KDoc 보강                        | T4와 병렬 가능 (T2 완료 후) |
| 5    | T10    | README 8개 파일 업데이트                                        | T4와 병렬 가능              |
| 6    | T6     | API 모듈 테스트 추가                                            | T4 완료 후 (T5, T10과 병렬) |
| 6    | T7     | core 모듈 편의 함수 추가 + deprecated                           | T4 완료 후 (T6, T10과 병렬) |
| 7    | T8     | core 모듈 테스트 추가                                           | T7 완료 후                  |
| 8    | T9     | 전체 빌드 검증 (4개 모듈)                                       | T6, T8 완료 후              |

### 병렬 실행 가능 그룹

```
T1 → T2 → T3 → T4 ─┬─ T5 (병렬)
                     ├─ T6 (병렬)
                     ├─ T7 ─ T8 (순차)
                     └─ T10 (병렬)
                          └─ (T5, T6, T8 모두 완료) → T9
```

---

## 수정 대상 파일 전체 목록

| 파일                                                                                                   | 태스크             | 변경 유형                                                          |
|--------------------------------------------------------------------------------------------------------|--------------------|--------------------------------------------------------------------|
| `virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt`         | T1, T2, T3, T4, T5 | 인터페이스 확장, KDoc 보강, deprecated 추가                        |
| `virtualthread/api/build.gradle.kts`                                                                   | T6                 | `testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))` 추가 |
| `virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopesTest.kt`     | T6                 | 테스트 추가                                                        |
| `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredTaskScopeSupport.kt` | T7                 | 편의 함수 추가, deprecated 추가                                    |
| `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopeSupportTest.kt` | T8                 | 테스트 추가                                                        |
| `virtualthread/api/README.md`                                                                          | T10                | 결정 트리, API 교체                                                |
| `virtualthread/api/README.ko.md`                                                                       | T10                | 결정 트리, API 교체 (한국어)                                       |
| `virtualthread/jdk21/README.md`                                                                        | T10                | 예제 코드 교체                                                     |
| `virtualthread/jdk21/README.ko.md`                                                                     | T10                | 예제 코드 교체 (한국어)                                            |
| `virtualthread/jdk25/README.md`                                                                        | T10                | 예제 코드 교체                                                     |
| `virtualthread/jdk25/README.ko.md`                                                                     | T10                | 예제 코드 교체 (한국어)                                            |
| `virtualthread/README.md`                                                                              | T10                | 새 API 이름 반영                                                   |
| `virtualthread/README.ko.md`                                                                           | T10                | 새 API 이름 반영 (한국어)                                          |

**변경하지 않는 파일**:

- `virtualthread/jdk21/src/main/.../Jdk21StructuredTaskScopeProvider.kt` — default 메서드로 자동 상속
- `virtualthread/jdk25/src/main/.../Jdk25StructuredTaskScopeProvider.kt` — default 메서드로 자동 상속

---

## 핵심 구현 제약 사항

| 항목                                        | 결정                                                      | 근거                                                                          |
|---------------------------------------------|-----------------------------------------------------------|-------------------------------------------------------------------------------|
| `typealias` vs 독립 인터페이스              | `typealias` 채택                                          | 반환 타입 불일치 문제로 독립 인터페이스 기각 (Spec 섹션 3.1.1)                |
| Provider SPI `withAll`/`withAny` deprecated | **하지 않음**                                             | jdk21/jdk25 override SPI — deprecated 시 구현체에 경고 전파 (Spec 섹션 3.4.1) |
| `getOrNull()` 구현 패턴                     | `try { get() } catch (_: IllegalStateException) { null }` | join() 전 호출 시 JDK `ensureJoinedIfOwner()` 검사 우회                       |
| deprecated 함수 내 deprecated API 호출      | `@Suppress("DEPRECATION")` 필수                           | `structuredTaskScopeAll` → `StructuredTaskScopes.all()` 호출 체인             |
| `autoJoin` 파라미터                         | **도입하지 않음**                                         | YAGNI (Spec 섹션 3.3.3)                                                       |
| join() warn 로그                            | **추가하지 않음**                                         | 노이즈 과다 → KDoc 경고로 대체 (Spec 섹션 3.3.2)                              |

---

## 빌드 검증 명령

```bash
# 핵심 4개 모듈 전체 테스트
./gradlew :bluetape4k-virtualthread-api:test \
          :bluetape4k-virtualthread-jdk21:test \
          :bluetape4k-virtualthread-jdk25:test \
          :bluetape4k-core:test

# downstream 모듈 컴파일 검증 (deprecated 경고 처리 확인)
./gradlew :bluetape4k-junit5:compileTestKotlin \
          :bluetape4k-workflow:compileTestKotlin
```

---

## DoD 체크리스트

- [ ] `virtualthread/api` — `typealias StructuredTaskScopeFailFast`, `StructuredTaskScopeFirstSuccess` 추가
- [ ] `virtualthread/api` — `StructuredSubtask.getOrNull()` default 메서드 구현 + KDoc
- [ ] `virtualthread/api` — `StructuredSubtask.get()` KDoc 상태별 동작 명시
- [ ] `virtualthread/api` — `StructuredTaskScopeProvider.withFailFast()`/`withFirstSuccess()` default 메서드 추가
- [ ] `virtualthread/api` — `withAll()`/`withAny()` KDoc `@see` 안내 추가 (deprecated 아님)
- [ ] `virtualthread/api` — `StructuredTaskScopes.failFast()`/`firstSuccess()` 추가 (factory 기본값 포함)
- [ ] `virtualthread/api` — `all()`/`any()` `@Deprecated(message, ReplaceWith)` 추가
- [ ] `virtualthread/api` — `StructuredTaskScopeAll.join()` KDoc 타임아웃 경고 추가
- [ ] `virtualthread/jdk21` — 변경 없음 확인 (기존 테스트 그대로 통과)
- [ ] `virtualthread/jdk25` — 변경 없음 확인 (기존 테스트 그대로 통과)
- [ ] `bluetape4k/core` — `structuredTaskScopeFailFast {}`/`structuredTaskScopeFirstSuccess {}` 추가
- [ ] `bluetape4k/core` — `structuredTaskScopeAll {}`/`structuredTaskScopeAny {}` `@Deprecated` 추가
- [ ] `bluetape4k/core` — deprecated 함수에 `@Suppress("DEPRECATION")` 추가
- [ ] 새 API 테스트 추가 (`failFast`, `firstSuccess`, `getOrNull`, `joinUntil`) — T6, T8
- [ ] getOrNull () UNAVAILABLE 테스트: CountDownLatch + `failFast<Unit>` + try-catch 구조로 타입 불일치 없이 결정론적 재현
- [ ] firstSuccess 전체 실패 테스트: `StructuredTaskScope.FailedException` 아닌 **mapper가 반환한 예외
  타입** assertion (`assertFailsWith<IllegalStateException>`)
- [ ] 기존 테스트 전부 통과 (회귀 없음)
- [ ] KDoc 갱신 (모든 public API — 새 추가 + 기존 보강)
- [ ] README.md / README.ko.md 결정 트리 추가 및 API 예제 교체 (8개 파일)
- [ ] `@Suppress("DEPRECATION")` 대상 파일 처리 완료 (`StructuredTaskScopeSupport.kt`, `junit5`, `workflow` 확인)
- [ ] `./gradlew :bluetape4k-virtualthread-api:test :bluetape4k-virtualthread-jdk21:test :bluetape4k-virtualthread-jdk25:test :bluetape4k-core:test` 통과
- [ ] `./gradlew :bluetape4k-junit5:compileTestKotlin :bluetape4k-workflow:compileTestKotlin` 오류 없음
- [ ] 브레이킹 체인지 없음 확인 (`@Deprecated`만 추가, 삭제 없음)
- [ ] `withFailFast()`/`failFast()` factory 기본값 통일 확인 (둘 다 `VirtualThreads.threadFactory()`)
