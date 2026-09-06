# Issue #1656: Ktor 애플리케이션 소유 리소스 lifecycle 공통화 설계

## 상태와 범위

- 대상 저장소: `bluetape4k-projects`
- 대상 모듈: `bluetape4k-ktor-core`
- 대상 버전: `2.1.0`
- 작업 유형: Type A 공개 API 추가
- 기준 ref: `origin/develop` / `8e5ff93d71faa372efaf1012088acc9e480bf04e`
- 연결 이슈: [#1656](https://github.com/bluetape4k/bluetape4k-projects/issues/1656)
- 비교 저장소: `bluetape4k-graph`, `bluetape4k-leader`, `bluetape4k-aws`
- 상태: 2026-09-06 사용자 최종 승인, 명세 P0/P1 해소

이번 변경은 Ktor application이 소유한 리소스를 application 종료와 연결하는 최소 registry를
`ktor/core`에 추가한다. Graph, Leader, AWS의 실제 전환은 공통 API가 배포된 뒤 별도
adoption 이슈와 PR에서 수행한다.

## 1. 문제와 목표

Graph, Leader, AWS의 Ktor plugin은 application 소유 리소스의 등록과 종료를 각자
구현한다. 구현마다 idempotent close, 종료 역순, late registration, 일부 실패 격리,
reentrant close 보장이 달라 공통 동작을 반복해서 검증해야 한다.

목표는 다음과 같다.

1. application마다 하나의 resource registry를 명시적으로 설치한다.
2. 등록 token과 registry 종료를 하나의 선형화 경계로 직렬화한다.
3. registry를 여러 번 닫아도 각 등록 항목을 최대 한 번만 닫는다.
4. 정상 종료에서는 등록의 역순으로 리소스를 닫는다.
5. registry 종료 뒤 들어온 late registration은 즉시 닫는다.
6. resource close가 registry나 다른 등록 token으로 재진입해도 deadlock이 발생하지 않는다.
7. 한 항목의 close 실패가 나머지 항목의 종료를 막지 않는다.
8. 정상 종료에 새 thread, dispatcher, coroutine scope를 만들지 않는다.
9. 종료 실패를 resource 정보 없이 opaque registration ID와 close phase로 진단할 수 있다.

## 2. 범위와 비범위

### 2.1 포함 범위

- `ApplicationResourceRegistry`의 동기식 등록·종료 계약
- `AutoCloseable`과 동기식 close action 등록
- 개별 등록을 조기에 종료하는 idempotent registration token
- Ktor `ApplicationStopped` event와 registry를 연결하는 명시적 installer
- 종료 대상 목록의 최소 성공·실패 집계
- lifecycle, 동시성, 실패 격리, coroutine cancellation 회귀 테스트
- 공개 API 한국어 KDoc과 영문·국문 README 사용 예

### 2.2 제외 범위

- suspend cleanup, `Job.join`, 비동기 close 완료 대기
- registry가 직접 제공하는 timeout, retry, backoff, dispatcher와 coroutine scope 소유권
- backend client 생성, 소유권 판정, health/readiness와 shutdown 순서 정책
- exception message, credential, resource 내부 식별자를 포함한 상세 report
- Graph·Leader의 신규 production dependency와 publication POM 변경
- Graph·Leader·AWS의 실제 plugin 전환
- JVM 전체 shutdown hook이나 전역 registry

Leader의 bounded `Job` await와 상세 shutdown report는 Leader adapter에 남긴다. AWS의
`MonitoringEvent` 동기 callback과 backend별 ownership, timeout bridge도 AWS adapter가
소유한다. 공통 registry는 이 정책을 흡수하지 않는다. 대신 등록 대상은 application 시작
경로가 소유하는 trusted resource/action으로 제한하고, 각 close는 caller 또는 adapter가
반드시 유한한 시간 안에 동기적으로 끝나도록 보장해야 한다. request나 외부 입력으로 close
action을 동적 등록하는 사용은 지원하지 않는다.

## 3. 현재 근거와 제약

- `bluetape4k-ktor-core`는 이미 `bluetape4k-core`와 Ktor server core를 `api` dependency로
  사용한다. 새 module이나 외부 dependency는 필요하지 않다.
- 기존 `ShutdownQueue`는 JVM 전역이라 application별 격리와 late/reentrant registration
  계약을 제공하지 못한다.
- `AutoCloseable.closeSafe`는 close 실패를 항목별로 격리할 수 있다.
  `closeTimeout`은 별도 비동기 실행을 만들기 때문에 공통 정상 종료 경로에서는 사용하지
  않고 bounded wait가 필요한 domain adapter에 남긴다.
- Ktor 공식 문서는 `ApplicationStopped`를 application resource 해제 지점으로 안내한다.
- Ktor 3.5.2 JVM engine은 `ApplicationStopping`을 발생시킨 뒤 application job을
  cancel/join하고 plugin을 제거하려고 시도한 다음 `ApplicationStopped`를 발생시킨다.
  `disposeAndJoin()`이 engine `shutdownTimeout`을 넘겨도 engine은 예외를 격리하고
  `ApplicationStopped`를 발생시키므로, 이 event는 모든 application coroutine이 실제로
  종료됐다는 증거가 아니다.
- `MonitoringEvent` handler는 suspend lambda가 아닌 동기식 `(Param) -> Unit`이다. 공통
  registry의 close 경로도 동기식으로 유지한다.
- `ApplicationStopped` handler 실행은 engine의 `disposeAndJoin()` timeout 바깥에 있다.
  따라서 registry는 engine timeout에 기대지 않고 등록 action 자체의 bounded completion을
  공개 선행조건으로 둔다.

참고 자료:

- [Ktor application monitoring](https://ktor.io/docs/server-events.html)
- [Ktor 3.5.2 `EmbeddedServerJvm`](https://github.com/ktorio/ktor/blob/3.5.2/ktor-server/ktor-server-core/jvm/src/io/ktor/server/engine/EmbeddedServerJvm.kt#L293-L318)
- [Ktor 3.5.2 `MonitoringEvent`](https://github.com/ktorio/ktor/blob/3.5.2/ktor-server/ktor-server-core/common/src/io/ktor/server/application/hooks/CommonHooks.kt#L50-L68)
- [Ktor 3.5.2 `AttributesJvm.computeIfAbsent`](https://github.com/ktorio/ktor/blob/3.5.2/ktor-utils/jvm/src/io/ktor/util/AttributesJvm.kt#L38-L51)

## 4. 대안 비교와 결정

| 대안 | 장점 | 단점 | 결정 |
|---|---|---|---|
| 각 plugin의 lifecycle 유지 | dependency와 API 변화가 없음 | 중복 구현과 서로 다른 종료 보장이 계속됨 | 채택하지 않음 |
| application attribute를 처음 사용할 때 lazy 등록 | 호출 코드가 짧음 | lifecycle 설치 시점이 숨겨지고 시작 이후 첫 접근을 안전하게 설명하기 어려움 | 채택하지 않음 |
| suspend registry가 `Job`, timeout, report를 모두 소유 | 한 abstraction에서 상세 종료를 수행 | Leader 정책을 중복하고 새 scope/thread 및 backend별 정책을 공통 모듈에 끌어들임 | 채택하지 않음 |
| 명시적 Ktor installer와 동기식 최소 registry | 설치·소유권이 드러나고 기존 adapter 정책을 보존하며 공통 불변식만 재사용 | suspend cleanup은 adapter가 별도로 연결해야 함 | **채택** |

## 5. 공개 API 계약

`io.bluetape4k.ktor.core` package에 다음 API를 추가한다. 세부 이름은 구현 중 compile
검증에서 Ktor API 충돌이 확인되면 의미를 유지한 채 조정할 수 있으며, 명세 변경으로
기록한다.

```kotlin
enum class ApplicationResourceClosePhase {
    EARLY,
    SHUTDOWN,
    LATE_REGISTRATION,
}

enum class ApplicationResourceRegistryState {
    OPEN,
    DRAINING,
    CLOSED,
}

data class ApplicationResourceCloseFailure(
    val registrationId: Long,
    val phase: ApplicationResourceClosePhase,
    val fatal: Boolean,
)

data class ApplicationResourceCloseReport(
    val state: ApplicationResourceRegistryState,
    val attempted: Int,
    val inFlight: Int,
    val closed: Int,
    val failures: List<ApplicationResourceCloseFailure>,
)

class ApplicationResourceRegistration internal constructor(...) : AutoCloseable {
    val id: Long
    override fun close()
}

class ApplicationResourceRegistry : AutoCloseable {
    fun register(resource: AutoCloseable): ApplicationResourceRegistration
    fun register(closeAction: () -> Unit): ApplicationResourceRegistration
    val closeReport: ApplicationResourceCloseReport
    override fun close()
}

fun Application.installApplicationResourceLifecycle(): ApplicationResourceRegistry
```

계약은 다음과 같다.

- installer는 application마다 registry를 정확히 하나 설치하고 반환한다.
- 같은 application에서 installer를 다시 호출하면 기존 registry를 반환한다.
- `register`는 해당 항목을 조기에 닫을 수 있는 registration token을 반환한다. token의
  양수 `id`는 registry 안에서만 단조 증가하는 opaque 값이며 resource 정보로부터 파생하지
  않는다.
- token과 registry는 경합하더라도 항목을 최대 한 번만 claim하고 닫는다.
- token close는 registry에서 항목을 제거한 뒤 실제 close를 lock 밖에서 수행한다.
- 동일 `AutoCloseable` 또는 동일 close-action 객체의 중복 등록은 registry lifetime 동안
  identity 기준으로 거부한다. `IllegalArgumentException` message에는 resource 문자열이나
  class 이름을 넣지 않는다. 서로 다른 lambda가 같은 내부 resource를 capture하는 경우는
  탐지할 수 없으므로 caller가 단일 ownership을 지켜야 한다.
- registry close는 아직 claim되지 않은 항목의 역순 종료 대상 목록을 만든 뒤 실제 close를 lock
  밖에서 수행한다.
- registry close가 시작된 뒤의 등록은 항목을 보관하지 않고 호출 thread에서 즉시 닫으며
  no-op registration token을 반환한다.
- `closeReport`는 property를 읽는 순간의 registry state와 `EARLY`, `SHUTDOWN`,
  `LATE_REGISTRATION` close 결과를 한 lock 안에서 복사한 immutable value다. close claim 시
  `attempted`와 `inFlight`를 함께 올리고 완료 시 `inFlight`를 내리면서 `closed` 또는
  `failures`를 갱신하므로, close 진행 중에도 `attempted == inFlight + closed +
  failures.size`를 만족한다. 초기값은 `OPEN/0/0/0/emptyList()`다.
- 최초 registry close는 lock 안에서 `OPEN -> DRAINING`으로 전환하고 종료 대상 목록을
  분리한다. 모든 shutdown 항목의 결과를 반영한 뒤 `CLOSED`로 전환한다. `DRAINING` 중
  재진입한 registry close는 no-op이고 새 등록은 `LATE_REGISTRATION`으로 즉시 처리한다.
  `CLOSED` 뒤에도 late close가 진행되는 동안 `inFlight > 0`일 수 있으며, 그 결과는 같은
  누적 report의 다음 immutable copy에 보인다.
- 일반 failure와 JVM-fatal `Error`는 모두 나머지 cleanup을 막지 않는다. fatal 여부는 report와
  숫자 기반 log에 남기고 모든 claim 항목을 시도한 뒤 원본 message/cause가 없는 sanitized
  fatal marker를 다시 던진다. 순수 registry를 직접 닫는 caller에는 marker가 전달된다.
  installer 경로에서는 Ktor `safeRaiseEvent`가 marker를 engine 밖으로 전파하지 않으므로,
  sanitized warning과 report가 실제 운영 신호다. 이로써 치명 실패를 일반 실패와 구분하면서
  credential이나 resource 내부 정보가 Ktor log로 전파되지 않게 한다.
- `close()`는 순수 registry를 자체 `MonitoringEvent` bridge에서 재사용할 수 있도록 공개한다.
  installer가 반환한 registry의 close ownership은 installer에만 있으며 caller가 직접
  `close()`하거나 다른 종료 bridge와 동시에 연결해서는 안 된다.

## 6. Ktor lifecycle 계약

1. caller는 application module 초기화에서 installer를 명시적으로 호출한다.
2. Ktor 3.5.2의 `Attributes.computeIfAbsent` supplier는 경합 시 여러 번 평가될 수 있으므로
   supplier에서는 side effect 없는 lifecycle holder만 만든다. attribute winner holder가
   자체 lock과 `NEW -> READY` 또는 `NEW -> FAILED` 상태 전이에서 event subscription을 정확히
   한 번 초기화한다. 내부 subscription registrar seam은 Kotlin `internal`로 제한하고 public
   installer는 Ktor monitor registrar만 주입한다. holder는 subscribe 호출과 handle publication을
   같은 lock 안에서 수행하고 callback도 같은 lock에서 handle ownership을 claim한다. Ktor
   `Events.subscribe` 구현은 handler를 등록할 뿐 callback을 동기 호출하지 않으므로, 다른 thread에서 handler가 handle
   반환 전에 실행돼도 holder lock에서 대기한 뒤 publication된 handle을 정확히 한 번 claim한다.
   fake registrar test는 raw subscribe 횟수와 handle dispose 횟수를 callback/registry close
   횟수와 별도로 관찰한다.
3. subscribe가 throw하면 holder는 registry를 닫고 `FAILED`로 고정한다. 최초 호출과 이후 모든
   호출은 원본 message/cause/suppressed가 없는 동일 type/message의 sanitized installation
   exception을 던지며 재시도하지 않는다. Ktor registrar는 handle 반환 뒤 fallible 작업을 하지
   않으므로 installation rollback에 아직 획득한 handle이 남는 단계가 없다. 반환된 handle의
   dispose는 `ApplicationStopped` callback에서 정확히 한 번 시도하고 dispose 실패도 원본
   Throwable 없이 기록하며 registry close 결과를 되돌리지 않는다.
4. subscription은 `ApplicationStopped`에서 registry를 동기적으로 닫고 `finally`에서 자기
   자신을 dispose한다. plugin uninstall 뒤에 발생하는 event를 쓰므로 custom plugin
   instance의 `AutoCloseable` 또는 uninstall 순서에 기대지 않는다.
5. public API는 다른 lifecycle event를 선택하는 configuration을 제공하지 않는다.
6. `ApplicationStopping`에서 suspend cleanup이 필요한 adapter는 순수 registry와 자체
   `MonitoringEvent` 동기 bridge를 사용한다. installer와 custom bridge를 같은 registry에
   중복 연결하지 않는다.
7. application coroutine `Job`을 공통 registry가 직접 await하지 않는다. 독립 `Job`의
   취소만 필요하면 caller가 `register { job.cancel() }`을 사용한다.
8. application stop 이후에도 외부 코드가 보유한 registry reference로 register할 수
   있으나, 해당 action은 즉시 실행된다.
9. `ApplicationStopped` 전에 Ktor disposal timeout이 발생하면 application `Job`이 아직
   끝나지 않았을 수 있다. resource를 사용하는 background job은 adapter가
   `ApplicationStopping`에서 bounded drain하거나, cancellation을 무시한 job이 resource에
   접근하지 않는다는 조건을 보장해야 한다.
10. 서로 다른 Ktor plugin의 event handler 실행 순서는 이 registry가 보장하지 않는다.
   의존 리소스는 하나의 composite action으로 묶거나 한 registry에 의존 순서대로 등록한다.

사용 예는 다음과 같다.

```kotlin
fun Application.module() {
    val resources = installApplicationResourceLifecycle()
    val client = createClient()

    resources.register(client)
}
```

## 7. 동시성과 상태 전이

registry는 하나의 `ReentrantLock`, ordered entry list, state와 report counter를 사용한다. 실제
resource close는 lock 안에서 호출하지 않는다.

| 현재 상태 | 동작 | 결과 |
|---|---|---|
| OPEN | resource/action 등록 | list 뒤에 추가하고 token 반환 |
| OPEN | 동일 객체 재등록 | 정보 노출 없는 `IllegalArgumentException` |
| OPEN | token close | entry claim·제거 후 lock 밖에서 즉시 close |
| OPEN | registry close | DRAINING으로 전환하고 역순 종료 대상 목록을 분리 |
| DRAINING | registry close | no-op, 진행 중 report 유지 |
| DRAINING/CLOSED | 새 객체 register | lock 밖에서 즉시 close하고 no-op token 반환 |
| DRAINING/CLOSED | 기존 identity 재등록 | 정보 노출 없는 `IllegalArgumentException` |
| DRAINING | shutdown 대상 완료 | 마지막 항목 완료 뒤 CLOSED로 전환 |
| 임의 close 실행 중 | registry/token/register 재진입 | lock을 보유하지 않으므로 독립 상태 전이를 수행 |

동시에 시작한 register와 close 중 lock을 먼저 획득한 연산이 선형화 지점이 된다. register가
먼저면 종료 대상 목록에 포함되고, close가 먼저면 late registration으로 즉시 닫힌다. 두
경우 모두 항목은 최대 한 번 닫힌다.

installer를 사용한 registry는 lifecycle bridge가 단일 close owner다. custom bridge가 필요한
adapter는 installer를 사용하지 않고 순수 registry를 정확히 하나의 event에 연결한다. 서로
다른 thread에서 registry close를 동시에 소유하는 구성은 지원하지 않으며, API/KDoc과
adoption checklist가 이를 금지한다.

## 8. 실패, 운영, 보안 계약

- 각 resource/action은 `closeSafe`를 통해 독립적으로 닫고 error handler 자체는 절대
  exception을 던지지 않는다.
- 실패는 `failures`에 집계하고 다음 항목의 close를 계속한다.
- `closed`는 예외 없이 종료된 항목 수이며 close 진행 중에도
  `attempted == inFlight + closed + failures.size`를 만족한다.
- failure는 opaque `registrationId`, `phase`, `fatal`만 포함한다. exception instance,
  message, stack trace, resource `toString`, resource/class/name은 담지 않는다.
- registry log는 failure마다 `registrationId`, `phase`, `fatal`만 warning으로 남기며 원본
  `Throwable`을 logger에 전달하지 않는다. late failure도 `LATE_REGISTRATION`으로 report와
  log에 함께 남는다.
- caller가 등록한 close action은 application-startup code가 소유하는 trusted 작업이어야
  하고 유한한 시간 안에 동기적으로 끝나야 한다. Ktor engine timeout은 이 callback에
  적용되지 않으며 registry도 강제로 중단하지 않는다. backend wait가 있으면 adapter가
  caller-owned timeout/deadline으로 bounded completion을 먼저 제공해야 한다.
- `closeSafe`가 `Throwable`을 격리하는 현재 계약을 재사용하되 fatal 분류를 보존한다.
  cancellation을 신호로 사용해야 하는 suspend adapter는 이 동기 경계 밖에서 cancellation
  우선순위를 보존한다.
- close가 끝나지 않는 동안 registry는 완료 report를 만들 수 없다. 운영 환경은 server
  shutdown deadline과 process termination 정책으로 hung adapter를 탐지하며, registry는
  이를 대신하지 않는다.

## 9. 테스트 전략

### 9.1 Registry 단위 테스트

- registry를 두 번 닫아도 각 항목이 한 번만 닫힘
- registration token과 registry close가 경합해도 한 번만 닫힘
- 세 항목이 등록 역순으로 닫힘
- token으로 먼저 닫은 항목은 registry 종료 대상 목록에서 제외됨
- 동일 resource/action 객체의 중복 등록은 identity 기준으로 거부됨
- 종료 후 등록은 즉시 닫히고 no-op token을 반환함
- resource close 안에서 registry close를 다시 호출해도 deadlock이 없음
- resource close 안에서 새 resource를 등록하면 late registration으로 즉시 닫힘
- 중간 항목이 실패해도 앞뒤 항목이 계속 닫히고 EARLY/SHUTDOWN/LATE report가 정확함
- close 전 report는 `OPEN/0/0/0/emptyList()`이고, close action을 latch로 멈춘 동안 읽은
  report는 `DRAINING`, `inFlight > 0`, counter invariant를 만족함
- early close만 수행한 report와 shutdown 완료 후 `CLOSED/inFlight=0` report가 정확함
- opaque ID와 phase로 실패를 구분하며 exception message와 resource 문자열이 report/log에
  포함되지 않음
- fatal `Error`가 남은 cleanup 뒤 sanitized marker로 전달되고 원본 message/cause는 노출되지 않음
- secret sentinel이 registry report/logger, Ktor environment logger, marker의
  message/cause/suppressed 어느 곳에도 남지 않음
- logging backend가 실패해도 report 전이와 나머지 cleanup이 계속됨
- `register { job.cancel() }`로 독립 coroutine `Job`이 취소됨
- 반복 경합 테스트에서 double close와 미종료 항목이 없음
- finite close action이 JUnit `assertTimeout` 안에서 caller thread와 같은 thread에서 끝남

경합 테스트는 latch/barrier로 선형화 경계를 통제하고 무작위 sleep에 의존하지 않는다.

### 9.2 Ktor 통합 테스트

- installer를 반복 호출해도 동일 registry를 반환함
- `testApplication` 종료가 `ApplicationStopped` hook을 통해 등록 resource를 닫음
- 정상 disposal fixture에서는 application coroutine이 취소·join된 뒤 resource close
  action이 실행됨
- 별도 short-timeout embedded-engine fixture에서는 disposal timeout 뒤에도
  `ApplicationStopped`가 발생함을 확인하고, adapter가 background job을 먼저 bounded
  drain하여 resource close와 동시 접근하지 않음을 latch로 검증함
- 일부 close 실패가 application stop 자체와 나머지 resource cleanup을 막지 않음
- installer 경로의 fatal failure가 sanitized warning/report로 남고 engine stop은 계속되며,
  원본 message/cause가 log에 없음을 검증함
- installer 반복·동시 호출이 side-effect-free attribute supplier와 winner holder 초기화 경계를
  통해 단일 registry와 단일 subscription만 생성함
- fake registrar가 raw subscribe 1회와 callback 뒤 handle dispose 1회를 직접 보고하며,
  registry close 횟수와 별도로 assertion됨
- fake registrar가 handler를 공개한 뒤 handle 반환 직전 latch에서 멈추고 다른 thread가 callback을
  시작하는 경합에서도 publication 뒤 handle dispose가 정확히 한 번 수행됨
- subscribe failure 뒤 raw subscribe가 1회에서 멈추고 최초/후속 호출이 같은 sanitized
  type/message와 빈 cause/suppressed를 반환하며 registry를 정상 반환하지 않음
- handle dispose failure가 registry report를 되돌리거나 callback을 재설치하지 않고 원본
  failure 정보를 log/marker에 노출하지 않음
- `io.bluetape4k.ktor.core.consumer.ApplicationResourceLifecyclePublicApiTest`가 외부 package에서
  installer, registry, registration, report public API를 compile하고 기본 사용 예를 실행함

### 9.3 검증 명령

```bash
./gradlew :bluetape4k-ktor-core:test --no-build-cache --rerun-tasks
./gradlew :bluetape4k-ktor-core:check --no-build-cache --rerun-tasks
./gradlew :bluetape4k-ktor-core:detekt --no-build-cache --rerun-tasks
./gradlew :bluetape4k-ktor-core:jar \
  :bluetape4k-ktor-core:compileTestKotlin \
  :bluetape4k-ktor-core:generatePomFileForBluetape4kPublication \
  :bluetape4k-ktor-core:generateMetadataFileForBluetape4kPublication \
  :bluetape4k-ktor-core:checkPomFileForBluetape4kPublication
ruby scripts/publication/validate_poms.rb \
  ktor/core/build/publications/Bluetape4k/pom-default.xml
ruby scripts/publication/validate_module_metadata.rb \
  ktor/core/build/publications/Bluetape4k/module.json
mkdir -p ktor/core/build/reports/api
javap -public -classpath ktor/core/build/classes/kotlin/main \
  io.bluetape4k.ktor.core.ApplicationResourceClosePhase \
  io.bluetape4k.ktor.core.ApplicationResourceRegistryState \
  io.bluetape4k.ktor.core.ApplicationResourceCloseFailure \
  io.bluetape4k.ktor.core.ApplicationResourceRegistry \
  io.bluetape4k.ktor.core.ApplicationResourceRegistration \
  io.bluetape4k.ktor.core.ApplicationResourceCloseReport \
  io.bluetape4k.ktor.core.ApplicationResourceLifecycleKt \
  > ktor/core/build/reports/api/application-resource-lifecycle-javap.txt
diff -u \
  ktor/core/src/test/resources/abi/application-resource-lifecycle-javap.txt \
  ktor/core/build/reports/api/application-resource-lifecycle-javap.txt
ruby -r rexml/document -e '
  document = REXML::Document.new(File.read(ARGV.fetch(0)))
  actual = REXML::XPath.match(document, "/project/dependencies/dependency").map do |node|
    [node.elements["groupId"].text, node.elements["artifactId"].text, node.elements["scope"].text].join(":")
  end
  expected = %w[
    org.jetbrains:annotations:compile org.slf4j:slf4j-api:compile
    io.github.bluetape4k:bluetape4k-core:compile io.ktor:ktor-server-core-jvm:compile
    io.ktor:ktor-server-content-negotiation:compile io.ktor:ktor-server-status-pages:compile
    io.ktor:ktor-serialization-kotlinx-json:compile
    org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:compile
    org.jetbrains.kotlin:kotlin-stdlib:runtime org.jetbrains.kotlin:kotlin-reflect:runtime
    org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:runtime
    org.jetbrains.kotlinx:atomicfu-jvm:runtime
  ]
  abort("unexpected ktor-core POM dependency set") unless actual == expected
  puts "ktor-core-pom-baseline: dependencies=#{actual.length}"
' ktor/core/build/publications/Bluetape4k/pom-default.xml
git diff --exit-code origin/develop -- \
  ktor/core/build.gradle.kts gradle/libs.versions.toml settings.gradle.kts
if rg -n 'CoroutineScope|GlobalScope|Dispatchers|newSingleThreadContext|newFixedThreadPoolContext|asyncRunWithTimeout|closeTimeout|CompletableFuture|ForkJoinPool|Executors|Executor|kotlin\.concurrent\.thread|runBlocking|Timer\(|Thread\(|Thread\.ofVirtual|Thread\.startVirtualThread' \
  ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/ApplicationResourceLifecycle.kt; then
  exit 1
else
  BT4K_RG_EXIT=$?
  test "$BT4K_RG_EXIT" -eq 1
fi
git diff --check
```

이 repository에는 해당 module용 binary-compatibility task가 없다. 따라서 외부 package의
compile fixture와 review된 golden file에 대한 `javap -public` exact diff로 공개 signature를
검증한다. generated POM/module metadata validator와 기준 ref에서 확인한 12개 dependency의
정확한 순서·scope 비교로 publication 계약을 검증한다. dependency 파일은 `origin/develop`과
동일해야 한다. workflow/catalog/module registration은 변경하지 않으므로 관련 diff가 생기지
않았는지 함께 검토한다.

production 구현은 `ApplicationResourceLifecycle.kt` 한 파일에 둔다. 구현 중 production 파일을
분리해야 한다면 위 비동기 primitive source audit도 `ktor/core/src/main/kotlin` 아래 이번 변경의
모든 production 파일을 대상으로 확장하고, no-match `rg` exit code 1만 성공으로 인정한다.

## 10. 문서, 호환성, adoption

- 새 public class, property, function에 실제 동작과 제한을 설명하는 한국어 KDoc을 작성한다.
- `ktor/core/README.md`와 `README.ko.md`에 explicit installation, ownership, stop timing,
  bounded synchronous close 선행조건, opaque failure 진단, disposal-timeout caveat를 같은
  의미로 기록한다.
- 이 계약은 graceful shutdown 전용이다. `SIGKILL`, JVM crash, OOM처럼
  `ApplicationStopped`가 발생하지 않는 강제 종료에서는 cleanup을 보장하지 않는다고
  README와 KDoc에 명시한다.
- 기존 `installBluetape4kKtorCore()`의 signature와 기본 동작은 바꾸지 않는다.
- 새 기능은 additive API이며 기존 Ktor application에 자동 설치되지 않는다.
- 새 dependency와 module은 추가하지 않는다.
- projects PR에서 Graph, Leader, AWS의 adoption 범위와 dependency/POM 검증을 각각 durable
  follow-up 이슈로 등록한다. adoption 구현은 projects PR merge, exact artifact publication,
  POM/module metadata 확인 뒤 시작하며 이번 이슈에 포함하지 않는다.
- publication 전 rollback은 projects PR revert다. publication 후 adoption rollback은 각
  adapter의 registry 연결만 제거하고 기존 lifecycle 구현으로 복귀한다.
- adoption checklist는 trusted startup-only registration, bounded synchronous close,
  single lifecycle bridge ownership, cross-plugin ordering 비보장을 확인해야 한다.

## 11. 수용 기준과 DoD

- [ ] 공개 API와 내부 상태 전이가 이 명세와 일치한다.
- [ ] idempotent, reverse-order, late, reentrant, partial-failure 계약이 테스트로 증명된다.
- [ ] coroutine cancellation은 action adapter 경계로 증명되고 generic bounded await는 없다.
- [ ] 정상 close 경로에 새 thread, dispatcher, coroutine scope가 없다.
- [ ] 모든 등록 action의 bounded completion 선행조건이 KDoc/README/adoption checklist에 있다.
- [ ] report와 log가 opaque ID/phase/fatal만 사용하고 credential/resource 내부 정보를 노출하지 않는다.
- [ ] Ktor disposal timeout 뒤에도 발생하는 `ApplicationStopped` 의미와 background job 제약이 검증된다.
- [ ] 영문·국문 README와 한국어 KDoc이 실제 API 이름·동작과 일치한다.
- [ ] targeted test, module check, detekt, diff check, API/POM 검증이 통과한다.
- [ ] 여섯 관점 spec/plan/code review가 각각 `P0=0, P1=0`으로 수렴한다.
- [ ] Graph·Leader·AWS adoption이 별도 이슈로 추적된다.
- [ ] PR exact-head CI, review/thread read-back, mergeability를 확인한다.

## 12. 결정되지 않은 항목

없음. installer는 Ktor custom plugin uninstall에 기대지 않고 application attribute와
`ApplicationStopped` subscription을 사용한다. 구현 중 Ktor public API 제약으로 내부 저장
형태가 바뀌더라도 명시적 설치, application별 단일 registry, 단일 event bridge, 동기 close라는
공개 계약은 유지한다.

## 13. 1차 명세 검토 반영

- Performance: P1 1건과 P2 1건에 대해 trusted bounded action 선행조건, caller-thread test,
  비동기 primitive source audit를 반영했다.
- Security: P2 3건에 대해 startup-only 등록, fatal 분류·sanitized marker, identity 중복
  거부를 반영했다.
- Operations: P1 4건과 P2 3건에 대해 disposal-timeout 의미, opaque ID/phase report, 정확한
  publication 명령, late 집계, adoption·ordering 계약을 반영했다.
- 2차 review의 Performance P2 1건은 유효한 regex와 `rg` exit code 1만 허용하는 fail-closed
  source audit로 수정했다.
- 2차 review의 Operations P1 3건은 `state/inFlight` 원자 report, 정상/timeout fixture 분리,
  `checkPomFile`·외부 package compile fixture·`javap` golden diff·12개 POM dependency
  baseline으로 수정했다. fatal marker는 installer 경로에서 Ktor가 격리하므로 sanitized
  warning/report가 운영 신호임을 명시했다.
- Stability, Developer/API, User/Caller 독립 lane은 command deadline 안에 verdict를 반환하지
  못해 main-session fallback으로 검토했다. fallback은 close ownership 단일화, installer의
  atomic installation, cumulative immutable-copy report, 재진입과 중복 ownership 제한,
  README/KDoc parity를 명세에 추가했다.
- 최종 plan review의 P1은 Ktor `Attributes.computeIfAbsent` supplier가 경합 시 여러 번 평가될
  수 있다는 점이었다. supplier는 side-effect-free holder만 만들고 winner holder의 lock/state가
  subscription을 exactly-once 초기화·rollback하도록 계약과 concurrency test를 수정했다.
- 최종 Operations 재검토의 유효한 P1 1건은 §8의 진행 중 report invariant를
  `attempted == inFlight + closed + failures.size`로 정정해 해소했다. API 예시에는 이미
  `state/inFlight`가 포함되어 있음을 exact-line read-back으로 확인했다.
- 구현과 heavy validation은 작성된 명세 승인 뒤 수행한다.
