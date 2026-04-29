# Spring Boot 3 / 4 동기화 유지 정책

Issue #112 기준으로 `spring-boot3/`와 `spring-boot4/`는 같은 하위 모듈 이름을 유지한다. 신규 모듈을 한쪽에만 추가하면 Spring Boot 3/4 사용자 경험이 갈라지므로 PR 단계에서 구조, BOM, CI 등록을 함께 확인한다.

## 모듈 추가 체크리스트

- `spring-boot3/{module}`와 `spring-boot4/{module}`를 동시에 추가한다.
- 두 모듈 모두 `build.gradle.kts`, `README.md`, `README.ko.md`, 테스트 리소스를 같은 수준으로 둔다.
- Gradle 프로젝트명은 `:bluetape4k-spring-boot3-{module}`와 `:bluetape4k-spring-boot4-{module}` 형식을 따른다.
- Spring Boot 4 모듈은 `implementation(platform(Libs.spring_boot4_dependencies))`를 사용한다. 전역 `dependencyManagement`로 Spring Boot 4 BOM을 적용하지 않는다.
- Spring Boot 4 소스에서 Spring Framework 7.x / Spring Data 4.x / Jakarta EE 11 패키지 또는 API 차이가 있으면 README나 KDoc에 의도를 남긴다.
- 공통 기능을 한쪽에 먼저 구현한 경우 같은 PR 안에서 다른 쪽도 구현하거나, 의도적인 비대칭이면 PR 설명에 사유와 후속 이슈를 남긴다.

## Spring Boot 4 BOM 업데이트 추적

- 버전 위치: `buildSrc/src/main/kotlin/Libs.kt`
  - `Plugins.Versions.spring_boot4`
  - `Versions.spring_boot4`
  - `Libs.spring_boot4_dependencies`
- 업데이트 기준:
  - Spring Boot 4 BOM이 관리하는 Spring Framework 7.x, Spring Data 4.x, Hibernate/Jakarta 계열 버전을 함께 확인한다.
  - Spring Boot 4 모듈의 `resolutionStrategy` 강제 버전은 BOM으로 흡수 가능한지 업데이트 때마다 재검토한다.
  - Spring Boot 3 전역 BOM이 Spring Boot 4 classpath를 낮추는지 `dependencyInsight` 또는 대상 모듈 테스트로 확인한다.
- 권장 검증:
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-spring-boot4-core:test`
  - 변경 모듈별 `./bin/repo-test-summary -- ./gradlew :bluetape4k-spring-boot4-{module}:test`
  - BOM 업데이트 PR에서는 nightly `Nightly Tests (Testcontainers)`를 `workflow_dispatch`로 실행한다.

## CI 확인 결과

- 일반 CI(`.github/workflows/ci.yml`)에는 `Spring Boot 3/4 Parity` job을 둔다.
  - `.github/scripts/check-spring-boot-parity.py`가 `spring-boot3/`와 `spring-boot4/`의 하위 모듈 이름이 같은지 확인한다.
  - 각 모듈의 `build.gradle.kts`가 대응 Spring Boot BOM을 적용하는지 확인한다.
  - 모든 Spring Boot 3/4 모듈이 nightly `test` task에 등록되어 있는지 확인한다.
  - 비-demo 모듈은 nightly `koverXmlReport` task에도 등록되어 있는지 확인한다.
- nightly CI(`.github/workflows/nightly-tests.yml`)는 Spring Boot 4를 독립 job으로 실행한다.
  - H2 기반 모듈은 `Test / Spring Boot (H2)`에서 Spring Boot 3/4를 나란히 실행한다.
  - Docker/Testcontainers가 필요한 모듈은 `Test / Spring Boot 4 (...)` matrix job에서 Spring Boot 3 job과 분리 실행한다.
  - Spring Boot 4 job은 `mock-web-server`와 `mock-webflux-server` 이미지를 별도로 빌드한 뒤 테스트한다.

2026-04-29 확인 기준으로 `spring-boot3/`와 `spring-boot4/`는 각각 13개 모듈이 같은 이름으로 존재하며, 모든 모듈의 nightly `test` 등록과 비-demo 모듈의 `koverXmlReport` 등록을 CI 스크립트가 검증한다.
