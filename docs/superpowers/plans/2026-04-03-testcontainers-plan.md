# testing/testcontainers 리팩토링 구현 플랜

- **작성일**: 2026-04-03
- **스펙 파일**: `docs/superpowers/specs/2026-04-03-testcontainers-design.md`
- **모듈**: `testing/testcontainers` (`bluetape4k-testcontainers`)
- **목표**: `PropertyExportingServer` 인터페이스 도입, 키 명명 규칙 통일, singleton/fresh API 분리, 시스템 프로퍼티 등록 reversible화, Spring 헬퍼 분리

---

## Phase 0: 기반 인터페이스 (모든 다른 태스크의 기반)

### T0-1: PropertyExportingServer 인터페이스 정의

- **complexity: high**
- **의존성**: 없음
- **설명**: `GenericServer`를 확장하는 `PropertyExportingServer` 인터페이스를 별도 파일로 정의한다. `propertyNamespace: String`,
  `propertyKeys(): Set<String>`, `properties(): Map<String, String>` 3개 멤버를 포함하며, `propertyKeys()`와 `properties()`는
  `emptySet()`/`emptyMap()` 기본 구현을 제공한다. `propertyKeys()`는 컨테이너 start() 전에도 호출 가능해야 하며,
  `properties()`는 start() 후에만 유효하다 (KDoc에 명시).
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt`
- **완료 기준**: 컴파일 통과, 기존 `GenericServer` 코드 변경 없이 새 인터페이스 추가. KDoc 한국어 작성 완료.

### T0-2: registerSystemProperties(): AutoCloseable 구현

- **complexity: high**
- **의존성**: T0-1
- **설명**: `PropertyExportingServer`의 확장 함수 `registerSystemProperties(): AutoCloseable`을 구현한다. 기존
  `writeToSystemProperties(name, extraProps)` 확장 함수는 그대로 유지하고, 새로운 `writeToSystemProperties()` (인자 없는 버전)도
  `PropertyExportingServer` 전용으로 추가한다. `registerSystemProperties()`는 등록 전 시스템 프로퍼티 스냅샷을 저장하고,
  `close()` 호출 시 이전 값(또는 null이면 `clearProperty`)으로 복원한다. 구현은 `GenericServer.kt` 파일 하단 또는
  `PropertyExportingServer.kt` 하단에 확장 함수로 배치한다.
- **파일**:
  `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt` (인터페이스와 동일 파일)
- **완료 기준**: 단위 테스트로 프로퍼티 등록/복원 검증 가능. 기존 `writeToSystemProperties(name, extraProps)` 시그니처 유지.

### T0-2-test: registerSystemProperties() 단위 테스트

- **complexity: medium**
- **의존성**: T0-2
- **설명**: Docker 불필요 (`MockPropertyExportingServer` 사용). 테스트 케이스:
    1. 존재하지 않던 프로퍼티: `close()` 후 `null`로 복원
    2. 기존 값이 있던 프로퍼티: `close()` 후 이전 값으로 복원
    3. `close()` 두 번 호출 시 idempotent (예외 없음)
    4. JUnit 5 `@BeforeEach`/`@AfterEach` 패턴으로 사용
    5. `use {}` 블록 패턴으로 사용
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/RegisterSystemPropertiesTest.kt`
- **완료 기준**: 모든 5개 케이스 통과, Docker 없이 실행 가능.

### T0-3: buildDotSeparatedJdbcProperties() + buildJdbcPropertiesCompat() 유틸리티

- **complexity: high**
- **의존성**: T0-1
- **설명**: `JdbcServer.kt`에 두 개의 신규 확장 함수를 추가한다.
    1. `buildDotSeparatedJdbcProperties()`: dot-separated 키만 반환 (`driver.class.name`, `jdbc.url`, `username`,
       `password`, `database`)
    2. `buildJdbcPropertiesCompat()`: 신규 dot-separated 키 + 기존 kebab-case 키 (`driver-class-name`, `jdbc-url`) 모두 포함

    - 기존 `buildJdbcProperties()`는
      `@Deprecated("Use buildDotSeparatedJdbcProperties() or buildJdbcPropertiesCompat() instead")` 어노테이션 추가하되 삭제하지 않는다.
    - 하위 호환성: Phase 1 마이그레이션 기간에 `buildJdbcPropertiesCompat()`을 사용하여 양쪽 키 동시 등록.
- **타입 변경**: 기존 `buildJdbcProperties(): Map<String, Any?>` →
  `buildDotSeparatedJdbcProperties(): Map<String, String>` (null 값 제거, 빈 문자열 미포함)
- **의미 변화**: null 값은 맵에 포함되지 않음 (기존에는 `null`로 포함됨).
  `writeToSystemProperties()`는 이미 null 필터링하므로 시스템 프로퍼티 등록에 영향 없음. 단, 맵 직접 참조 시 차이 있음.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/JdbcServer.kt`
- **완료 기준**: 컴파일 통과, 기존 `buildJdbcProperties()` 호출부 영향 없음. `@Deprecated` IDE 경고 확인. 타입 변경 KDoc 문서화.

### T0-4: withCompatKeys() 비-JDBC 범용 호환 유틸리티

- **complexity: medium**
- **의존성**: T0-1
- **설명**:
  `Map<String, String>.withCompatKeys(mapping: Map<String, String>): Map<String, String>` 확장 함수를 구현한다. 신규 dot-separated 키 → 기존 camelCase 키 매핑을 받아 양쪽 키를 모두 포함하는 맵을 반환한다. Kafka의
  `bootstrapServers` → `bootstrap.servers`, Pulsar의 `serviceUrl` → `service.url` 등 비-JDBC 서버의 하위 호환에 사용한다.
- **파일**:
  `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt` (유틸리티 함수로 동일 파일에 배치)
- **완료 기준**: 컴파일 통과, 단위 테스트로 매핑 동작 검증.

---

## Phase 1: JDBC 서버 마이그레이션 (Phase 0 완료 후)

모든 JDBC 서버에 대해: `PropertyExportingServer` 구현, `propertyNamespace`/`propertyKeys()`/`properties()` 오버라이드, `start()`에서
`buildJdbcPropertiesCompat()` 사용, `Launcher.shared` 추가 + 기존 이름 `@Deprecated`.

### T1-1: PostgreSQLServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: `PostgreSQLServer`가 `PropertyExportingServer`를 구현하도록 수정한다.
    - `propertyNamespace = "postgresql"` (기존 `NAME` 상수 활용)
    - `propertyKeys()`: `setOf("driver.class.name", "jdbc.url", "username", "password", "database")`
    - `properties()`: `buildDotSeparatedJdbcProperties()` 반환
    - `start()`: `writeToSystemProperties(NAME, buildJdbcPropertiesCompat())` 로 양쪽 키 동시 등록
    - `Launcher.shared` 추가, `Launcher.postgres`는 `@Deprecated("Use shared instead", ReplaceWith("shared"))` +
      `get() = shared`
    - `Launcher.withExtensions()`은 팩토리이므로 유지 (Launcher 밖 companion의 `create` 팩토리는 불필요 — 이미 `invoke` +
      `withExtensions` 체이닝으로 커버)
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/PostgreSQLServer.kt`
- **완료 기준**: 컴파일 통과, 기존 `Launcher.postgres` 접근 시 `@Deprecated` 경고, `Launcher.shared` 동일 인스턴스 반환.

### T1-2: PostgisServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "postgis"`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/PostgisServer.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

### T1-3: PgvectorServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "pgvector"`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/PgvectorServer.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

### T1-4: MySQL8Server 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "mysql8"`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/MySQL8Server.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

### T1-5: MySQL5Server 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "mysql5"`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/MySQL5Server.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

### T1-6: MariaDBServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "mariadb"`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/MariaDBServer.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

### T1-7: CockroachServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "cockroach"`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/CockroachServer.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

### T1-8: ClickHouseServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "clickhouse"`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/ClickHouseServer.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

### T1-9: TrinoServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: T1-1과 동일 패턴. `propertyNamespace = "trino"`. Trino는 `getDatabaseName()` 없음 → `propertyKeys()`에서
  `database` 키 제외.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/TrinoServer.kt`
- **완료 기준**: 컴파일 통과, `PropertyExportingServer` 구현 완료.

---

## Phase 2: 비-JDBC 서버 마이그레이션 (Phase 0 완료 후, Phase 1과 병렬 가능)

### T2-1: RedisServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "redis"`. 추가 프로퍼티 없음 (기본 host/port/url만).
  `Launcher.shared` 추가 + 기존 이름 `@Deprecated`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/RedisServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-2: RedisClusterServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "redis.cluster"`. `propertyKeys()`: `nodes`, `urls`,
  `nodes.0`~`nodes.5`. `properties()`에서 클러스터 노드 정보 반환. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/RedisClusterServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-3: KafkaServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-4
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "kafka"`. `propertyKeys()`: `bootstrap.servers`,
  `bound.port.numbers`. `properties()`에서 `withCompatKeys()` 사용하여 기존 camelCase 키(`bootstrapServers`,
  `boundPortNumbers`)도 동시 등록 (compat 키는 `properties()` 반환값에서 `withCompatKeys()`로 포함하며, `start()` 내부가 아님). `start()`에서
  `writeToSystemProperties(NAME, properties())` 대신 `writeToSystemProperties()` (인자 없는 버전) 사용. `Launcher.shared` 추가,
  `Launcher.kafka` `@Deprecated`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/KafkaServer.kt`
- **완료 기준**: 컴파일 통과, 기존 `bootstrapServers` 키와 신규 `bootstrap.servers` 키 모두 시스템 프로퍼티에 등록.

### T2-4: RedpandaServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-4
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "redpanda"`. `propertyKeys()`: `bootstrap.servers`,
  `schema.registry.url`. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/RedpandaServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-5: PulsarServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-4
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "pulsar"`. `propertyKeys()`: `service.url`, `admin.url`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/PulsarServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-6: RabbitMQServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "rabbitmq"`. `propertyKeys()`: `amqp.port`, `amqps.port`,
  `http.port`, `https.port`. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/RabbitMQServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-7: NatsServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "nats"`. `propertyKeys()`: `nats.url`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/NatsServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-8: LocalStackServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "localstack"`. 추가 프로퍼티 없음 (기본만).
  `Launcher.shared` 추가 (기본 서비스로 시작), `Launcher.create(vararg services)` 팩토리 추가 (호출자 생명주기 관리). 기존
  `Launcher.getLocalStack()`이 있다면 `@Deprecated`.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/LocalStackServer.kt`
- **완료 기준**: 컴파일 통과, `Launcher.shared`와 `Launcher.create()` 분리 명확.

### T2-9: CassandraServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "cassandra"`. `propertyKeys()`: `cql.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/CassandraServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-10: MongoDBServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "mongodb"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/MongoDBServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-11: HazelcastServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "hazelcast"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/HazelcastServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-12: Ignite2Server 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "ignite2"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/Ignite2Server.kt`
- **완료 기준**: 컴파일 통과.

### T2-13: Ignite3Server 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "ignite3"`. `propertyKeys()`: `rest.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/Ignite3Server.kt`
- **완료 기준**: 컴파일 통과.

### T2-14: ElasticsearchServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "elasticsearch"`. 추가 프로퍼티 없음 (기본만).
  `Launcher.shared` 추가. Spring `ClientConfiguration` 관련 코드는 T4-2에서 별도 파일로 분리.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/ElasticsearchServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-15: ElasticsearchOssServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "elasticsearch.oss"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/ElasticsearchOssServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-16: OpenSearchServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "opensearch"`. 추가 프로퍼티 없음.
  `Launcher.shared` 추가. Spring 코드는 T4-3에서 분리.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/OpenSearchServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-17: MinIOServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "minio"`. `propertyKeys()`: `access.key`, `secret.key`,
  `endpoint`. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/MinIOServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-18: InfluxDBServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "influxdb"`. `propertyKeys()`: `organization`, `bucket`,
  `admin.token`, `username`. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/InfluxDBServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-19: PrometheusServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-4
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "prometheus"`. `propertyKeys()`: `server.port`,
  `pushgateway.port`, `graphite.exporter.port`. `properties()`에서 `withCompatKeys()` 사용하여 기존
  `graphiteExporter.port` 키도 동시 등록. `Launcher.shared` 추가, `Launcher.prometheus` `@Deprecated`.
  `Launcher.sharedWithDefaultPort` 추가, `Launcher.defaultPrometheus` `@Deprecated`. `Launch` deprecated object 유지.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/PrometheusServer.kt`
- **완료 기준**: 컴파일 통과, `Launcher.shared`/`Launcher.sharedWithDefaultPort` 정상 동작.

### T2-20: JaegerServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "jaeger"`. `propertyKeys()`: `thrift.port`, `query.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/JaegerServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-21: ZipkinServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "zipkin"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/ZipkinServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-22: VaultServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "vault"`. `propertyKeys()`: `token`. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/VaultServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-23: ConsulServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "consul"`. `propertyKeys()`: `http.port`, `dns.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/ConsulServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-24: ZooKeeperServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "zookeeper"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/ZooKeeperServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-25: ToxiproxyServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "toxiproxy"`. `propertyKeys()`: `control.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/ToxiproxyServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-26: KeycloakServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "keycloak"`. `propertyKeys()`: `auth.url`, `realm`,
  `admin.username`, `admin.password`. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/KeycloakServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-27: HttpbinServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "httpbin"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/HttpbinServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-28: HttpbinHttp2Server 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "httpbin.http2"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/HttpbinHttp2Server.kt`
- **완료 기준**: 컴파일 통과.

### T2-29: NginxServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "nginx"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/NginxServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-30: WireMockServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "wiremock"`. `propertyKeys()`: `http.port`, `https.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/WireMockServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-31: Neo4jServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "neo4j"`. `propertyKeys()`: `bolt.port`, `http.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/Neo4jServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-32: MemgraphServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "memgraph"`. `propertyKeys()`: `bolt.port`.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/MemgraphServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-33: PostgreSQLAgeServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2, T0-3
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "postgresql.age"`. JDBC 프로퍼티 + `age.graph.name` 추가 키.
  `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/PostgreSQLAgeServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-34: OllamaServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "ollama"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/llm/OllamaServer.kt`
- **완료 기준**: 컴파일 통과.

### T2-35: ChromaDBServer 마이그레이션

- **complexity: medium**
- **의존성**: T0-1, T0-2
- **설명**: `PropertyExportingServer` 구현. `propertyNamespace = "chromadb"`. 추가 프로퍼티 없음. `Launcher.shared` 추가.
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/llm/ChromaDBServer.kt`
- **완료 기준**: 컴파일 통과.

---

## Phase 3: Launcher API 재설계 (Phase 1, 2와 통합)

> Phase 1, 2의 각 서버 마이그레이션 태스크에 `Launcher.shared` 추가가 포함되어 있으므로, Phase 3는 전체 서버에 걸친 **일괄 검증 및 누락 보완** 태스크이다.

### T3-1: Launcher.shared + create() 일괄 검증 및 보완

- **complexity: high**
- **의존성**: T1-*, T2-* 전체
- **설명**: 모든 서버의 `Launcher` 객체를 검수한다.
    - `shared` lazy 프로퍼티 존재 여부 확인
    - 기존 이름(e.g. `postgres`, `kafka`, `redis`)에 `@Deprecated` + `get() = shared` 위임 확인
    - 팩토리 메서드가 필요한 서버(LocalStack, PostgreSQL withExtensions)에 `create()` 또는 기존 팩토리 패턴 유지 확인
    - `ShutdownQueue.register(this)` 호출 확인
    - 누락된 서버 보완
- **파일**: 모든 서버 파일 (46개)
- **완료 기준**: 모든 서버의 `Launcher.shared` 접근 가능, 기존 이름은 `@Deprecated` 경고 표시.

### T3-2: PrometheusServer sharedWithDefaultPort 처리

- **complexity: medium**
- **의존성**: T2-19
- **설명**: `Launcher.sharedWithDefaultPort` lazy 프로퍼티 추가 (T2-19에서 이미 포함). `Launcher.defaultPrometheus`는
  `@Deprecated("Use sharedWithDefaultPort instead")` + `get() = sharedWithDefaultPort`.
  `Launch` deprecated object는 유지 (삭제는 Phase 3 정리에서 결정).
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/PrometheusServer.kt`
- **완료 기준**: `Launcher.sharedWithDefaultPort` 정상 동작, `@Deprecated` 경고 확인.

---

## Phase 4: Spring 헬퍼 분리

### T4-1: KafkaServer Spring 메서드 분리

- **complexity: medium**
- **의존성**: T2-3
- **설명**: `KafkaServer.kt`의 `Launcher.Spring` 내부 object 전체를 `KafkaServerSpringSupport.kt`로 이동한다.
  `Launcher.Spring`을 extension function으로 변환하거나, deprecated alias로 유지한다.
    - 새 파일에서 `KafkaServer.Launcher`의 extension function으로 재정의
    - Spring import 9개 (`ProducerFactory`, `ConsumerFactory`, `KafkaTemplate`, `DefaultKafkaProducerFactory`,
      `DefaultKafkaConsumerFactory`, `ConcurrentKafkaListenerContainerFactory`, `KafkaListenerContainerFactory`,
      `ConcurrentMessageListenerContainer`, `ContainerProperties`)를 `KafkaServer.kt`에서 제거
    - `KafkaServer.kt`의 `Launcher.Spring` object는
      `@Deprecated("Moved to KafkaServerSpringSupport.kt")` 처리하거나, 새 파일의 함수로 위임
- **파일**:
    - `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/KafkaServerSpringSupport.kt` (신규)
    - `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/KafkaServer.kt` (수정)
- **완료 기준**: `KafkaServer.kt`에서 Spring import 제거, `KafkaServerSpringSupport.kt`에서 동일 기능 제공, 컴파일 통과.

### T4-2: ElasticsearchServer Spring 메서드 분리

- **complexity: medium**
- **의존성**: T2-14
- **설명**: `ElasticsearchServer.kt`의 `Launcher.getClientConfiguration()` 메서드를
  `ElasticsearchServerSpringSupport.kt`로 이동한다. `spring-data-elasticsearch`의 `ClientConfiguration` import을
  `ElasticsearchServer.kt`에서 제거한다.
- **파일**:
    -
    `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/ElasticsearchServerSpringSupport.kt` (신규)
    - `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/ElasticsearchServer.kt` (수정)
- **완료 기준**: `ElasticsearchServer.kt`에서 Spring import 제거, 컴파일 통과.

### T4-3: OpenSearchServer Spring 메서드 분리

- **complexity: medium**
- **의존성**: T2-16
- **설명**: `OpenSearchServer.kt`의 Spring `ClientConfiguration` 관련 코드를 `OpenSearchServerSpringSupport.kt`로 이동한다.
- **파일**:
    -
    `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/OpenSearchServerSpringSupport.kt` (신규)
    - `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/OpenSearchServer.kt` (수정)
- **완료 기준**: `OpenSearchServer.kt`에서 Spring import 제거, 컴파일 통과.

### T4-4: ElasticsearchOssServer Spring 메서드 분리

- **complexity: medium**
- **의존성**: T2-15
- **설명**: `ElasticsearchOssServer.kt`의 Spring 관련 코드를 `ElasticsearchOssServerSpringSupport.kt`로 이동한다.
- **파일**:
    -
    `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/ElasticsearchOssServerSpringSupport.kt` (신규)
    - `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/ElasticsearchOssServer.kt` (수정)
- **완료 기준**: Spring import 제거, 컴파일 통과.

---

## Phase 5: Contract Test 작성

### T5-0: bluetape4k-patterns 체크리스트 검증

- **complexity: low**
- **의존성**: T0-1, T0-2, T0-3 (기반 인터페이스 완료 후)
- **설명**: 기반 인터페이스 및 구현체가 bluetape4k 코딩 컨벤션을 준수하는지 리뷰 체크리스트로 확인한다. 확인 항목:
    - 모든 public 인터페이스/클래스에 한국어 KDoc 작성
    - `companion object : KLogging()` 패턴 (구현체에 적용)
    - `data class`는 `Serializable` + `serialVersionUID`
    - `@Deprecated` 사용 시 `replaceWith` 명시
- **파일**: 해당 없음 (리뷰 체크리스트)
- **완료 기준**: 위 4개 항목 모두 충족 확인.

### T5-1: PropertyExportingServerContractTest 작성

- **complexity: medium**
- **의존성**: T0-1, Phase 1 및 Phase 2의 최소 5개 서버 마이그레이션 완료
- **설명**: 모든 `PropertyExportingServer` 구현체의 계약을 검증하는 테스트 클래스를 작성한다.
    1. **NAME uniqueness**: 모든 서버의 `propertyNamespace`가 고유한지 검증
    2. **propertyNamespace 규칙**: `^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$` 패턴 준수
    3. **propertyKeys() 규칙**: 모든 키가 동일한 dot-separated lowercase 패턴 준수
    4. **Launcher shared semantics**: 같은 인스턴스 반환 검증 (Docker 필요 — `@EnabledIfDockerAvailable` 또는 조건부 실행)
    5. **registerSystemProperties 복원**: 프로퍼티 등록/복원 라운드트립 검증 (Docker 필요)

    - 항목 1~3은 Docker 없이 실행 가능 (`start()` 불필요), 항목 4~5는 Docker 환경 필요
    - `serverFactories: List<Pair<String, () -> PropertyExportingServer>>` 하드코딩 목록 사용
    - `@TestFactory` + `DynamicTest`로 각 서버별 개별 테스트 생성
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/PropertyExportingServerContractTest.kt`
- **완료 기준**: Docker 없는 환경에서 키 규칙 테스트 통과, Docker 환경에서 전체 테스트 통과.

---

## Phase 6: 문서화

### T6-1: testing/testcontainers README.md 업데이트

- **complexity: low**
- **의존성**: Phase 1~5 완료
- **설명**: `testing/testcontainers/README.md`에 다음 내용 추가/수정:
    - 새 키 명명 규칙 (dot-separated lowercase) 설명
    - `PropertyExportingServer` 인터페이스 소개
    - `registerSystemProperties()` 사용 예시
    - `Launcher.shared` 패턴 설명
    - 마이그레이션 가이드 (기존 kebab-case/camelCase 키 → dot-separated 키)
    - 병렬 테스트 시 `@Execution(ExecutionMode.SAME_THREAD)` 권장 사항
- **파일**: `testing/testcontainers/README.md`
- **완료 기준**: README에 신규 패턴 설명 포함.

### T6-2: CLAUDE.md testcontainers 섹션 업데이트

- **complexity: low**
- **의존성**: Phase 1~5 완료
- **설명**: 루트 `CLAUDE.md`의 Testing 섹션에 testcontainers 리팩토링 결과 반영.
    - `PropertyExportingServer` 인터페이스 설명 추가
    - `Launcher.shared` 패턴 언급
    - 서버 구현 시 `propertyNamespace`/`propertyKeys()`/`properties()` 오버라이드 필수 사항 명시
- **파일**: `CLAUDE.md`
- **완료 기준**: CLAUDE.md에 testcontainers 신규 패턴 반영.

---

## Phase 7: 구 키 제거 (지연 실행)

**실행 Gate 조건** (모든 조건 충족 시에만 Phase 7 실행):

1. Phase 6 완료 후 최소 2주 경과
2. 외부 모듈 grep 확인: `rg "jdbc-url|driver-class-name|bootstrapServers|boundPortNumbers" --type kt --type yaml` 결과 0건
3. CI/CD 전체 테스트 통과 확인

### T7-1: buildJdbcProperties() 제거 (JdbcServer.kt)

- **complexity: low**
- **의존성**: Gate 조건 충족
- **설명**: `@Deprecated` 처리된 `buildJdbcProperties()` 함수를 `JdbcServer.kt`에서 완전히 제거한다.

### T7-2: @Deprecated Launcher 별칭 제거 (전체 서버)

- **complexity: low**
- **의존성**: Gate 조건 충족
- **설명**: 전체 서버 파일에서 `@Deprecated` 처리된 `Launcher` 별칭 프로퍼티(예: `postgres`, `kafka`, `redis` 등)를 제거한다.

### T7-3: buildJdbcPropertiesCompat() 및 withCompatKeys() compat 분기 제거

- **complexity: low**
- **의존성**: Gate 조건 충족
- **설명**: `buildJdbcPropertiesCompat()` 함수 및 각 서버 `properties()`의
  `withCompatKeys()` compat 분기를 제거하고, 신규 dot-separated 키만 반환하도록 정리한다.

### T7-4: PrometheusServer Launch deprecated object 제거

- **complexity: low**
- **의존성**: Gate 조건 충족
- **설명**: `PrometheusServer.kt`의 `Launch` deprecated object를 제거한다.

### T7-5: Phase 6 이후 README "마이그레이션 가이드" 섹션 업데이트

- **complexity: low**
- **의존성**: T7-1 ~ T7-4 완료
- **설명**: `testing/testcontainers/README.md`의 "마이그레이션 가이드" 섹션을 "이전 키 제거됨"으로 업데이트하여 구 키가 더 이상 지원되지 않음을 명시한다.

> **주의**: Gate 조건 미충족 시 Phase 7 실행 금지.

---

## 태스크 요약

| Phase   | 태스크                    | Complexity                  | 총 태스크 수 |
|---------|------------------------|-----------------------------|---------|
| Phase 0 | T0-1 ~ T0-4, T0-2-test | high(3) + medium(2)         | 5       |
| Phase 1 | T1-1 ~ T1-9            | medium(9)                   | 9       |
| Phase 2 | T2-1 ~ T2-35           | medium(35)                  | 35      |
| Phase 3 | T3-1 ~ T3-2            | high(1) + medium(1)         | 2       |
| Phase 4 | T4-1 ~ T4-4            | medium(4)                   | 4       |
| Phase 5 | T5-0 ~ T5-1            | low(1) + medium(1)          | 2       |
| Phase 6 | T6-1 ~ T6-2            | low(2)                      | 2       |
| Phase 7 | T7-1 ~ T7-5 (지연 실행)    | low(5)                      | 5       |
| **합계**  |                        | high(4), medium(52), low(8) | **64**  |

## 병렬 실행 가능 그래프

```
T0-1 ─┬─► T0-2 ─┬─► T0-2-test
      ├─► T0-3 ─┤
      └─► T0-4 ─┤
                ├─► T1-1 ~ T1-9  (Phase 1, 서로 독립 → 병렬 가능)
                ├─► T2-1 ~ T2-35 (Phase 2, 서로 독립 → 병렬 가능, Phase 1과도 병렬)
                └─► (Phase 1+2 완료 후)
                    ├─► T3-1
                    ├─► T3-2
                    ├─► T4-1 ~ T4-4 (서로 독립 → 병렬 가능)
                    └─► T5-0 (bluetape4k-patterns 체크리스트)
                        └─► T5-1
                            └─► T6-1, T6-2 (병렬 가능)
                                └─► [2주 경과 + Gate 조건 충족 후]
                                    └─► T7-1 ~ T7-5 (병렬 가능)
```

## 리스크 요약

| 리스크                            | 영향 | 완화                                                             |
|--------------------------------|----|----------------------------------------------------------------|
| 프로퍼티 키 변경으로 기존 테스트 실패          | 중  | `buildJdbcPropertiesCompat()` / `withCompatKeys()`로 양쪽 키 동시 등록 |
| 46개 서버 수정 중 누락                 | 중  | T5-1 Contract test로 자동 탐지                                      |
| Launcher.shared 도입 시 컴파일 경고 폭증 | 낮  | `@Deprecated` + `ReplaceWith`로 IDE 자동 수정                       |
| 병렬 테스트에서 시스템 프로퍼티 경합           | 중  | KDoc + README에 `@Execution(ExecutionMode.SAME_THREAD)` 권장 문서화  |
| Contract test 서버 목록 드리프트       | 낮  | 향후 ServiceLoader/classpath scanning 도입 고려                      |
