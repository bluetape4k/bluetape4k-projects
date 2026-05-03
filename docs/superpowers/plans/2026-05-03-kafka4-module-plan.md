# Plan: infra/kafka4 모듈 신규 생성

- **Spec**: `docs/superpowers/specs/2026-05-03-kafka4-module-design.md`
- **Issue**: #281
- **Branch**: `feat/kafka4-module`
- **Date**: 2026-05-03
- **Status**: Ready for execution

## 0. 결정사항

| 항목 | 결정 |
|---|---|
| 모듈 전략 | `infra/kafka` 유지, `infra/kafka4` 병렬 신규 생성 |
| Spring BOM | `implementation(platform(libs.spring.boot4.dependencies))` |
| Spring Kafka alias | `spring-kafka4 = 4.0.5`, `spring-kafka4-test = 4.0.5` 별도 추가 |
| Reactor Kafka alias | `reactor-kafka4` 별도 추가로 Kafka 4 classpath 경계 유지 |
| Kafka artifact 정렬 | `infra/kafka4` 내부에서 `org.apache.kafka` group을 `kafka4` version ref로 강제 |
| Jackson | Jackson 3 (`bluetape4k-jackson3`, `tools.jackson.*`, Spring Kafka 4 Jackson classes) |
| Embedded Kafka | KRaft 전용, `kraft = true` 제거 |
| Streams branch DSL | `KafkaStreamBrancher` 대신 Kafka Streams native `split/branch/defaultBranch` |

## 1. 작업 순서

### Phase A: 모듈 스캐폴딩

1. `infra/kafka`를 `infra/kafka4`로 복사한다.
2. `infra/kafka4/build.gradle.kts`를 Kafka 4 / Boot 4 / Jackson 3 기준으로 수정한다.
3. `gradle/libs.versions.toml`에 `spring-kafka4`와 `reactor-kafka4` aliases를 추가한다.
4. `./gradlew projects` 또는 targeted Gradle task로 `:bluetape4k-kafka4` 등록을 확인한다.

### Phase B: 컴파일 포팅

5. main source compile을 실행한다.
6. Spring Kafka 4 / Kafka 4 제거 API를 확인해 좁게 수정한다.
7. Jackson 2 imports를 Jackson 3 또는 Spring Kafka 4 Jackson classes로 치환한다.
8. 한국어 KDoc은 기존 public API의 KDoc을 유지하고, 신규/변경 public API가 생기면 한국어 KDoc을 추가한다.

### Phase C: 테스트 포팅

9. `@EmbeddedKafka(kraft = true)`를 제거한다.
10. Spring Boot embedded broker mapping은 `bootstrapServersProperty` 또는 `${spring.embedded.kafka.brokers}` 방식으로 정리한다.
11. `KafkaStreamBrancher` 테스트를 Kafka Streams native branching API로 변경한다.
12. `compileTestKotlin`을 실행해 test API 차이를 정리한다.
13. `:bluetape4k-kafka4:test`를 실행한다.

### Phase D: 문서

14. `infra/kafka4/README.md` 작성
15. `infra/kafka4/README.ko.md` 작성
16. 두 README에 compatibility 표, dependency snippet, Mermaid diagram, test command를 포함한다.

### Phase E: 검증 / 리뷰 / PR

17. `.github/workflows/nightly-tests.yml`의 infra matrix에 `:bluetape4k-kafka4:test`와 `:bluetape4k-kafka4:koverXmlReport`를 추가한다.
18. `./bin/repo-test-summary -- ./gradlew :bluetape4k-kafka4:compileKotlin`
19. `./bin/repo-test-summary -- ./gradlew :bluetape4k-kafka4:compileTestKotlin`
20. `./bin/repo-test-summary -- ./gradlew :bluetape4k-kafka4:test`
21. `git diff --check`
22. Tier 4 code review: 컴파일 리스크, dependency 경계, README/KDoc 체크
23. Lore commit 생성, push, PR 생성

## 2. 회귀 기준

- 기존 `infra/kafka` 파일을 수정하지 않는다. 단, catalog alias 추가는 공용 catalog 변경으로 허용한다.
- `spring-kafka` 3.x alias는 그대로 유지한다.
- `spring-kafka4` alias만 `infra/kafka4`에서 사용한다.
- Kafka 4 모듈의 test/runtime classpath에 Kafka 3 artifact가 섞이지 않도록 `org.apache.kafka` group을 정렬한다.
- `README.md`와 `README.ko.md` 둘 중 하나만 갱신된 상태로 커밋하지 않는다.
