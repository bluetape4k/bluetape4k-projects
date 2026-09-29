# Coroutine timeout API 코드 리뷰

## 검토 범위와 근거

- 기준: `origin/develop=7c6cbd1e261efb23b8146ea5c6f949d6591212ee`부터 `feature/coroutines-timeout` 전체 변경.
- 범위: `CompletableFuture`, `ExecutorService`, `SuspendLazy`, `Deferred`, `Job`의 `Duration` timeout API, JVM 공개 서명, 동시성·취소 계약, 한·영 KDoc/README와 예제.
- 상태: 로컬 구현과 회귀 테스트는 완료했다. 마지막 동기화 의미 보정 뒤 별도 독립 재리뷰 verdict는 받지 못했다. hosted CI는 PR 생성 후 실행한다.

## 리뷰 결과와 수정

최초 리뷰는 `REQUEST CHANGES`(P0 0, P1 2, P2 1)였다. 후속 성능·안정성·API 정확성 검토에서 추가 지적을 받아 다음처럼 수정했다.

| 등급 | 발견 | 수정과 검증 |
|---|---|---|
| P1 | `ExecutorServiceExtensionsTest`의 11개 중 7개가 JUnit이 인식하지 않는 반환형이었다. | 테스트 helper를 `Unit` 반환으로 바꿨다. 최신 XML에서 11개 실행, 실패·오류·skip 0이다. |
| P1 | 같은 단일 dispatcher에서 `suspendBlockingLazy`의 blocking initializer가 실행돼 timeout waiter의 반환을 막을 수 있었다. | timeout 초기화를 waiter와 격리하고 필요할 때 `Dispatchers.IO`를 쓴다. 기본 context, 같은 dispatcher, 별도 dispatcher 및 timeout 뒤 source 작업의 생존을 latch 기반 테스트로 검증했다. |
| P2 | 분리된 blocking initializer의 owner 취소/정리 경로가 부족했다. | `SuspendLazy.cancel()` 기본 멤버와 initializer 소유 `SupervisorJob`을 두었다. interruptible worker 정리 및 scope 소유 lazy에서 owner scope를 보존하는 동작을 검증했다. 이전 ABI 모양으로 컴파일한 Java 구현 fixture도 새 기본 메서드 호출을 확인한다. |
| P2 | 공통 `Mutex`가 `LazyThreadSafetyMode.PUBLICATION`과 `NONE`에서도 initializer 동작을 직렬화해 공개 `Lazy` 계약을 바꿀 수 있었다. | mutex를 `SYNCHRONIZED`에서만 생성하고 사용하도록 한정했다. `PUBLICATION`에서 여러 initializer 실행을 확인하는 테스트는 수정 전에 실패했고, 모드 조건 적용 후 통과했다. |
| P3 | 설정 `CoroutineContext`에 든 `Job`이 caller `Job`보다 우선할 수 있다는 직접 호출 계약이 문서에 분명하지 않았다. | KDoc과 한국어·영어 README에 명시했다. |
| P3 | interrupt를 무시하는 blocking initializer의 timeout 한계가 전용 테스트로 고정되지 않았다. | interrupt를 무시한 작업이 timeout 반환 뒤 release 신호까지 계속되는 테스트를 추가했다. 이 한계는 README에도 설명한다. |

## 최신 검증

- `./gradlew :bluetape4k-core:test --rerun-tasks --stacktrace --console=plain`: `BUILD SUCCESSFUL`, 1,711개 통과, 실패·오류·skip 0.
- `./gradlew :bluetape4k-coroutines:test --rerun-tasks --stacktrace --console=plain`: `BUILD SUCCESSFUL`, 684개 통과, 실패·오류·skip 0.
- `ExecutorServiceExtensionsTest`: 11/11 실행, JUnit discovery 누락 없음.
- `./gradlew :bluetape4k-core:detekt :bluetape4k-coroutines:detekt --rerun-tasks --console=plain`: task 종료 코드 0. 프로젝트 설정은 `ignoreFailures`이며 XML에는 core 269건, coroutines 110건 진단이 있다. 새 timeout 변경 Kotlin 파일에는 신규 진단이 없다. core의 `CompletableFutureSupport.kt` 두 진단은 기준 브랜치에도 존재한다.
- `javap -p -s -c`: `SuspendLazy.cancel()V`가 public default method이고, timeout 메서드·확장 함수의 JVM descriptor와 `ExecutorService.invokeAll`의 `List`, `invokeAny`의 결과 반환형을 확인했다. 구 인터페이스만 구현한 Java fixture가 새 timeout default 메서드와 `cancel()`을 호출한다.
- 전역 `checkBinaryCompatibility`/`checkProductionAbi` task는 없다. 위 검사는 변경 API 대상의 bytecode 확인이지 저장소 전체 ABI baseline 통과 증거가 아니다.
- README 호출 예제는 test source에 복제해 coroutines/core 전체 테스트 컴파일과 실행에 포함했다. 한·영 설명을 상호 대조했다.
- `git diff --check` 통과. `bluetape-writer` 자연스러움 기준으로 새 한국어 문서와 변경된 README를 다시 읽었다. 용어 감사는 6개 한국어 파일에서 clinic 전용 loanword 규칙 2건을 표시했다. 두 곳은 timeout API와 무관한 기존 Flow 설명이며 이번 diff에 포함되지 않아 의도적 예외로 보존했다.

## 마지막 보완에 대한 리뷰 수렴

마지막 독립 리뷰 뒤 바뀐 범위는 mode별 mutex 선택, `PUBLICATION` 회귀 테스트, context의 `Job` 설명, interrupt를 무시하는 initializer 테스트와 문서다. 해당 리뷰어들이 더는 활성 상태가 아니어서 마지막 변경은 주 세션에서 exact diff를 다시 검토했다. 다음 결과는 독립 재리뷰로 가장하지 않는다.

| 관점 | 최신 보완 검토와 결과 |
|---|---|
| 성능 | cached 값은 timeout timer를 만들지 않는다. timeout worker는 `Dispatchers.IO`에서 실행하고 `Mutex`는 `SYNCHRONIZED`에서만 할당·사용한다. `PUBLICATION` 중복 initializer 테스트가 이 계약을 검증한다. 미해결 P0/P1 없음. |
| 안정성 | timeout 대기자와 initializer `Deferred`의 생명주기를 분리한다. completion callback은 공유 참조를 정리하고 owner job을 완료한다. 기본/별칭 dispatcher, 재시도, 취소와 non-cooperative 작업 종료를 전체 테스트로 검증했다. 미해결 P0/P1 없음. |
| 보안 | 이 보완은 신뢰 입력·권한·직렬화·비밀 정보 경계를 추가하지 않는다. 이전 보안 검토는 clean이었고 새 diff에도 해당 변경이 없다. 최신 diff 범위 N/A. |
| 운영 | worker 취소는 interrupt를 요청하며 무시하는 코드는 남을 수 있다. 새 테스트와 두 README가 이 종료 한계를 설명한다. 별도 설정·마이그레이션·운영 절차는 추가하지 않았다. 미해결 P0/P1 없음. |
| 개발자/API | `LazyThreadSafetyMode`별 의미, JDK 결과형, `SuspendLazy` default method를 source·`javap`·legacy fixture로 대조했다. 새 timeout Kotlin 경로 Detekt 진단 0건. 미해결 P0/P1 없음. |
| 사용자/호출자 | 한국어·영어 README가 상대 Duration, timeout과 nullable 결과, waiter/source 취소 소유권, 설정 `Job` 동작을 같은 의미로 설명한다. 예제는 모듈 테스트에서 컴파일·실행했다. 미해결 P0/P1 없음. |
| 주 세션 통합 | API, 구현, 테스트, README, lesson, testlog와 ABI 증거를 현재 branch diff에 대조했다. 알려진 제한과 검토 provenance를 본 문서에 기록했다. P0=0, P1=0. |

## 최종 판단

현재 확인된 미해결 P0/P1/P2/P3 결함은 없다. 마지막 보완에 대한 관점별 결과는 위 표처럼 주 세션에서 확인했으며, 독립 reviewer의 마지막 verdict는 없다. 비협력적이고 interrupt를 무시하는 blocking 작업은 timeout 뒤에도 실제 작업이 끝날 때까지 실행될 수 있다. 커스텀 dispatcher의 timeout 시간 정밀도는 그 dispatcher의 스케줄링에 따른다. hosted CI는 PR 생성 뒤 exact head에서 확인한다.
