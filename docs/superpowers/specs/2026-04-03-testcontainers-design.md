# testing/testcontainers 모듈 리팩토링 설계 스펙

- **작성일**: 2026-04-03
- **모듈**: `testing/testcontainers` (`bluetape4k-testcontainers`)
- **목표**: 계약 강화, 키 명명 규칙 통일, singleton/fresh API 분리, 시스템 프로퍼티 등록 reversible화

---

## 1. 현재 문제점 분석

### 1.1 PropertyExportingServer 계약 부재

현재 `GenericServer` 인터페이스는 `port`와 `url`만 정의하며, 프로퍼티 export 로직은 각 서버의 `start()` 메서드에 **자유 형식으로 흩어져** 있다.

- `writeToSystemProperties(NAME, extraProps)` 호출이 서버마다 다른 키와 다른 값 조합 사용
- 어떤 서버가 어떤 프로퍼티를 export하는지 인터페이스 계약으로 보장할 수 없음
- 새 서버 추가 시 export 누락 가능

### 1.2 키 명명 규칙 불일치

현재 40+ 서버에서 사용하는 키 패턴이 3가지 이상 혼재:

| 서버         | 현재 키                                   | 문제                      |
|------------|----------------------------------------|-------------------------|
| JDBC 서버들   | `driver-class-name`, `jdbc-url`        | kebab-case              |
| Kafka      | `bootstrapServers`                     | camelCase               |
| Prometheus | `server.port`, `graphiteExporter.port` | dot-case + camelCase 혼재 |
| RabbitMQ   | `amqp.port`, `rabbitmq.http.port`      | dot-case (일관됨)          |
| Cassandra  | `cql.port`                             | dot-case                |
| Vault      | `token`                                | 단일 단어                   |
| Ignite3    | `rest.port`                            | dot-case                |

**기본 3개 키** (`host`, `port`, `url`)는 `writeToSystemProperties`에서 일관 처리되지만, extra props가 제각각이다.

### 1.3 singleton/fresh instance 구분 불명확

- `Launcher.xxx` lazy 프로퍼티 = singleton 의미이나 **immutable 보장 없음**
- `LocalStackServer.Launcher.getLocalStack(vararg services)`: 호출 때마다 새 인스턴스 생성 -> 팩토리 역할이지만 `Launcher` 안에 위치
- `PostgreSQLServer.Launcher.withExtensions()`: 매번 새 인스턴스 -> singleton이 아님에도 `Launcher` 소속
- `PrometheusServer.Launcher`에 `prometheus`(랜덤 포트)와 `defaultPrometheus`(고정 포트) 2개의 싱글턴 공존

### 1.4 시스템 프로퍼티 비복원성

- `writeToSystemProperties`는 `System.setProperty`만 호출
- 비-singleton 테스트에서 서로 다른 서버가 같은 NAME으로 프로퍼티를 덮어쓸 수 있음
- 테스트 격리를 위한 cleanup 수단 없음

### 1.5 Spring 의존성 혼재

- `KafkaServer.kt`: `org.springframework.kafka.*` 9개 import (Launcher.Spring 내부 객체)
- `ElasticsearchServer.kt`: `org.springframework.data.elasticsearch.client.ClientConfiguration`
- `OpenSearchServer.kt`: `org.springframework.data.elasticsearch.client.ClientConfiguration`
- `ElasticsearchOssServer.kt`: 동일
- 이 import들 때문에 spring-kafka, spring-data-elasticsearch가 `compileOnly`에 포함되어야 함

---

## 2. 설계 접근법 비교

### 접근법 A: 인터페이스 도입 + 점진적 마이그레이션 (권장)

새 `PropertyExportingServer` 인터페이스를 만들고, 기존 `writeToSystemProperties` 확장 함수는 내부적으로 이 인터페이스를 사용하도록 위임. 기존 서버들을 순차적으로 구현.

- **장점**: 기존 코드 호환성 유지, 한 서버씩 마이그레이션 가능
- **단점**: 마이그레이션 완료까지 두 패턴 공존

### 접근법 B: GenericServer 자체를 확장

`GenericServer`에 `propertyNamespace`, `properties()` 추가. 기존 `GenericServer` 구현체 모두 일괄 수정.

- **장점**: 인터페이스 하나로 통일
- **단점**: 기존 GenericServer가 ContainerState 확장이므로 기본 구현이 필요. 40+ 서버 일괄 수정 부담

### 접근법 C: 어노테이션 기반 선언

`@ExportProperty` 어노테이션으로 프로퍼티 선언, 리플렉션으로 수집.

- **장점**: 선언적
- **단점**: 런타임 리플렉션 오버헤드, 타입 안전성 약화, bluetape4k 설계 철학에 부적합

### 선택: 접근법 A (인터페이스 도입 + 점진적 마이그레이션)

---

## 3. 상세 설계

### 3.1 PropertyExportingServer 인터페이스

```kotlin
package io.bluetape4k.testcontainers

/**
 * 시스템 프로퍼티로 서버 연결 정보를 export하는 서버의 계약입니다.
 *
 * 모든 프로퍼티 키는 `testcontainers.{propertyNamespace}.{key}` 형식을 따릅니다.
 * 키는 dot-separated lowercase 규칙을 사용합니다 (예: `jdbc.url`, `bootstrap.servers`).
 */
interface PropertyExportingServer: GenericServer {

    /**
     * 시스템 프로퍼티 네임스페이스입니다.
     * 예: `"postgresql"` -> `testcontainers.postgresql.*`
     */
    val propertyNamespace: String

    /**
     * export할 프로퍼티 키 이름 집합을 반환합니다 (값 없음).
     * 컨테이너 start() 전에도 호출 가능합니다.
     * contract test에서 키 명명 규칙 검증에 사용합니다.
     *
     * 기본 키(`host`, `port`, `url`)는 포함하지 않습니다.
     *
     * @return 서버별 추가 프로퍼티 키 집합
     */
    fun propertyKeys(): Set<String> = emptySet()

    /**
     * export할 프로퍼티 키-값 쌍을 반환합니다.
     * 컨테이너 start() 후에만 호출 가능합니다 — start() 전 호출 시 포트 미매핑으로
     * `IllegalStateException`이 발생할 수 있습니다.
     * `writeToSystemProperties()` 및 `registerSystemProperties()`가 이 메서드를 사용합니다.
     *
     * 키는 네임스페이스 이하의 상대 경로입니다 (예: `"jdbc.url"`, `"bootstrap.servers"`).
     * 기본 프로퍼티(`host`, `port`, `url`)는 자동 포함되므로 여기서 반복할 필요 없습니다.
     *
     * @return 서버별 추가 프로퍼티 맵
     * @throws IllegalStateException 컨테이너가 시작되지 않은 경우
     */
    fun properties(): Map<String, String> = emptyMap()
}
```

### 3.2 writeToSystemProperties 개선

```kotlin
/**
 * [PropertyExportingServer]의 프로퍼티를 시스템 프로퍼티로 등록합니다.
 * 기존 `writeToSystemProperties(name, extraProps)` 시그니처는 @Deprecated로 유지합니다.
 */
fun <T: PropertyExportingServer> T.writeToSystemProperties() {
    writeToSystemProperties(propertyNamespace, properties())
}

/**
 * 시스템 프로퍼티를 등록하고 복원 가능한 [AutoCloseable]을 반환합니다.
 * `close()` 호출 시 등록 전 값(또는 null이면 제거)으로 복원합니다.
 */
fun <T: PropertyExportingServer> T.registerSystemProperties(): AutoCloseable {
    val baseKey = "$SERVER_PREFIX.$propertyNamespace"
    val allProps = linkedMapOf(
        "$baseKey.host" to host,
        "$baseKey.port" to port.toString(),
        "$baseKey.url" to url,
    )
    properties().forEach { (key, value) ->
        allProps["$baseKey.$key"] = value
    }

    // 이전 값 스냅샷
    val previousValues: Map<String, String?> = allProps.keys.associateWith { System.getProperty(it) }

    // 등록
    allProps.forEach { (key, value) -> System.setProperty(key, value) }

    log.info {
        buildString {
            appendLine()
            appendLine("Registered $propertyNamespace Server properties:")
            allProps.forEach { (key, value) -> appendLine("\t$key=$value") }
        }
    }

    return AutoCloseable {
        previousValues.forEach { (key, prevValue) ->
            if (prevValue != null) {
                System.setProperty(key, prevValue)
            } else {
                System.clearProperty(key)
            }
        }
        log.info { "Restored system properties for $propertyNamespace" }
    }
}
```

### 3.3 키 명명 규칙 표준

**규칙**: `testcontainers.{namespace}.{key}` 에서 `{key}`는 **dot-separated lowercase**만 허용.

#### JDBC 서버 (JdbcServer 구현체)

| 현재 키                | 신규 키                |
|---------------------|---------------------|
| `driver-class-name` | `driver.class.name` |
| `jdbc-url`          | `jdbc.url`          |
| `username`          | `username`          |
| `password`          | `password`          |
| `database`          | `database`          |

```kotlin
// JdbcServer 확장 — buildJdbcProperties() 대체
fun <T: JdbcServer> T.buildDotSeparatedJdbcProperties(): Map<String, String> {
    return buildMap {
        put("driver.class.name", getDriverClassName())
        put("jdbc.url", getJdbcUrl())
        getUsername()?.let { put("username", it) }
        getPassword()?.let { put("password", it) }
        getDatabaseName()?.let { put("database", it) }
    }
}
```

#### 전체 서버 키 목록

| 서버                 | namespace       | 추가 키                                                                |
|--------------------|-----------------|---------------------------------------------------------------------|
| PostgreSQLServer   | `postgresql`    | `driver.class.name`, `jdbc.url`, `username`, `password`, `database` |
| PostgisServer      | `postgis`       | (동일)                                                                |
| PgvectorServer     | `pgvector`      | (동일)                                                                |
| MySQL8Server       | `mysql8`        | (동일)                                                                |
| MySQL5Server       | `mysql5`        | (동일)                                                                |
| MariaDBServer      | `mariadb`       | (동일)                                                                |
| CockroachServer    | `cockroach`     | (동일)                                                                |
| ClickHouseServer   | `clickhouse`    | (동일)                                                                |
| TrinoServer        | `trino`         | `driver.class.name`, `jdbc.url`, `username`, `password`             |
| RedisServer        | `redis`         | (기본 host/port/url만)                                                 |
| RedisClusterServer | `redis.cluster` | `nodes`, `urls`, `nodes.0`..`nodes.5` (인덱스별 노드)                     |

> **참고**: `redis.cluster` 네임스페이스는 점(
`.`)을 포함하므로 네임스페이스 구분자와 충돌 가능성이 있음. 파싱 시 첫 번째 점 이후 전체를 키로 취급하는 규칙 필요. | MongoDBServer |
`mongodb` | (기본만) | | CassandraServer | `cassandra` | `cql.port` | | HazelcastServer |
`hazelcast` | (기본만) | | Ignite2Server | `ignite2` | (기본만) | | Ignite3Server | `ignite3` |
`rest.port` | | ElasticsearchServer | `elasticsearch` | (기본만) | | ElasticsearchOssServer |
`elasticsearch.oss` | (기본만) | | OpenSearchServer | `opensearch` | (기본만) | | KafkaServer | `kafka` | `bootstrap.servers`,
`bound.port.numbers` | | RedpandaServer | `redpanda` | `bootstrap.servers`, `schema.registry.url` | | PulsarServer |
`pulsar` | `service.url`, `admin.url` | | RabbitMQServer | `rabbitmq` | `amqp.port`, `amqps.port`, `http.port`,
`https.port` | | NatsServer | `nats` | `nats.url` | | LocalStackServer | `localstack` | (기본만) | | MinIOServer |
`minio` | `access.key`, `secret.key`, `endpoint` | | PrometheusServer | `prometheus` | `server.port`,
`pushgateway.port`, `graphite.exporter.port` | | JaegerServer | `jaeger` | `thrift.port`,
`query.port` | | ZipkinServer | `zipkin` | (기본만) | | ZooKeeperServer | `zookeeper` | (기본만) | | VaultServer | `vault` |
`token` | | ConsulServer | `consul` | `http.port`, `dns.port` | | Neo4jServer | `neo4j` | `bolt.port`,
`http.port` | | MemgraphServer | `memgraph` | `bolt.port` | | PostgreSQLAgeServer | `postgresql.age` | JDBC 키 +
`age.graph.name` | | OllamaServer | `ollama` | (기본만) | | ChromaDBServer | `chromadb` | (기본만) | | InfluxDBServer |
`influxdb` | `organization`, `bucket`, `admin.token`, `username` | | HttpbinServer |
`httpbin` | (기본만) | | HttpbinHttp2Server | `httpbin.http2` | (기본만) | | NginxServer |
`nginx` | (기본만) | | WireMockServer | `wiremock` | `http.port`, `https.port` | | ToxiproxyServer | `toxiproxy` |
`control.port` | | KeycloakServer | `keycloak` | `auth.url`, `realm`, `admin.username`, `admin.password` |

### 3.4 singleton/fresh instance API 분리

#### 현재 패턴

```kotlin
object Launcher {
    val postgres: PostgreSQLServer by lazy { ... }  // singleton
    fun withExtensions(vararg ext: String): PostgreSQLServer = ...  // fresh instance
}
```

#### 개선 패턴

```kotlin
/**
 * 테스트에서 재사용할 서버 싱글턴과 팩토리를 제공합니다.
 */
object Launcher {
    /**
     * JVM 전역 공유 인스턴스입니다.
     * 기본 설정으로 시작되며 immutable합니다.
     */
    val shared: PostgreSQLServer by lazy {
        PostgreSQLServer().apply {
            start()
            ShutdownQueue.register(this)
        }
    }

    // 하위 호환: 기존 이름 유지
    @Deprecated("Use shared instead", ReplaceWith("shared"))
    val postgres: PostgreSQLServer get() = shared
}

// PrometheusServer.Launcher 개선 예시:
object Launcher {
    val shared: PrometheusServer by lazy { ... }

    @Deprecated("Use shared instead", ReplaceWith("shared"))
    val prometheus: PrometheusServer get() = shared

    /**
     * 기본 포트(9090)를 사용하는 공유 인스턴스입니다.
     * 포트 충돌 방지가 필요한 경우 사용합니다.
     */
    val sharedWithDefaultPort: PrometheusServer by lazy {
        PrometheusServer(useDefaultPort = true).apply {
            start()
            ShutdownQueue.register(this)
        }
    }

    @Deprecated("Use sharedWithDefaultPort instead", ReplaceWith("sharedWithDefaultPort"))
    val defaultPrometheus: PrometheusServer get() = sharedWithDefaultPort
}

companion object: KLogging() {
    // ...

    /**
     * 새 [PostgreSQLServer] 인스턴스를 생성합니다.
     * 호출자가 start()/stop() 생명주기를 관리합니다.
     */
    @JvmStatic
    operator fun invoke(...): PostgreSQLServer {
        ...
    }
}
```

**규칙 정리**:

| API                         | 용도                          | 생명주기                |
|-----------------------------|-----------------------------|---------------------|
| `XxxServer(...)` (invoke)   | fresh instance 생성           | 호출자 관리              |
| `XxxServer.Launcher.shared` | JVM 전역 공유 싱글턴               | ShutdownQueue 자동 관리 |
| (기존 이름 e.g. `postgres`)     | `@Deprecated`, `shared`로 위임 | 동일                  |

**LocalStackServer 개선**:

```kotlin
object Launcher {
    val shared: LocalStackServer by lazy {
        LocalStackServer()
            .withServices(*DEFAULT_SERVICES.toTypedArray())
            .apply {
                start()
                ShutdownQueue.register(this)
            }
    }

    /**
     * 지정된 서비스로 새 인스턴스를 생성하고 시작합니다.
     * 호출자가 생명주기를 관리하므로 ShutdownQueue에 자동 등록하지 않습니다.
     */
    fun create(vararg services: String): LocalStackServer {
        return LocalStackServer()
            .withServices(*services)
            .apply {
                start()
            }
    }
}
```

### 3.5 AutoCloseable 시스템 프로퍼티 등록

`registerSystemProperties()` 반환값이 `AutoCloseable`이므로 다음과 같이 사용:

```kotlin
// JUnit 5에서 사용 예시
class MyTest {
    private lateinit var propsHandle: AutoCloseable

    @BeforeEach
    fun setup() {
        val server = PostgreSQLServer().apply { start() }
        propsHandle = server.registerSystemProperties()
    }

    @AfterEach
    fun cleanup() {
        propsHandle.close()  // 이전 시스템 프로퍼티 복원
    }
}

// try-with-resources 패턴
server.registerSystemProperties().use {
    // 테스트 코드
}
```

### 3.6 Spring 헬퍼 분리

#### 범위 분석

| 파일                          | Spring 의존성                  | 분리 대상                               |
|-----------------------------|-----------------------------|-------------------------------------|
| `KafkaServer.kt`            | `spring-kafka`              | `Launcher.Spring` 내부 객체 전체          |
| `ElasticsearchServer.kt`    | `spring-data-elasticsearch` | `Launcher.getClientConfiguration()` |
| `OpenSearchServer.kt`       | `spring-data-elasticsearch` | `Launcher.getClientConfiguration()` |
| `ElasticsearchOssServer.kt` | `spring-data-elasticsearch` | `Launcher.getClientConfiguration()` |

#### 분리 전략: 별도 파일 (같은 모듈)

Spring 전용 extension 함수를 별도 파일로 분리한다. 모듈 분리까지는 불필요 (이미 `compileOnly`로 선언).

```
testcontainers/
  src/main/kotlin/io/bluetape4k/testcontainers/
    mq/
      KafkaServer.kt              -- Spring 의존성 제거
      KafkaServerSpringSupport.kt -- Spring 전용 extension 함수
    storage/
      ElasticsearchServer.kt      -- Spring 의존성 제거
      ElasticsearchServerSpringSupport.kt
      OpenSearchServer.kt         -- Spring 의존성 제거
      OpenSearchServerSpringSupport.kt
      ElasticsearchOssServerSpringSupport.kt
```

**KafkaServer** 예시:

```kotlin
// KafkaServerSpringSupport.kt
package io.bluetape4k.testcontainers.mq

import org.springframework.kafka.core.*
// ...

/**
 * KafkaServer에 대한 Spring Kafka 통합 헬퍼입니다.
 */
fun KafkaServer.Launcher.getStringProducerFactory(
    kafkaServer: KafkaServer = kafka,
): ProducerFactory<String, String> {
    ...
}

fun KafkaServer.Launcher.getStringConsumerFactory(
    kafkaServer: KafkaServer = kafka,
): ConsumerFactory<String, String> {
    ...
}

// ... 기존 Spring 내부 객체의 메서드들을 extension function으로 변환
```

### 3.7 Contract Test 설계

```kotlin
package io.bluetape4k.testcontainers

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * 모든 PropertyExportingServer 구현체의 계약을 검증합니다.
 */
class PropertyExportingServerContractTest {

    // 테스트 대상 서버 목록 (실제 Docker 시작은 하지 않음)
    private val serverFactories: List<Pair<String, () -> PropertyExportingServer>> = listOf(
        "PostgreSQLServer" to { PostgreSQLServer() },
        "MySQL8Server" to { MySQL8Server() },
        "RedisServer" to { RedisServer() },
        "KafkaServer" to { KafkaServer() },
        // ... 모든 서버
    )

    /**
     * 1. NAME uniqueness 검증
     */
    @Test
    fun `all servers have unique propertyNamespace`() {
        val namespaces = serverFactories.map { (_, factory) -> factory().propertyNamespace }
        namespaces shouldHaveSize namespaces.toSet().size
    }

    /**
     * 2. propertyNamespace가 dot-separated lowercase 규칙 준수
     */
    @TestFactory
    fun `propertyNamespace follows dot-separated lowercase`(): List<DynamicTest> {
        val pattern = Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")
        return serverFactories.map { (name, factory) ->
            DynamicTest.dynamicTest(name) {
                val server = factory()
                server.propertyNamespace shouldMatch pattern
            }
        }
    }

    /**
     * 3. propertyKeys()가 dot-separated lowercase 규칙 준수
     * Docker 불필요 — propertyKeys()는 start() 전에도 호출 가능
     */
    @TestFactory
    fun `property keys follow dot-separated lowercase`(): List<DynamicTest> {
        val keyPattern = Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")
        return serverFactories.map { (name, factory) ->
            DynamicTest.dynamicTest(name) {
                val server = factory()
                // propertyKeys()는 start() 전에도 호출 가능 (값 없이 키 이름만 반환)
                val keys = server.propertyKeys()
                keys.forEach { key ->
                    key shouldMatch keyPattern
                }
            }
        }
    }

    /**
     * 4. Launcher shared semantics: 같은 인스턴스 반환
     */
    @Test
    fun `Launcher shared returns same instance`() {
        // Docker 환경 필요 — @EnabledIfDockerAvailable 사용
        val a = RedisServer.Launcher.shared
        val b = RedisServer.Launcher.shared
        a shouldBeSameInstanceAs b
    }

    /**
     * 5. registerSystemProperties 복원 검증
     */
    @Test
    fun `registerSystemProperties restores previous values on close`() {
        // Docker 환경 필요
        val key = "testcontainers.redis.host"
        System.setProperty(key, "original-value")

        val server = RedisServer.Launcher.shared
        val handle = server.registerSystemProperties()

        System.getProperty(key) shouldNotBe "original-value"  // 덮어씌워짐

        handle.close()

        System.getProperty(key) shouldBe "original-value"  // 복원됨
    }
}
```

---

## 4. 마이그레이션 호환성 전략

### 4.1 하위 호환 보장

1. **기존 `writeToSystemProperties(name, extraProps)` 확장 함수**: 유지 (삭제하지 않음)
2. **기존 `buildJdbcProperties()`**: `@Deprecated`, 신규 `buildDotSeparatedJdbcProperties()` 병행
3. **기존 Launcher 프로퍼티명** (e.g. `Launcher.postgres`): `@Deprecated` + `get() = shared`
4. **기존 프로퍼티 키** (e.g. `jdbc-url`): Phase 1에서 **양쪽 키 동시 등록**, Phase 2에서 구 키 제거

### 4.2 양쪽 키 동시 등록 (Phase 1)

```kotlin
// Phase 1: 하위 호환을 위해 구 키와 신 키 모두 등록
fun <T: JdbcServer> T.buildJdbcPropertiesCompat(): Map<String, String> {
    return buildMap {
        // 신규 키
        put("driver.class.name", getDriverClassName())
        put("jdbc.url", getJdbcUrl())
        getUsername()?.let { put("username", it) }
        getPassword()?.let { put("password", it) }
        getDatabaseName()?.let { put("database", it) }
        // 하위 호환 키 (deprecated)
        put("driver-class-name", getDriverClassName())
        put("jdbc-url", getJdbcUrl())
    }
}
```

### 4.3 비-JDBC 서버 호환 전략

JDBC 서버의
`buildJdbcPropertiesCompat()`과 마찬가지로, 비-JDBC 서버(Kafka, Pulsar 등)에서도 기존 camelCase 키와 신규 dot-separated 키를 동시 등록할 수 있는 범용 유틸리티를 제공한다.

```kotlin
/**
 * 신규 키가 존재할 때 하위 호환 키도 함께 등록합니다.
 *
 * @param mapping 신규 키 → 구 키 매핑 (예: "bootstrap.servers" to "bootstrapServers")
 * @return 신규 키 + 하위 호환 키가 모두 포함된 프로퍼티 맵
 */
fun Map<String, String>.withCompatKeys(mapping: Map<String, String>): Map<String, String> {
    val result = this.toMutableMap()
    mapping.forEach { (newKey, oldKey) ->
        result[newKey]?.let { result[oldKey] = it }  // 신규 키 값이 있으면 구 키도 등록
    }
    return result
}

// KafkaServer 사용 예시:
// KafkaServer.properties() 구현 내부:
mapOf(
    "bootstrap.servers" to bootstrapServers,
    "bound.port.numbers" to boundPortNumbers
).withCompatKeys(
    mapOf(
        "bootstrap.servers" to "bootstrapServers",
        "bound.port.numbers" to "boundPortNumbers"
    )
)
```

**Phase 1**에서 비-JDBC 서버도 `withCompatKeys()`를 사용하여 양쪽 키를 동시 등록한다.

### 4.4 Phase 계획

| Phase   | 내용                                                                     | 시기           |
|---------|------------------------------------------------------------------------|--------------|
| Phase 1 | `PropertyExportingServer` 인터페이스 추가, 양쪽 키 등록, `Launcher.shared` 추가      | 즉시           |
| Phase 2 | 모든 서버 `PropertyExportingServer` 구현 완료, `registerSystemProperties()` 추가 | Phase 1 + 1주 |
| Phase 3 | 구 키 제거, 구 Launcher 프로퍼티명 제거, Spring 헬퍼 분리                              | Phase 2 + 2주 |

---

## 5. 구현 우선순위 및 태스크 목록

### P0 (핵심 — 먼저 구현)

- [ ] **T1**: `PropertyExportingServer` 인터페이스 정의 (`GenericServer.kt` 또는 별도 파일)
- [ ] **T2**: `registerSystemProperties(): AutoCloseable` 확장 함수 구현
- [ ] **T3**: `buildDotSeparatedJdbcProperties()` (dot-separated) 구현 + 기존 `buildJdbcProperties()` `@Deprecated`
- [ ] **T4**: JDBC 서버 6종 마이그레이션 (PostgreSQL, Postgis, Pgvector, MySQL8, MySQL5, MariaDB)
- [ ] **T5**: Redis/Kafka/LocalStack 마이그레이션 (사용 빈도 높은 서버 우선)

### P1 (계약 완성)

- [ ] **T6**: storage 서버 마이그레이션 (Cassandra, MongoDB, Hazelcast, Ignite2/3, Elasticsearch, OpenSearch, MinIO, InfluxDB)
- [ ] **T7**: mq 서버 마이그레이션 (Redpanda, Pulsar, RabbitMQ, NATS)
- [ ] **T8**: infra 서버 마이그레이션 (Prometheus, Jaeger, Zipkin, Vault, Consul, ZooKeeper, Toxiproxy, Keycloak)
- [ ] **T9**: http/graphdb/llm 서버 마이그레이션 (Httpbin, Nginx, WireMock, Neo4j, Memgraph, AGE, Ollama, ChromaDB)
- [ ] **T10**: Launcher `shared` 프로퍼티 추가 + 기존 이름 `@Deprecated`

### P2 (품질 보증)

- [ ] **T11**: Contract test 작성 (namespace uniqueness, 키 규칙, shared semantics, property 복원)
- [ ] **T12**: Spring 헬퍼 분리 (KafkaServerSpringSupport, ElasticsearchServerSpringSupport, OpenSearchServerSpringSupport)
- [ ] **T13**: CockroachServer, ClickHouseServer, TrinoServer, RedisClusterServer 마이그레이션

### P3 (정리)

- [ ] **T14**: 구 키 제거 (Phase 3) — `buildJdbcProperties()`, 구 Launcher 프로퍼티명, 구 프로퍼티 키
- [ ] **T15**: PrometheusServer `Launch` (deprecated) 제거
- [ ] **T16**: README.md 및 CLAUDE.md 업데이트 (`testing/testcontainers/README.md`의 구 키 예시를 신규 dot-separated 키로 업데이트 포함)

---

## 6. 영향 범위

### 직접 영향

- `testing/testcontainers` 모듈 전체 (46개 서버 파일)
- `GenericServer.kt`, `JdbcServer.kt` 인터페이스 파일

### 간접 영향 (프로퍼티 키 변경 시)

프로퍼티 키를 참조하는 외부 모듈:

```
spring-boot3/exposed-jdbc-demo  -> testcontainers.postgresql.jdbc-url 참조 가능성
spring-boot3/exposed-r2dbc-demo -> 동일
data/exposed-jdbc-tests         -> 동일
data/exposed-r2dbc-tests        -> 동일
```

Phase 1에서 양쪽 키 동시 등록으로 하위 호환 보장.

### build.gradle.kts 변경 불필요

`compileOnly` 의존성 구조는 유지. Spring 헬퍼 분리는 파일 분리만으로 충분.

---

## 7. 리스크 및 완화 전략

| 리스크                                 | 영향 | 완화                                                                                                                                                                                      |
|-------------------------------------|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 프로퍼티 키 변경으로 기존 테스트 실패               | 중  | Phase 1에서 양쪽 키 동시 등록                                                                                                                                                                    |
| 46개 서버 일괄 수정 중 누락                   | 중  | Contract test로 누락 서버 자동 탐지                                                                                                                                                              |
| Launcher.shared 도입으로 기존 코드 컴파일 경고   | 낮  | @Deprecated + ReplaceWith로 IDE 자동 수정                                                                                                                                                    |
| registerSystemProperties() 멀티스레드 경합 | 낮  | 테스트는 보통 단일 스레드; 문서로 주의사항 명시                                                                                                                                                             |
| 병렬 테스트 스레드 안전성                      | 중  | `registerSystemProperties()`는 JVM-global `System.setProperty`를 수정하므로 병렬 실행 클래스에서 `previousValues` snapshot이 충돌할 수 있음. `@Execution(ExecutionMode.SAME_THREAD)` 사용 권장을 KDoc 및 README에 문서화 |
| contract test 서버 목록 드리프트            | 낮  | contract test의 `serverFactories` 목록이 하드코딩되어 새 서버 추가 시 수동 업데이트 필요. 향후 `ServiceLoader` 또는 classpath scanning 기반 자동 검색 도입 고려                                                               |
