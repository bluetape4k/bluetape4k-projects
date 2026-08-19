# #1321 PropertyExportingServer의 Spring DynamicPropertyRegistry 연동 설계

- Epic: [#1418](https://github.com/bluetape4k/bluetape4k-projects/issues/1418)
- Slot: 3/4
- Issue: [#1321](https://github.com/bluetape4k/bluetape4k-projects/issues/1321)
- 선행 슬롯: #1335, #1339
- 후속 슬롯: [#1337](https://github.com/bluetape4k/bluetape4k-projects/issues/1337)
- 기준 브랜치: `develop`의 `846a804b9287c61bbf802d0573909005e1a66f8f`
- 작업 브랜치: `feat/1418-03-dynamic-property-registry`

## 결정 요약

`PropertyExportingServer`가 이미 제공하는 `propertyNamespace`, `propertyKeys()`와
`properties()`를 Spring Test의 `DynamicPropertyRegistry`에 연결하는 선택 모듈
`bluetape4k-testcontainers-spring`을 추가한다. core `bluetape4k-testcontainers`에는
Spring 의존성을 추가하지 않으며, 기존 시스템 프로퍼티 등록 계약과 새 동적 등록 계약은
서로 다른 수명주기로 유지한다.

새 공개 API는 다음 top-level extension 하나다.

```kotlin
fun PropertyExportingServer.registerDynamicProperties(
    registry: DynamicPropertyRegistry,
)
```

이 함수는 `propertyKeys()`로 키 이름만 열거하고, 각 키를
`testcontainers.{propertyNamespace}.{key}`로 등록한다. 값 supplier는 registry가
프로퍼티를 실제로 해석할 때 `properties()`를 호출한다. 따라서 등록 시점에 값을
미리 계산하거나 시스템 프로퍼티를 변경하지 않는다.

## 배경과 현재 계약

- #1321은 `PropertyExportingServer`를 Spring `DynamicPropertyRegistry`와 연결해
  반복적인 `@DynamicPropertySource` 보일러플레이트를 줄이는 승격 후보로 등록됐다.
- core 인터페이스는 SDK-neutral Testcontainers 모듈에 있으며 Spring Test를 현재
  의존하지 않는다.
- 시스템 프로퍼티 키는 `testcontainers.{propertyNamespace}.{kebab-case-key}` 형식이다.
  `propertyKeys()`는 `start()` 전에도 이름을 반환하고, `properties()`는 서버의
  실제 연결 정보를 계산한다.
- Workshop의 기존 helper는 Redis host/port/url을 직접 `registry.add`한다. 그 helper는
  소비자 애플리케이션의 테스트 지원 코드이고, projects 저장소의 재사용 가능한 bridge
  API를 대신하지 않는다.

## 목표

1. 모든 `PropertyExportingServer` 구현체가 동일한 namespace와 키 변환 규칙으로
   Spring 동적 프로퍼티를 등록하게 한다.
2. supplier 평가를 Spring registry에 위임해 컨테이너 시작·프로퍼티 해석 순서를
   소비자가 선택하게 한다.
3. Spring 의존성을 선택 모듈에 격리해 기존 core 소비자의 classpath와 ABI를 바꾸지 않는다.
4. 빈 키, namespace 경계, properties/key 불일치, supplier 예외, 중복 등록의 동작을
   테스트와 문서로 고정한다.
5. Epic #1418의 stacked train에서 후속 #1337이 이 Slot의 merge head만 기준으로 시작할
   수 있도록 변경 범위를 새 모듈과 명시된 문서·CI 등록으로 제한한다.

## 비목표

- core `PropertyExportingServer`에 Spring API나 `compileOnly` Spring 의존성을 추가하는 것
- Spring Boot auto-configuration, bean post-processor, `@DynamicPropertySource` annotation
  processor를 제공하는 것
- 기존 `registerSystemProperties()`의 eager 동작·복원 semantics를 변경하는 것
- Workshop 내부 helper를 제거하거나 모든 기존 테스트를 새 API로 일괄 마이그레이션하는 것
- 컨테이너를 자동으로 시작하거나 `properties()` 값을 캐시하는 것
- 충돌하는 Spring registry entry를 탐지·삭제·덮어쓰는 별도 정책을 만드는 것

## 선택지와 결정

| 선택지 | 장점 | 문제 | 결정 |
| --- | --- | --- | --- |
| A. `testing/testcontainers-spring` 선택 모듈 | Spring 경계가 명확하고 core 소비자가 영향받지 않음 | 모듈·문서·CI 등록이 필요함 | **선택** |
| B. core에 `compileOnly` Spring API 추가 | 호출부가 한 artifact만 필요함 | 공개 SDK-neutral core에 Spring 타입이 새고, `compileOnly` 소비자 설정이 불명확함 | 제외 |
| C. Workshop helper만 유지 | 변경량이 작음 | projects의 재사용 API 승격과 #1321 목적을 충족하지 못함 | 제외 |

선택 모듈은 `api(project(":bluetape4k-testcontainers"))`와
`api("org.springframework:spring-test")`를 제공한다. Spring Test는 public 함수의
`DynamicPropertyRegistry` 타입이 소비자 compile classpath에 필요하므로 `compileOnly`가
아닌 공개 API 의존성이다. Spring 버전은 저장소의 Spring Boot 4 dependency platform으로
관리하고 모듈에서 임의 버전을 고정하지 않는다.

## API 및 데이터 흐름

### 등록 규칙

1. `propertyKeys()`를 한 번 호출한다.
2. 반환된 각 `key`에 대해
   `testcontainers.$propertyNamespace.$key`를 계산한다.
3. `registry.add(fullKey) { properties()[key] ?: error(...) }` 형태의 supplier를
   등록한다.
4. 함수는 `Unit`을 반환하며 시스템 프로퍼티를 읽거나 쓰지 않는다.

`propertyKeys()`가 빈 집합이면 아무 entry도 등록하지 않는다. 키 이름은 core가 정의한
기존 kebab-case 계약을 그대로 사용하며, bridge가 별도 변환하거나 필터링하지 않는다.
namespace나 key에 점(`.`)이 포함되는지에 대한 추가 정규화도 하지 않는다. 기존 core
계약의 입력을 신뢰해 두 API가 같은 full key를 만든다.

### Lazy supplier와 불일치

등록 단계에서 `properties()`를 호출하지 않는다. supplier 평가 시 map을 읽고,
`propertyKeys()`에 있었지만 map에 없는 키는 `IllegalStateException`으로 실패한다.
이 실패는 조용히 누락된 Spring property를 만드는 대신 서버 구현의 계약 위반을 즉시
드러낸다. map에 추가 키가 있어도 `propertyKeys()`에 없는 키는 등록하지 않는다.

supplier가 서버의 `properties()` 예외를 던지면 예외를 감싸거나 재시도하지 않고 그대로
전달한다. 컨테이너가 시작되지 않은 상태에서 값이 필요한 경우의 lifecycle 오류도
동일하게 소비자에게 보인다.

### 중복과 우선순위

bridge는 registry의 기존 entry를 사전 검사하거나 삭제하지 않는다. 같은 full key가
여러 서버 또는 helper에서 등록되면 Spring `DynamicPropertyRegistry`의 등록 순서와
우선순위 semantics를 따른다. 이 함수는 충돌을 해결하는 authority가 아니며, 소비자는
동일 key를 한 경로에서만 등록해야 한다.

Spring 문서도 registry supplier가 프로퍼티가 실제로 해석될 때 호출된다고 설명한다.
구현은 이 계약을 보존한다.

- [DynamicPropertyRegistry Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/DynamicPropertyRegistry.html)
- [DynamicPropertySource reference](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-dynamicpropertysource.html)

## 수명주기와 호환성

- bridge 등록은 컨테이너를 start/stop하지 않는다.
- bridge 등록은 `System.setProperty`, `System.clearProperty`를 호출하지 않는다.
- 기존 `registerSystemProperties()`와 같은 server instance에 함께 사용해도 등록
  메커니즘이 서로의 cleanup을 변경하지 않는다.
- core 모듈의 public signature와 기존 module coordinate는 변경하지 않는다.
- 새 모듈은 Spring을 사용하는 소비자만 선택적으로 추가한다.
- Java 25/1.13.0 baseline을 선행 Slot에서 상속하고, 새 모듈도 저장소 기본 Kotlin/JVM
  target을 사용한다. Java 21 compatibility island에 암묵적으로 추가하지 않는다.

## 파일·모듈 설계

### 새 모듈

- `testing/testcontainers-spring/build.gradle.kts`
- `testing/testcontainers-spring/README.md`
- `testing/testcontainers-spring/README.ko.md`
- `testing/testcontainers-spring/src/main/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistry.kt`
- `testing/testcontainers-spring/src/test/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistryTest.kt`
- `testing/testcontainers-spring/src/test/resources/junit-platform.properties`
- `testing/testcontainers-spring/src/test/resources/logback-test.xml`

`settings.gradle.kts`의 `includeModules("testing", withBaseDir = false)` 자동 등록을
그대로 사용한다. 별도 include를 복제하지 않으며 `bluetape4k-testcontainers-spring`
project path가 생성되는지 settings/catalog 검증으로 확인한다.

### 등록 표면

- 루트 `README.md`, `README.ko.md`의 testing module 목록
- `.github/workflows/ci.yml`의 `testing/testcontainers-spring/**` path filter와
  module test job
- `.github/workflows/nightly-tests.yml`의 pure JVM bridge test task(컨테이너 시작 없음)
- `.github/workflows/codeql.yml`의 testing scope 또는 명시적인 N/A 근거
- 필요할 때만 static module/coverage contract 검사에 새 경로를 추가한다. 기존
  `testing/testcontainers`의 Docker build·Kover exclusion을 새 bridge에 복사하지 않는다.

## 테스트 계약

최소 테스트는 Testcontainers Docker를 시작하지 않는 fake server와 fake registry로
작성한다.

1. **full key mapping**: `redis` namespace의 `host`, `port`, `url`이 각각
   `testcontainers.redis.host`, `...port`, `...url`로 등록된다.
2. **lazy evaluation**: 등록 직후 `properties()` 호출 횟수는 0이고, supplier를
   평가한 뒤 정확히 1회 증가한다. 같은 supplier를 다시 평가하면 registry가 요청한
   시점마다 값을 읽는 계약을 보존한다.
3. **empty keys**: 빈 `propertyKeys()`는 registry entry를 만들지 않는다.
4. **key/map mismatch**: 선언된 키가 map에 없으면 supplier 평가 시
   `IllegalStateException`이 발생하고, 누락이 조용히 무시되지 않는다.
5. **supplier failure**: `properties()`가 던진 예외가 원래 타입·메시지로 전달된다.
6. **no system property mutation**: 등록 전후 관련 `System.getProperty`가 동일하다.
7. **collision delegation**: 같은 registry에 두 번 등록해도 bridge가 기존 값을
   삭제·덮어쓰기 위한 별도 로직을 실행하지 않으며 두 `add` 호출이 전달된다.
8. **public API compile**: README의 `@DynamicPropertySource` 예제가 새 모듈 coordinate와
   package를 사용한다.

기존 `testing/testcontainers` contract test와 testcontainers integration suite는 이
Slot의 직접 변경 대상이 아니므로 별도로 순차 실행해 regression 증거를 남긴다.

## 문서 계약

EN/KO README는 같은 marker와 의미를 갖는다.

- dependency coordinate: `io.bluetape4k:bluetape4k-testcontainers-spring`
- 예제 API: `registerDynamicProperties(registry)`
- full key 예시: `testcontainers.redis.host`
- 값은 lazy supplier로 평가되며 bridge가 container lifecycle을 소유하지 않음
- 시스템 프로퍼티 등록과 DynamicPropertyRegistry 등록은 별도 선택지
- 중복 key는 consumer가 피하고 registry semantics를 따름

문서는 Spring Test 의존성이 없는 core 사용자가 새 모듈을 자동으로 받는다고 오해하지
않도록 선택 모듈임을 명시한다.

## 실패 모드와 대응

| 실패 모드 | 탐지 | 대응 |
| --- | --- | --- |
| core가 Spring API를 transitively 끌어옴 | core dependency graph·module test | Spring은 새 모듈에만 두고 core diff를 차단 |
| 등록 시점에 `properties()`가 eager 평가됨 | 호출 횟수 검증 | `propertyKeys()`만 eager 호출하고 supplier 내부로 값 계산 이동 |
| key와 map이 불일치해 빈 값이 등록됨 | mismatch supplier test | 명시적 `IllegalStateException`으로 실패 |
| 시스템 프로퍼티가 오염됨 | before/after system property test | bridge에서 JVM system property API를 호출하지 않음 |
| README/CI에 새 모듈이 누락됨 | module path·marker·workflow read-back | settings, README, CI/nightly/codeql을 같은 Slot에서 갱신 |
| 기존 helper와 key가 충돌함 | collision delegation test·문서 | bridge가 해결하지 않고 consumer가 단일 등록 경로를 선택 |
| 후속 stacked slot이 잘못된 base를 사용함 | PR base/head exact read-back | Slot 3 merge SHA를 기록한 뒤에만 #1337 branch를 생성 |

## 검증 순서

```bash
./gradlew :bluetape4k-testcontainers-spring:test \
  --no-daemon --no-configuration-cache --no-build-cache
./gradlew :bluetape4k-testcontainers-spring:compileKotlin \
  :bluetape4k-testcontainers-spring:detekt \
  --no-daemon --no-configuration-cache --no-build-cache
./gradlew :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-testcontainers-spring:check \
  --no-daemon --no-configuration-cache --no-build-cache
./gradlew projects --no-daemon --no-configuration-cache --no-build-cache
git diff --check
node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  README.md README.ko.md \
  testing/testcontainers-spring/README.md \
  testing/testcontainers-spring/README.ko.md
```

Testcontainers Docker integration suite는 기존 module의 Docker/Jib 의존성을 유지하므로
bridge unit test와 같은 Gradle invocation에서 병렬 실행하지 않는다. Docker가 필요한
기존 suite는 별도 순차 검증으로 분리한다.

## Slot 3 DoD

- [ ] #1321 live issue·Epic #1418 metadata와 선행 Slot merge head를 read-back했다.
- [ ] 승인된 spec/plan이 branch에 기록되고 Lore commit을 사용했다.
- [ ] `bluetape4k-testcontainers-spring`가 자동 등록되고 Spring Test dependency가
      core에서 격리된다.
- [ ] public extension이 full key, lazy supplier, mismatch/failure semantics를 구현한다.
- [ ] fake registry/server contract test와 module compile/detekt/check가 통과한다.
- [ ] EN/KO module README와 root testing 목록이 parity를 이룬다.
- [ ] CI/nightly/codeql path와 test contract가 새 module을 누락하지 않는다.
- [ ] PR body 마지막 섹션이 `## DoD Status`이며
      `Required checks: X/Y; N/A: N; Blocked: N`을 포함한다.
- [ ] 독립 6-perspective review에서 P0/P1이 0이고 actionable P2가 해소됐다.
- [ ] Epic #1418 진행률을 3/4로 보고하고, 후속 #1337은 Slot 3 merge 후에만 시작한다.

## 추적성

| #1321 요구 | 설계/검증 위치 |
| --- | --- |
| Spring DynamicPropertyRegistry bridge | API 및 데이터 흐름, 새 module source |
| core Spring dependency 금지 | 선택지 A, 수명주기와 호환성, dependency graph check |
| namespace/properties 재사용 | 등록 규칙, full key mapping test |
| supplier lifecycle | lazy evaluation, supplier failure test |
| collision semantics | 중복과 우선순위, collision delegation test |
| 재사용 가능한 published surface | module README, root catalog, CI path, public API compile |

## 작성·검토 게이트

- SPW-01 source ledger: #1321/#1418 live metadata, core source, Workshop design/plan,
  Spring official Javadoc를 확인했다.
- SPW-02 spec structure: 배경·목표·비목표·대안·결정·API·수명주기·테스트·실패모드를
  포함한다.
- SPW-03 Korean naturalness: reader-facing prose는 한국어로 유지하고 API·명령·URL은
  원문 token을 보존한다.
- SPW-04 traceability: issue 요구와 각 DoD/test가 표로 연결돼 있다.
- SPW-05 readback: 파일 전체를 다시 읽고 미완성 placeholder와 체크되지 않은
  설계 항목이 남지 않았는지 확인한다. 구현 후 checklist는 plan/PR DoD에서 갱신한다.
