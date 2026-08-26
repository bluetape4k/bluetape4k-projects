# Epic #1420 데이터 계약 train은 경계별 증거를 누적해야 한다

관련 이슈: #1359 · #1346 · #1357 · #1358

train: `fix/1359-cassandra-write-options-nullable` →
`build/1346-querydsl-kotlin-codegen` →
`fix/1357-hibernate-lettuce-root-condition` →
`fix/1358-mongodb-boot41-boundary`

## 원인과 결정

- Cassandra `WriteOptions`는 nullable duration/timestamp를 다루면서도
  invalid TTL을 statement에 흘려보낼 수 있었다. `null`은 clause를 생략하고,
  zero/subsecond duration은 whole-second `TTL 0`으로 보존하며, negative 값은
  builder에서 거부하고 Int seconds 범위 초과는 `ArithmeticException`으로
  끝나도록 고정했다. Cassandra timestamp는 microseconds 계약을 문서와
  테스트에 함께 남겼다.
- QueryDSL Kotlin codegen 후보는 clean Kotlin 2.4/JDK 25 compile에서
  `com.querydsl.kotlin.codegen.ExtensionsKt.asTypeName(Extensions.kt:48)`의
  `NullPointerException`으로 생성 소스 0개를 남겼다. upstream
  [QueryDSL #3454](https://github.com/querydsl/querydsl/issues/3454) 확인 결과가
  해결될 때까지 Java APT 경로를 지원 matrix의 기본으로 유지하고 후보 의존성은
  비활성화했다.
- Hibernate-Lettuce는 root enabled와 `metrics.enabled`를 독립적으로 보던
  경계가 있어 root disabled 상태에서 metrics/Actuator가 살아날 수 있었다.
  root/metrics 2×2 `ApplicationContextRunner` 검증으로 customizer, binder,
  endpoint의 노출 조건을 모두 고정했다.
- Spring Boot 4.1 MongoDB는 `MongoProperties` binding namespace가
  `spring.mongodb.*`이고 `DataMongoReactiveAutoConfiguration`이 Mongo
  client auto-configuration 뒤에 온다. custom auto-configuration을 그 뒤로
  정렬하고, legacy-only `spring.data.mongodb.uri`를 exact migration
  exception으로 fail-fast해 localhost fallback을 차단했다. 통합 테스트의
  URL/lifecycle 소유자는 `AbstractReactiveMongoTest` 하나로 남겼다.

## 검증 증거

- #1359: `OptionsSupportTest` 18개, Cassandra detekt 통과.
- #1346: Java APT compatibility test 2개와 기존 QueryDSL 예제 5개 통과.
  Java APT clean baseline은 81 generated sources/324 KB, 18.54초였고 Kotlin
  후보는 12.29초에 NPE와 generated sources 0개로 실패했다. 이 모듈에는 JMH
  대상이 없어 별도 benchmark는 N/A다.
- #1357: root/metrics 2×2와 기본값/optional-class 자동 설정 테스트 20개,
  detekt 통과.
- #1358: network I/O 없는 context 계약 테스트 10개와 실제 MongoDB
  Testcontainers 코루틴 통합 테스트 31개(12.2초) 통과. context와 container
  실행을 분리해 mock 기반 order/lifecycle 검증이 container startup에
  의존하지 않도록 했으며, container는 직렬로 한 번만 실행했다.
- 누적 Cassandra 강제 재실행은 181개까지 진행한 뒤 Gradle daemon이
  `Shutdown in progress`로 사라지고 MockK class redefinition 예외가 겹쳐
  18개 initialization failure를 남겼다. 당시 Docker에는 실행 중인 orphan
  container가 없었고 daemon도 종료 상태였다. 로그와 상태를 보존한 뒤 허용된
  1회 재시도를 일반 test 명령으로 수행해 269개 전체가 22.6초에 통과했다.
  따라서 이 실패는 코드 회귀가 아닌 일회성 daemon/JDK instrumentation
  환경 증거로 남기며, 추가 재시도는 하지 않았다.

## train 운영과 되돌리기

각 child는 앞 child의 정확한 HEAD를 base로 삼는다. predecessor가 merge된
뒤에는 `git range-diff`로 patch series를 확인하고, remote가 예상한 이전 SHA일
때만 명시적인 `--force-with-lease`로 successor를 restack한다. predecessor를
되돌릴 때는 merge SHA를 보존한 별도 revert PR을 만들고 후속 merge를 중단한다.
Mongo migration을 즉시 적용할 수 없는 소비자는 legacy namespace를 지원하는
이전 artifact를 임시 pin한 뒤 설정을 바꾸고 Boot 4.1 artifact로 복귀한다.

## 남은 게이트

로컬 누적 테스트와 문서 parity는 이 train에서 확인하지만, 각 PR의 hosted
CI/Nightly exact-head run, required check, review approval과 mergeability는 PR
생성 뒤 live GitHub에서 다시 확인해야 한다. 병합·자동 병합·branch/worktree
정리는 fresh 명시 승인이 있을 때까지 수행하지 않는다.
