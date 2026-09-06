# #1640 JPA·AsyncTaskExecutor 통합 회귀 검증 설계

## 목적과 승인 범위

- 이슈: [#1640](https://github.com/bluetape4k/bluetape4k-projects/issues/1640), milestone `2.1.0`, 담당자 `debop`.
- 기준 커밋: `31ee966cf339f7b895284329c8fbd53638ab837c`.
- 작업 브랜치: `test/issue-1640-jpa-executor-contracts`, PR base: `develop`.
- 사용자는 Type A 계획과 아래 상세 설계에 차례로 승인했다. PR 생성은 승인 범위이며 병합은 exact-head 검증 후 별도 승인이 필요하다.
- 현재 Projects 제품 결함을 재현했다는 주장이 아니라 Spring Boot 수정과 Projects executor 구성의 결합을 검증하는 작업이다.

## 현재 근거

1. [Spring Boot #50801](https://github.com/spring-projects/spring-boot/pull/50801)은 JPA 구성 과정에서 모든 `AsyncTaskExecutor`를 일찍 생성하던 경로를 지연 조회로 변경했다. executor를 선언하는 설정의 생성자가 `EntityManagerFactory`에 의존하면 기본 동기 bootstrap에서도 순환 의존이 발생할 수 있었다.
2. upstream 테스트 `whenAsyncTaskExecutorIsDefinedInJpaDependentConfigurationDoesNotFail`가 그 구성을 검증한다. 조회한 PR head는 `d4d20787c17d5f4c2f390a793d01a58d381f57e0`, merge는 `1c423ab57d10163c171984fbe4af2c9e1683e389`이다. [해당 테스트 변경](https://github.com/spring-projects/spring-boot/pull/50801/files), [4.1.1 릴리스](https://github.com/spring-projects/spring-boot/releases/tag/v4.1.1).
3. `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/virtualthread/VirtualThreadAutoConfiguration.kt`는 명시적으로 등록하면 `AsyncTaskExecutor`가 없을 때 가상 스레드 executor를 제공한다. 현재 구현에는 `spring.threads.virtual.enabled` 조건이 없다.
4. `spring-boot/hibernate-lettuce/build.gradle.kts`와 `spring-boot/hibernate-lettuce-demo/build.gradle.kts`는 test configuration에서 Boot `4.0.3`, Framework `7.0.5`, Hibernate `7.2.4.Final`, Spring Data `4.0.3`을 강제한다.
5. 준비 단계 `dependencyInsight`는 catalog가 요청한 Boot `4.1.1` 대신 규칙에 의해 `4.0.3`을 선택함을 확인했다. 기본 catalog ref는 `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`이며 환경변수 override는 없었다.
6. 기존 `AbstractVirtualThreadControllerTest`는 3개 통과, 실패·오류·제외 0개였다. 이 결과는 새 JPA fixture나 전체 모듈 통과 증거가 아니다.

## 선택한 배치와 대안

`spring-boot/hibernate-lettuce`의 기존 JPA·H2 의존성을 재사용하고,
`testImplementation(project(":bluetape4k-spring-boot-core"))`만 추가한다.
새 통합 테스트는 `src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/jpa/executor/`에 두고, 기존 cache 테스트의 package scan과 분리한다.

대안은 core 테스트에 JPA·H2를 추가하는 것이다. executor 코드와 가까워지지만 범용 core의 테스트 classpath에 persistence stack이 추가된다. 이미 JPA를 검증하는 모듈을 사용하는 편이 변경 범위를 줄인다.

기존 Redis 통합 테스트에 사례를 추가하는 방법은 채택하지 않는다. 순환 의존 검증에 Redis나 L2 cache 동작이 필요하지 않고, 실패 원인이 불필요하게 늘어난다.

## 구성과 동작 계약

### 독립 JPA fixture

`ApplicationContextRunner`에서 필요한 DataSource·Hibernate JPA·Spring Data JPA 자동 구성만 명시적으로 선택한다. 엔티티와 repository 탐색 범위는 fixture 전용 package로 제한하고, repository 자체는 Spring Data가 생성하도록 한다. `EntityManagerFactory`나 repository mock으로 성공을 대체하지 않는다.

각 context는 고유한 H2 인메모리 DB를 사용하고 context 종료와 함께 자원을 정리한다. cache 자동 구성이나 Redis launcher를 로드하지 않는다. JPA 저장 후 새 조회로 식별자와 값을 검증하며, executor에도 실제 작업을 제출하고 제한 시간 내 결과를 확인한다.

전체 fixture는 8개 context로 제한한다. Projects 기본 executor 3개(property 미설정·false·true), EMF 의존 caller executor 2개(false·true), Boot 단독 executor 3개(미설정·false·true)다. 모든 context에서 실제 EMF와 repository 생성은 확인하되, 저장·새 조회는 Projects 기본 미설정 사례에서만 실행한다. 각 사례는 짧은 작업 1개만 제출하며 최대 in-flight 작업도 1개다. Boot 단독 경로에서만 TaskExecution 자동 구성을 추가한다.

fixture는 JUnit `ExecutionMode.SAME_THREAD`로 실행하고 각 context를 닫은 뒤 다음 사례로 진행한다. 신규 fixture에 thread/queue 부하 실험이나 동시성 stress를 추가하지 않는다. 기존 전체 모듈의 별도 동시성 테스트는 변경하지 않는다.

### 기본 executor와 caller back-off

- caller executor가 없으면 Projects 구성을 명시적으로 등록하여 `virtualThreadTaskExecutor`가 하나 생성되는지 확인한다.
- caller executor를 선언하는 설정은 생성자에서 실제 `EntityManagerFactory`를 받는다. 이 사례가 upstream 회귀 재현 조건이다. 기본 `spring.jpa.bootstrap=default`를 유지한다.
- caller executor가 있으면 Projects의 기본 bean이 생성되지 않고 caller bean과 동일한 인스턴스를 사용하는지 확인한다. caller executor는 플랫폼 스레드를 사용하여 Projects가 임의로 가상 스레드 설정을 덮어쓰지 않았는지도 검증한다.
- JPA와 무관한 executor 두 개를 함께 생성하는 테스트는 위 순환 의존 사례를 대신할 수 없다.

caller fixture는 `@Configuration(proxyBeanMethods = false)` 클래스로 만들고 생성자에서 `EntityManagerFactory`를 받는다. 그 클래스의 `@Bean` 메서드가 `SimpleAsyncTaskExecutor`를 구체 반환 타입으로 선언한다. caller 설정을 user configuration으로 먼저 등록하고 Projects 설정을 명시적으로 등록한다. JPA 자동 구성은 `AutoConfigurations.of(...)`의 정상 순서를 사용하며, `@Lazy`, 수동 EMF bean, 순환 참조 허용 옵션, 임의 `@DependsOn`으로 회귀 조건을 우회하지 않는다.

성공 판정은 `context.startupFailure`가 없음을 먼저 확인한 뒤 EMF·repository·executor의 사용 가능성을 검사한다. 실패하면 예외 원인 사슬을 읽어 `BeanCurrentlyInCreationException`과 관련 bean 경로를 기록한다. 클래스 누락, 잘못된 엔티티 탐색, JDBC 연결 실패는 회귀 재현 성공이 아니라 fixture 또는 classpath 실패다.

### Boot property와 명시적 import의 구분

다음 두 경로를 분리한다.

| 경로 | false 또는 기본값 | true | 검증 대상 |
|---|---|---|---|
| Projects 구성 명시 등록, caller executor 없음 | 가상 스레드 | 가상 스레드 | 실행 작업의 `Thread.currentThread().isVirtual` |
| Projects 구성 없음, Boot TaskExecution 자동 구성 | 플랫폼 스레드 | 가상 스레드 | Boot가 만든 executor의 실제 실행 스레드 |

caller back-off 사례도 property true/false에서 caller 설정을 유지한다.
Projects 구성을 등록하지 않았다는 이유로 모든 executor가 없어야 한다고 가정하지 않는다. Boot 자체 자동 구성과 Projects bean 이름의 부재를 구분한다.

### 종료와 실패 처리

context가 소유한 executor, `EntityManagerFactory`, DataSource는 context 종료로 닫힌다. 테스트 작업은 종료 전에 제한 시간 안에 완료하고, 종료 이후에는 EMF 닫힘과 executor의 신규 제출 거부를 확인한다. `SimpleAsyncTaskExecutor`의 기본 설정이 실행 중 작업의 완료 대기까지 보장한다고 가정하지 않는다.

caller bean도 테스트 context가 생성·관리한 자원이므로 해당 context 종료 대상이다. 외부에서 공유 중인 executor를 임의로 닫는 계약을 새로 도입하지 않는다. 신규 fixture의 shutdown 실패나 timeout은 테스트 실패로 처리하며 재시도로 숨기지 않는다.

시간 기준은 startup 30초, 작업 결과 `Future.get(5, TimeUnit.SECONDS)`, context 종료 10초, 사례 전체 60초다. startup·close 기준은 반환 후 확인하는 경과 시간 assertion이며 강제 중단 상한이 아니다. `@Timeout(60)`은 SAME_THREAD interrupt 경계이고 비협력적 작업의 종료를 보장하지 않는다.

따라서 신규 fixture를 실행하는 모든 Gradle invocation에 외부 프로세스 상한을 적용한다. 단독 fixture 실행은 10분, AC-06의 각 전체 모듈 실행은 30분으로 제한하고 CI에도 유한한 step/job timeout을 적용한다. 초과 시 해당 실행의 로그·가능한 thread dump를 보존하고 소유한 Gradle 실행과 자식 test JVM을 종료한 뒤 실패 처리한다. 검증은 `--no-daemon`의 격리된 프로세스로 실행하여 다른 작업의 daemon을 종료하지 않는다. 전역 Gradle daemon이나 건강한 Docker 환경을 재시작하지 않는다.

무제한 `get`·`join`·`await`는 사용하지 않는다. 작업 timeout 때는 반환된 Future를 취소하고 context를 `finally`에서 닫으며, 최초 실패와 정리 실패를 모두 보존한다. 정상 흐름에서는 `ApplicationContextRunner.run` 반환 후 닫힌 자원에 대해 판정한다. Projects와 caller는 `SimpleAsyncTaskExecutor`, Boot 경로는 실제 `SimpleAsyncTaskExecutor` 또는 `ThreadPoolTaskExecutor`임을 확인한 다음 `TaskRejectedException`을 검증한다. 이는 임의의 `AsyncTaskExecutor` 구현 전체의 계약이 아니다.

DataSource는 기존 의존성의 `HikariDataSource`를 fixture에서 선택하고, 종료 후 `isClosed`를 확인한다. H2에는 `DB_CLOSE_DELAY=-1`을 넣지 않아 마지막 연결 종료 후 DB를 유지하지 않는다. EMF의 `isOpen=false`와 pool 종료를 각각 확인한다.

## 버전 정렬과 호환성

두 Hibernate Lettuce build의 구버전 강제 블록을 제거하는 것을 우선한다. 새 버전 상수나 새로운 강제 규칙으로 치환하지 않는다. 필요 시 기존 catalog alias와 BOM의 관계만 최소 수정한다.

검증 기준은 파일에 적힌 버전이 아니라 `testCompileClasspath` 및 `testRuntimeClasspath`에서 실제 선택된 Boot·Framework·Hibernate·Spring Data·Jakarta Persistence 버전과 선택 이유다. Boot `4.1.1` 및 catalog/BOM과 일치하는 그래프를 확인하고, 두 모듈의 기존 테스트가 그 그래프로 성공해야 정렬이 완료된다. compile/runtime graph도 확인하여 테스트에서만 숨기는 불일치를 만들지 않는다.

전역 catalog ref, 프로젝트 버전, 생산 코드 API, gRPC 설정, 모듈 등록 체계는 변경하지 않는다. Ignite2 복원, 새 외부 의존성, 캐시 리팩터링은 제외한다. core와 Hibernate Lettuce의 생산 의존 관계도 추가하지 않는다.

기존 테스트의 구버전 우회 설정이 정렬 후 실패하면 원인을 먼저 확인한다. 테스트 fixture의 불필요한 우회 제거는 이 범위에 포함하되, 제품 동작 수정이나 전역 버전 변경이 필요하면 설계를 재검토한다.

## 실패 모드와 대응

| 실패 모드 | 탐지 | 대응 |
|---|---|---|
| 구버전 강제로 테스트가 통과해 수정 검증처럼 보임 | resolved graph와 선택 이유 | 두 build 정렬 후 전체 검증 재실행 |
| executor가 EMF와 무관하여 회귀 조건을 놓침 | constructor 의존과 기본 bootstrap 검토 | upstream 구조를 보존한 caller 설정 사용 |
| 넓은 scan이 기존 cache·Redis fixture를 로드함 | bean 목록·container 시작 여부 | 전용 package와 명시적 자동 구성 |
| property가 Projects 활성 조건이라는 잘못된 가정 | true/false 실제 스레드 검증 | Boot 경로와 명시적 Projects 등록 경로 분리 |
| 테스트 완료 뒤 executor 또는 DB 자원 누수 | 종료 상태와 제출 거부 | context 소유권과 제한 시간 있는 정리 |
| 정렬 후 기존 JPA·demo 동작 퇴행 | 순차 전체 모듈 테스트 | fixture/graph 원인 진단; 제품 수정 필요 시 중단 |

## 수락 기준과 검증

| ID | 완료 조건 | 증거 |
|---|---|---|
| AC-01 | catalog ref와 실제 의존성 그래프 일치 | 두 모듈 dependencyInsight, 선택 이유, override 유무 |
| AC-02 | 기본 JPA context와 Projects executor 사용 가능 | 실제 repository 저장/조회와 executor 실행 결과 |
| AC-03 | EMF 의존 caller 설정의 context 시작 및 back-off | 기본 bootstrap, 단일 caller bean, Projects 기본 bean 부재 |
| AC-04 | Boot property와 Projects 명시 등록 구분 | 위 경로별 true/false와 실제 스레드 결과 |
| AC-05 | context 종료 후 자원 정리 | EMF 닫힘, Hikari pool 닫힘, 지정 executor 제출 거부, 단계별 시간 assertion·interrupt와 모든 검증 실행의 외부 프로세스 상한 |
| AC-06 | 기존 동작 유지 | core → hibernate-lettuce → demo 전체 테스트, 실패·오류·제외 수 |
| AC-07 | 빌드·정적 검증 및 전달 근거 | compile, 가능한 diagnostics, detekt, diff check, exact-head CI와 리뷰 |

먼저 신규 fixture와 기존 controller lifecycle 테스트를 실행하고, 이어 세 모듈 전체 테스트를 순차 실행한다. Testcontainers·실제 DB·JNI 검증은 다른 모듈이나 worktree와 병렬 실행하지 않는다.

이 작업은 테스트 추가이므로 단순히 테스트가 없는 상태를 RED로 보고하지 않는다. 의존성 계약의 기존 `4.0.3` 선택과 실패 assertion을 먼저 확인하고, 실제 동작 fixture가 위반을 검출하는 음성 대조를 둔다. `4.1.0` 비교 실행을 수행한다면 임시 진단으로만 취급하고 최종 catalog 정렬 상태로 복구한다. 실행하지 않은 구버전 비교나 직접 재현을 했다고 쓰지 않는다.

두 증거를 혼동하지 않는다. `4.0.3`은 catalog 불일치의 RED이며, `4.1.0`에서 발생한 순환 의존 회귀의 pre-fix RED가 아니다. 동작 assertion의 음성 대조는 기본 executor 구성 누락, 스레드 종류 불일치, 닫히지 않은 자원 상태를 각 assertion이 실패로 판정하는지 확인하는 데 한정한다. 이것도 upstream 구버전 재현 증거와 구분한다. 구버전 비교를 실행하는 경우에만 동일 fixture와 호환 BOM을 사용하는 `4.1.0`의 `BeanCurrentlyInCreationException` 및 `4.1.1`의 정상 시작을 쌍으로 기록한다.

## 명세 DoD와 다음 경계

명세는 문제·근거·대안·계약·실패 모드·호환성·수락 기준을 포함한다.
6개 관점 리뷰와 통합 검토에서 P0/P1을 해소한 뒤 사용자가 작성된 명세를 확인한다.
그다음 구현 계획을 작성·리뷰·커밋하고 구현한다. 지금은 생산 코드와 테스트 코드 변경 전이다.

전체 완료는 수락 기준 충족, lesson 커밋, 승인된 PR의 exact-head CI와 리뷰까지다.
병합 및 정리는 별도 승인 후 검증한다. 외부 조사 자료의 wiki 보존·색인도 전달 전에 완료하며 현재는 대기 상태다.
