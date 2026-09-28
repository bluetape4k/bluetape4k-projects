# Coroutine timeout API 코드 리뷰

## 검토 범위와 근거

- 기준 diff: `origin/develop` (`7c6cbd1e261efb23b8146ea5c6f949d6591212ee`)에서 현재 작업트리까지.
- 검토 항목: timeout 경계, 업무 예외와 취소 전파, 원본 작업 소유권, JDK 반환 계약, JVM ABI, 회귀 테스트, 한·영 README.
- 독립 리뷰어 실행을 요청했으나 현재 시점에 쓸 수 있는 판정이 도착하지 않았다. 아래 결과는 워크플로우가 허용한 **inline fallback review**이며 독립 attestation이 아니다.

## 판정

**PASS — P0 0, P1 0, P2 0.** 현재 PR 범위에서 제품 동작 또는 공개 API blocker를 찾지 못했다.

| 영역 | 검토 결과 |
|---|---|
| `CompletableFuture` | `get*`의 `ExecutionException` 유지와 `join*`의 원인 unwrap을 분리했다. 실제 timed wait의 timeout만 기본값/`null`로 바꾸며, 업무 원인인 `TimeoutException`은 보존한다. 완료된 nullable `join(default)`의 기존 기본값 동작도 테스트한다. |
| `ExecutorService` | 함수 목록을 `Callable`로 변환해 각 함수를 호출하고, `invokeAll`의 `List<Future<T>>`와 `invokeAny`의 `T` 반환을 보존한다. JDK timed overload에 Duration 나노초를 전달한다. 변환 시간이 JDK 대기 상한에 포함되지 않는 계약을 KDoc에 설명한다. |
| `SuspendLazy` | 신규 인터페이스 멤버는 기본 구현이다. 구 인터페이스로 컴파일한 Java 구현체 fixture가 현재 인터페이스에서 호출된다. `SuspendBlockingLazyImpl`은 waiter timeout/cancellation과 공유 초기화 작업을 분리하며, 성공값 재사용과 실패 후 재시도를 테스트한다. |
| `Deferred` / `Job` | `awaitUntil*` 및 `joinUntil`의 timeout은 waiter에 적용되고 원본 작업은 직접 취소하지 않는다. nullable 결과의 timeout 모호성, 실패·호출자 취소·무한 대기 계약을 테스트와 KDoc에 기록한다. |
| 문서와 ABI | 영·한 README의 실행 예제를 테스트 fixture와 대조했다. `javap -p -s`에서 JDK 반환형, Kotlin `Duration`의 JVM descriptor, `SuspendLazy` default method를 확인했다. 전역 ABI baseline task가 없어 이 검증은 targeted 증거로 한정한다. |

## 검증 증거와 잔여 범위

- `./gradlew :bluetape4k-core:test :bluetape4k-coroutines:test --no-configuration-cache`: core 1,704건, coroutines 667건 통과; 실패·오류·skip 0.
- `./gradlew :bluetape4k-core:detekt :bluetape4k-coroutines:detekt --no-configuration-cache --rerun-tasks`: Gradle task 성공. XML에는 저장소 기존 진단 379건이 남아 있고 변경 경로 진단 2건은 `origin/develop`의 `CompletableFutureSupport.kt`에도 존재하는 파일 함수 수 및 기존 `futureWithTimeout` 상수 항목이다.
- 이전 `SuspendLazy` ABI fixture는 coroutines 전체 테스트에 포함되어 통과했다.
- PR hosted CI는 생성 후 exact head에서 확인한다. 이 리뷰는 hosted CI나 전역 binary compatibility baseline 통과를 주장하지 않는다.
