# Spec: infra/kafka4 모듈 신규 생성

- **Issue**: #281
- **Branch**: `feat/kafka4-module` (worktree: `.worktrees/feat-kafka4-module`)
- **Date**: 2026-05-03
- **Status**: Reviewed

## 1. 배경 / 목적

`infra/kafka`는 Kafka 3.9.x와 Spring Kafka 3.x를 기준으로 유지한다. Kafka 4.x 클라이언트와 Spring Kafka 4.x는 ZooKeeper 제거, KRaft 전용 embedded broker, Jackson 3 지원, Spring Framework 7 / Spring Boot 4 의존성 때문에 기존 모듈에 섞으면 호환성 경계가 흐려진다.

이 작업은 신규 `infra/kafka4` 모듈을 추가해 다음을 제공한다.

1. `:bluetape4k-kafka4` 아티팩트 자동 등록
2. Kafka 4.2.0 client/streams API 기반 core, codec, coroutine, streams DSL 포팅
3. Spring Kafka 4.0.5 + Spring Boot 4.0.6 BOM 기준 test/support 포팅
4. `README.md` / `README.ko.md`로 Kafka 3 모듈과 Kafka 4 모듈의 경계를 문서화

## 2. 조사 요약

### 공식 문서 근거

- Spring Kafka 4.0.5 문서는 Kafka 4 전환으로 ZooKeeper 기반 기능이 제거되고 embedded test framework가 KRaft 전용이라고 설명한다.
- `@EmbeddedKafka`의 `kraft` 속성은 제거되었고 KRaft가 유일한 모드다.
- Spring Kafka 4는 Jackson 3 클래스를 제공하며 Jackson 2 클래스는 deprecated 호환 경로다.
- Spring Boot 4.0.6 Kafka 문서는 `@EnableKafkaStreams`와 `KafkaStreamsConfiguration` 자동 설정 조건을 유지하고, embedded broker 주소는 `bootstrapServersProperty = "spring.kafka.bootstrap-servers"` 또는 `${spring.embedded.kafka.brokers}` 매핑으로 주입한다고 안내한다.
- Spring 공식 release note는 Spring Kafka 4.0.5가 Spring Boot 4.0.6에 통합된다고 명시한다.

### Repo 근거

- `settings.gradle.kts`는 `includeModules("infra", withBaseDir = false)`로 `infra/kafka4`를 `:bluetape4k-kafka4`로 자동 등록한다.
- `gradle/libs.versions.toml`에는 `kafka4-*` alias가 이미 준비되어 있지만 Spring Kafka 4 별도 alias는 아직 없다. `reactor-kafka`는 root BOM 영향으로 Kafka 3 라인과 섞일 수 있어 Kafka 4 모듈 전용 alias가 필요하다.
- Spring Boot 4 모듈들은 `implementation(platform(libs.spring.boot4.dependencies))`를 직접 선언해 root Spring Boot 3 dependency management의 다운그레이드를 피한다.
- `infra/kafka`는 Kafka core, codecs, coroutines, Spring Kafka extensions, test utils, Kafka Streams DSL을 한 모듈에 제공한다.

## 3. 범위

### 포함

- `infra/kafka4/build.gradle.kts` 생성
- `gradle/libs.versions.toml`에 `spring-kafka4` version ref와 `spring-kafka4`, `spring-kafka4-test` aliases 추가
- `infra/kafka` 소스와 테스트를 `infra/kafka4`로 포팅
- Spring Kafka 4 / Boot 4 차이 반영
    - `@EmbeddedKafka(kraft = true)` 제거
    - Kafka Streams branch 테스트는 `KafkaStreamBrancher` 제거 후 Kafka Streams `split().branch().defaultBranch()` API 사용
    - Jackson 3 serializer/deserializer/serde 사용
    - Spring Boot 4 BOM 적용
- 영어/한국어 README 작성
- targeted compile/test 검증
- GitHub Nightly workflow의 infra matrix에 `:bluetape4k-kafka4:test`와 kover report task 추가

### 제외

- 기존 `infra/kafka` 변경 또는 제거
- Boot 4 starter/auto-configuration 신설
- Kafka Queue/share consumer 신규 API 래퍼 추가
- 공개 API 이름을 Kafka 4 전용으로 재설계

## 4. 설계 선택지

| 선택지 | 내용                                                              | 장점                              | 단점                               | 결정      |
|--------|-------------------------------------------------------------------|-----------------------------------|------------------------------------|-----------|
| A      | `infra/kafka`를 Kafka 4로 직접 업그레이드                         | 중복 없음                         | Kafka 3/Spring Kafka 3 사용자 깨짐 | 거절      |
| B      | `infra/kafka4` 신규 모듈에 기존 API를 포팅                        | 호환성 경계 명확, issue 목적 부합 | 일부 코드 중복                     | 채택      |
| C      | core와 spring 통합을 `infra/kafka4` / `spring-boot4/kafka`로 분리 | 의존성 그래프 가장 엄격           | 이번 issue 범위보다 큼             | 후속 후보 |

## 5. 주요 리스크 / 완화

### R1. Spring Boot 3 BOM이 Spring Kafka 4 의존성을 다운그레이드

완화: `infra/kafka4`는 Spring Boot 4 모듈 패턴처럼 `implementation(platform(libs.spring.boot4.dependencies))`를 선언하고, Spring Kafka 의존성은 `spring-kafka4` alias로 분리한다. 테스트 런타임에서는 `org.apache.kafka` artifact 전체를 `kafka4` version ref로 정렬해 Spring Boot 3 root dependency management의 Kafka 3.x 다운그레이드를 차단한다.

### R2. Spring Kafka 4에서 제거된 API로 컴파일 실패

완화: 먼저 `infra/kafka`를 기계적으로 복사한 뒤 targeted compile로 제거 API를 검출한다. 확인된 변화는 Spring Kafka 4 문서 기반으로 좁게 수정한다.

### R3. Embedded Kafka KRaft 변경으로 테스트가 불안정

완화: `@EmbeddedKafka(kraft = true)`를 제거하고 Boot 문서의 `bootstrapServersProperty` 또는 `${spring.embedded.kafka.brokers}` 방식만 사용한다. 실패 시 Spring Kafka 테스트만 축소 재현한 뒤 수정한다.

### R4. Jackson 2/3 혼용

완화: Kafka 4 모듈은 Spring Kafka 4의 Jackson 3 경로에 맞춰 `bluetape4k-jackson3`, `tools.jackson.*`, `JacksonJsonSerializer/Deserializer/Serde`를 사용한다.

## 6. Acceptance Criteria

- `:bluetape4k-kafka4` Gradle project가 자동 등록된다.
- `./gradlew :bluetape4k-kafka4:compileKotlin` 통과
- `./gradlew :bluetape4k-kafka4:compileTestKotlin` 통과
- 가능한 범위에서 `./gradlew :bluetape4k-kafka4:test` 통과
- `README.md`와 `README.ko.md`가 Kafka 4 / Spring Boot 4 전용 모듈임을 설명한다.
- `.github/workflows/nightly-tests.yml`이 `:bluetape4k-kafka4:test`를 실행한다.
- `infra/kafka` 기존 동작은 변경하지 않는다.

## 7. Spec Review

| 관점           | 검토 결과                                                                                                                                                       | 반영                                                          |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| Developer      | 전체 포팅은 크지만 기존 API 경계를 유지하면 사용자가 Kafka 3/4를 선택 가능하다.                                                                                 | `infra/kafka` 불변, 신규 모듈 병렬 추가                       |
| Security       | Kafka LZ4 transitive CVE 대응은 기존 `infra/kafka`의 exclude + `at.yawk.lz4` 유지가 필요하다.                                                                   | build.gradle에 동일 exclude 유지                              |
| Ops/SRE        | Embedded Kafka KRaft 전용 변화와 test runtime 안정성이 가장 큰 위험이다.                                                                                        | `kraft` 제거와 targeted test를 acceptance에 포함              |
| User/Caller    | Kafka 3 모듈과 Kafka 4 모듈의 선택 기준이 문서에 보여야 한다.                                                                                                   | README 양쪽에 compatibility 표 포함                           |
| Critic         | Spring Kafka 4 alias 분리가 없으면 기존 Kafka 3 모듈까지 같이 흔들린다.                                                                                         | `spring-kafka4` alias 신규 추가                               |
| Implementation | Spring Kafka 4 test runtime에서 일부 Kafka artifact가 root BOM에 의해 Kafka 3.x로 내려가면 embedded broker가 `MetadataVersion`/`Feature` class 오류로 실패한다. | `reactor-kafka4` alias와 Kafka group resolution strategy 추가 |
