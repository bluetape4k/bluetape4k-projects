# Issue #1344: Gradle 10 제거 예정 경고를 fail-closed로 관리하기

## 맥락

Gradle 9.7의 `help --warning-mode all`은 Kotlin DSL delegate와 BOM 의존성
constraint에서 Gradle 10 제거 예정 경고를 출력했다. 경고는 빌드를 즉시 실패시키지
않으므로 전체 빌드가 통과해도 다음 Gradle 메이저 업그레이드에서 구성 단계가 깨질 수
있었다.

## 결정

- `by project`, `by configurations`, `by sourceSets.creating`, `by
  configurations.getting`, `by tasks.registering`을 명시적인
  `providers.gradleProperty`, `getByName`, `create`, `register` 호출로 바꾼다.
- Java Platform BOM constraint에는 Project 객체를 넘기지 않고
  `rootProject.dependencies.project(path)`로 project dependency notation을 만든다.
- CI의 catalog-governance 단계에서 `help --warning-mode all`을 실행하고,
  `scheduled to be removed in Gradle 10` 또는 `will fail with an error in Gradle 10`
  문구가 있으면 실패시킨다. 다른 라이브러리의 독립적인 deprecation은 이 gate의
  범위에 포함하지 않는다.
- 등록된 task의 이름과 Provider 기반 연결(`artifact`, `dependsOn`)은 기존 이름과
  동작을 유지한다.

## 결과

- root build script, BOM, Hibernate consumer test source set, Redis consumer test
  configuration에서 Gradle 10 제거 예정 delegate/Project notation을 제거했다.
- 경고를 숨기거나 `warning-mode`를 전역으로 억제하지 않고, CI가 새 제거 예정
  경고를 fail-closed로 차단한다.

## 검증

- 변경 전 `./gradlew help --no-configuration-cache --warning-mode all --no-daemon
  --console=plain`에서 Gradle 10 제거 예정 경고 23개를 재현했다.
- 변경 후 같은 명령은 성공했고 Gradle 10 제거 예정 경고는 0개였다. NMCP의
  별도 Kotlin deprecation warning은 이 이슈의 Gradle 10 제거 경계가 아니므로
  별도 backlog로 남긴다.
- `./gradlew :bluetape4k-spring-boot-redis:consumerRuntimeTest
  :bluetape4k-hibernate:consumerRuntimeTest --no-configuration-cache --no-daemon`
  이 성공해 두 consumer source set/configuration 경계를 확인했다.
- `./gradlew :bluetape4k-spring-boot-redis:compileTestKotlin
  :bluetape4k-hibernate:compileTestKotlin --no-configuration-cache --no-daemon`이
  성공했다.
- `.github/workflows/ci.yml`은 `actionlint`와 `scripts/validate-ci-csv-coverage.rb`를
  통과했다.

## 향후 지침

Gradle 버전을 올리거나 build script delegate를 추가할 때는 먼저
`help --warning-mode all --no-configuration-cache`를 실행하고, Gradle 메이저 제거
예정 문구를 별도 분류한다. CI gate의 정규식은 Gradle 메이저 제거 계약만 포함하며,
독립적인 plugin API deprecation은 해당 plugin migration issue에서 처리한다.
