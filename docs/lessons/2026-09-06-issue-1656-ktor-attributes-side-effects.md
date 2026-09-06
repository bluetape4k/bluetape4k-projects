# #1656: Ktor Attributes 경쟁과 lifecycle side effect를 분리한다

## 배경

Ktor plugin과 애플리케이션 시작 코드가 만든 client, executor, scheduler 같은 리소스는
`Application` lifecycle에 맞춰 닫혀야 한다. 개별 plugin마다 `ApplicationStopped`를 직접
구독하면 중복 종료, 종료 순서 불일치, failure handling 누락이 반복된다.

공통 registry를 `Application.attributes.computeIfAbsent`로 설치하는 발상만으로는 충분하지
않다. supplier는 경쟁 중 둘 이상 평가될 수 있으므로, supplier 안에서 event를 구독하면
선택되지 않은 값도 subscription이라는 side effect를 남길 수 있다.

## 결정

- attribute supplier는 side effect 없는 private holder만 생성한다.
- attribute에서 선택된 holder의 `install()`만 `ApplicationStopped`를 한 번 구독한다.
- holder의 상태와 subscription handle은 같은 lock으로 직렬화한다.
- registry는 등록 역순 종료, 조기 token 종료, 늦은 등록의 즉시 종료를 하나의 claim 규칙으로
  처리한다.
- close action은 caller thread에서 동기적으로 실행한다. coroutine scope, dispatcher,
  timeout, retry, backend drain 정책은 registry가 소유하지 않는다.
- report와 logger에는 opaque registration ID, phase, fatal 여부만 남긴다. resource 문자열,
  예외 message, cause, class 이름은 노출하지 않는다.

## 실패와 예상 밖의 점

첫 구현은 테스트 편의를 위해 holder와 registrar seam을 `internal`로 두었다. Kotlin의
`internal`은 JVM bytecode에서 package-private가 아니므로 Java 소비자에게 public
implementation class로 보였다. production seam을 public ABI로 만드는 대신 테스트에서
private holder를 reflection으로 호출하고, `javap`과 외부 package 테스트로 노출면을 고정했다.

또한 Ktor application job의 disposal timeout과 `ApplicationStopped` handler의 실행 제한이
같다고 가정하면 안 된다. 실제 short-timeout fixture에서 application child가 timeout 뒤에도
정리 중인 상태를 만들고, bounded adapter가 drain을 끝낸 뒤 resource를 닫도록 검증했다.

## 결과

`installApplicationResourceLifecycle()`은 동시 호출에서도 같은 registry를 반환하며 event
subscription을 한 번만 만든다. registry와 token의 종료가 경합해도 항목별 close는 최대 한
번 실행된다. `OPEN`, `DRAINING`, `CLOSED` 중 어느 시점에 report를 읽어도
`attempted == inFlight + closed + failures.size`가 유지된다.

implementation holder는 JVM package-private이고 함수형 registrar를 위한 별도 class도
생성되지 않는다. public registration token에는 Kotlin compiler의 synthetic bridge만 남으며,
Java source가 호출할 수 있는 non-synthetic public constructor는 없다.

## 검증 근거

| 주장 | 근거 | 결과 |
|---|---|---|
| attribute supplier가 side effect를 만들지 않음 | private holder 생성과 winner `install()` 분리 | 동시 installer와 callback-before-handle 테스트 통과 |
| 종료가 정확히 한 번 실행됨 | token/registry barrier 경합과 중복 stop callback | 각 resource와 subscription dispose 1회 |
| disposal timeout 뒤 drain 경계 | `shutdownTimeout = 1L`인 `testApplication` fixture | child 활성 상태를 관찰한 뒤 drain 완료 후 resource close |
| 실패 정보 비노출 | registry/Ktor logger appender, fatal marker, secret resource fixture | message, cause, class, `toString()` sentinel 미검출 |
| public ABI 제한 | `javap -public`, class modifier, 외부 package 소비 테스트 | holder `ACC_PUBLIC` 없음, non-synthetic public token constructor 없음 |
| publication 계약 유지 | POM/module metadata validator와 설정 diff | validator failures=0, 직접 dependency 12개, 설정 diff 0 |
| 모듈 동작 | `:bluetape4k-ktor-core:check`, Kover, detekt | 39 tests, skipped/failures/errors 0 |

구현 기준 commit은 `f41fea0f5`다. Ktor의 `AttributesJvm` 구현과 event subscription 반환
handle을 실제 소비 JAR source에서 확인했으며, 라이브러리 내부 구현을 public API 계약으로
승격하지 않았다.

## Review Miss

초기 검토 계획은 registry의 역순·멱등성에 집중해 JVM visibility와 Ktor disposal timeout 뒤
실행 순서를 별도 fixture로 요구하지 않았다. 독립 구현 검토가 이 두 항목과 logger backend
자체의 실패 경로를 P1으로 찾아냈다. 이후 ABI 검사, short-timeout fixture, fatal/ordinary
failure 및 throwing appender matrix를 추가했다.

## 다음 변경 시 지킬 점

1. `Attributes.computeIfAbsent` supplier에는 subscription, thread 시작, resource allocation 같은
   side effect를 넣지 않는다.
2. Kotlin `internal`을 Java/Kotlin 소비자 모두에게 비공개인 JVM 경계로 표현하지 않는다.
3. lifecycle timeout, event dispatch, resource drain을 하나의 제한 시간으로 가정하지 않는다.
4. 동시성 report는 완료 결과뿐 아니라 `DRAINING`과 늦은 close의 `inFlight` 상태에서도 invariant를
   검증한다.
5. 실패 격리는 resource action뿐 아니라 logger와 subscription disposal이 던지는 경우까지
   포함한다.
