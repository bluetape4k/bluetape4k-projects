# #1321 Spring DynamicPropertyRegistry bridge에서 얻은 교훈

## 배경

`PropertyExportingServer`는 Testcontainers core에서 연결 정보를
`testcontainers.{namespace}.{key}` 형식으로 노출한다. Spring 테스트가 같은 값을
사용할 때마다 `registry.add`를 직접 반복하던 Workshop helper를 재사용 가능한
projects API로 승격했지만, core의 SDK-neutral 경계는 유지해야 했다.

## 결정

- Spring Test 타입은 `bluetape4k-testcontainers-spring` 선택 모듈에만 둔다.
- `PropertyExportingServer` core에는 Spring 의존성과 auto-configuration을 추가하지
  않는다.
- 등록 단계에서는 `propertyKeys()`만 읽고, 값은 DynamicPropertyRegistry supplier가
  해석될 때 `properties()`에서 읽는다.
- bridge는 컨테이너 수명주기와 JVM system property를 소유하지 않는다.
- 중복 key 해결은 bridge가 아니라 소비자와 Spring registry 등록 순서의 책임이다.

## 재발 방지 규칙

1. 새 adapter가 core contract를 재사용할 때 public module boundary를 먼저 설계하고,
   `compileOnly`로 선택 dependency를 core에 섞지 않는다.
2. 이름 목록(`propertyKeys`)과 값 계산(`properties`)을 분리한 기존 계약을 eager
   기준 데이터로 합치지 않는다. 등록 시점과 해석 시점의 호출 횟수를 테스트한다.
3. supplier에서 누락된 key를 빈 문자열이나 null로 대체하지 않는다. 구현 오류를
   `IllegalStateException`으로 드러내야 한다.
4. 시스템 프로퍼티 기반 API와 Spring dynamic API를 같은 cleanup 경로로 합치지 않는다.
5. 새 testing module은 settings auto-registration, root EN/KO catalog, CI path,
   nightly task, CodeQL scope를 한 변경에서 함께 확인한다.

## 검증 증거

- `./gradlew projects --no-daemon --no-configuration-cache --no-build-cache`에서
  `:bluetape4k-testcontainers-spring` project가 자동 등록됐다.
- 구현 전 module test는 extension 미해결 참조로 RED가 됐다.
- 구현 후 bridge contract 8개가 통과했고 `:bluetape4k-testcontainers-spring:check`
  와 detekt가 성공했다.
- core runtime dependency report에는 `spring-test`가 없고, adapter report에는
  Spring Test 7.0.8이 나타났다.
- `actionlint`가 CI/nightly/CodeQL workflow를 통과했고, CodeQL policy·Testcontainers
  contract·CI CSV coverage·nightly Kover isolation 검사가 모두 성공했다.

## 남은 경계

실제 Testcontainers 서버와 Spring application context를 함께 띄우는 통합 시나리오는
이 Slot의 범위가 아니다. bridge는 fake registry/server로 순수 JVM 계약을 고정하며,
실제 컨테이너 lifecycle 및 소비자 migration은 각 애플리케이션의 테스트 범위로
남긴다.
