# Issue #1334: Kafka4 CI 경로와 Kover 커버리지 계약

## 배경

일일 CI의 `kafka-infra` 경로 필터가 `infra/kafka/**`만 선택해 `infra/kafka4/**` 변경을 놓치고 있었다. Kafka4 모듈은 Gradle 설정에서 자동 등록되지만, 해당 모듈의 테스트와 Kover 보고서가 Kafka CI 작업과 산출물 계약에 포함되어 있는지 검증하는 장치도 없었다.

## 결정

- `kafka-infra` 필터에 `infra/kafka4/**`를 추가한다.
- Kafka CI 작업에서 `:bluetape4k-kafka4:test`와 `:bluetape4k-kafka4:koverXmlReport`를 Kafka3 작업과 함께 실행한다.
- `scripts/validate-ci-kafka4-coverage.rb`를 `changes` 작업에서 실행해 다음 계약을 fail-closed로 검사한다.
  - `settings.gradle.kts`의 `infra` 자동 등록과 `infra/kafka4/build.gradle.kts` 존재
  - 경로 필터와 `test-kafka-infra` 스케줄링 조건
  - Kafka3/Kafka4 테스트·Kover task 및 테스트 실패 전파
  - 테스트 결과 XML과 Kover 보고서 artifact 경로
- validator 회귀 스크립트에서 경로, task, 조건, artifact, 자동 등록 누락을 각각 실패 fixture로 고정한다.

## 검증 결과

- `ruby scripts/validate-ci-kafka4-coverage_test.rb` — 통과
- `ruby scripts/validate-ci-kafka4-coverage.rb` — `CI Kafka4 coverage is valid`
- `ruby scripts/validate-ci-csv-coverage.rb` — `CI CSV coverage is valid`
- `actionlint .github/workflows/ci.yml` — 통과
- `./gradlew :bluetape4k-kafka4:test --no-daemon --no-configuration-cache --console=plain` — 297개 테스트 통과
- `./gradlew :bluetape4k-kafka4:koverXmlReport --no-daemon --no-configuration-cache --console=plain` — `build/reports/kover/report.xml` 생성
- `./gradlew projects --no-daemon --no-configuration-cache --console=plain` — `:bluetape4k-kafka4`가 `/infra/kafka4`로 자동 등록됨

첫 전체 실행에서는 Embedded Kafka의 `createTopics` node-assignment timeout으로 1개 테스트가 실패했다. 해당 테스트를 단독 실행해 통과한 뒤 전체 297개 테스트를 재실행해 통과했으므로, 이번 변경의 회귀가 아닌 일시적 테스트 환경 변동으로 기록한다. CI에서는 기존 retry step이 이 경계를 처리한다.

## 재발 방지

CI 경로 필터, Gradle 자동 등록, 실제 test/Kover task, artifact 업로드 경로를 하나의 validator와 회귀 fixture로 함께 검사한다. 새 infra 모듈을 추가하거나 CI 작업을 분리할 때는 자동 등록 목록과 변경 경로·task manifest를 함께 갱신하고 validator를 먼저 실행한다.
