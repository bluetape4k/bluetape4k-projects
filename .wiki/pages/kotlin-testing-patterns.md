# Kotlin Testing Patterns

> 마지막 업데이트: 2026-04-07 | 관련 specs: 3개

## 개요

bluetape4k의 테스트 및 문서화 패턴. KDoc 한국어 작성 규칙, Testcontainers 계약 강화, suspend 테스트 패턴을 다룬다.

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| KDoc 인라인 예제 (`@sample` 미사용) | 예제가 KDoc 내부에 있어야 IDE tooltip에 바로 표시됨. `@sample` 태그는 별도 파일 참조 필요 | 2026-04-04 | kdoc-examples-all-modules-design |
| KDoc 한국어 작성 | 프로젝트 언어 정책 | 2026-04-04 | kdoc-examples-all-modules-design |
| PropertyExportingServer 인터페이스 도입 | 기존 `writeToSystemProperties` 로직이 서버마다 자유 형식으로 흩어져 계약 보장 불가 | 2026-04-03 | testcontainers-design |
| 키 명명: dot-separated lowercase | kebab-case/camelCase/dot-case 혼재 해소. `testcontainers.{namespace}.{key}` 표준화 | 2026-04-03 | testcontainers-design |
| `registerSystemProperties()` AutoCloseable 반환 | 테스트 격리: close() 시 이전 값 복원, 병렬 테스트 간 시스템 프로퍼티 오염 방지 | 2026-04-03 | testcontainers-design |

## 패턴 & 사용법

### KDoc 한국어 작성 규칙

**적용 대상**: 모든 public 클래스, 인터페이스, 확장 함수

**구조 순서 (필수)**:

```kotlin
/**
 * [설명 — 한국어]
 *
 * ```kotlin
 * [인라인 예제 코드]
 * ```
 *
 * @param [파라미터 설명]
 * @return [반환값 설명]
 * @throws [예외 설명]
 */
```

**KDoc 예제 규칙**:

```kotlin
/**
 * 두 정수의 합을 반환합니다.
 *
 * ```kotlin
 * val result = add(2, 3) // 5
 * ```
 *
 * @param a 첫 번째 정수
 * @param b 두 번째 정수
 * @return 두 정수의 합
 */
fun add(a: Int, b: Int): Int = a + b
```

**준수 사항**:
- 코드 블록 언어 태그 ` ```kotlin ` 필수
- import 문 포함 금지 (동일/외부 패키지 모두 생략)
- `@sample` 태그 미사용 (인라인 예제만)
- 결과값 주석 표시: `// "result"` 또는 `// [a, b, c]`
- 기존 KDoc 텍스트 삭제/변경 금지 (예제만 추가)
- 코드 로직 변경 금지

### 통합 테스트 JVM 옵션

```
# build.gradle.kts
tasks.test {
    jvmArgs(
        "-Xshare:off",
        "-Xmx4G",
        "-XX:+UseG1GC",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+EnableDynamicAgentLoading"
    )
}
```

DuckDB처럼 native 라이브러리를 사용하는 모듈은 추가 설정 필요:

```kotlin
tasks.test {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
```

### suspend 테스트 패턴

```kotlin
class MyCoroutineTest {

    @Test
    fun `기본 suspend 테스트`() = runTest(timeout = 30.seconds) {
        val result = someService.suspendCall()
        result shouldBe expected
    }

    // @BeforeEach / @AfterEach도 runTest 내부에서 처리
    @BeforeEach
    fun setup() = runTest {
        db = createTestDatabase()
    }

    @AfterEach
    fun teardown() = runTest {
        db.close()
    }
}
```

**규칙**:
- `runTest(timeout = 30.seconds)` — 기본 타임아웃 명시
- `@BeforeEach` / `@AfterEach`도 `runTest { }` 내부로 처리
- `runBlocking` 사용 금지 (R2DBC 모듈에서는 코루틴 컨텍스트 전파 깨짐)

### Testcontainers PropertyExportingServer 계약 패턴

#### 인터페이스 정의

```kotlin
interface PropertyExportingServer : GenericServer {
    /**
     * 시스템 프로퍼티 네임스페이스. 예: "postgresql" -> testcontainers.postgresql.*
     * dot-separated lowercase 규칙: ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$
     */
    val propertyNamespace: String

    /**
     * export할 프로퍼티 키 이름 집합 (start() 전에도 호출 가능).
     * 기본 키(host, port, url)는 포함하지 않음.
     */
    fun propertyKeys(): Set<String> = emptySet()

    /**
     * export할 프로퍼티 키-값 쌍 (start() 후에만 호출 가능).
     */
    fun properties(): Map<String, String> = emptyMap()
}
```

#### 시스템 프로퍼티 등록 (복원 가능)

```kotlin
// AutoCloseable 반환 — close() 시 이전 값 복원
class MyTest {
    private lateinit var propsHandle: AutoCloseable

    @BeforeEach
    fun setup() {
        val server = PostgreSQLServer().apply { start() }
        propsHandle = server.registerSystemProperties()
    }

    @AfterEach
    fun cleanup() {
        propsHandle.close()  // 시스템 프로퍼티 이전 값으로 복원
    }
}

// try-with-resources 패턴
server.registerSystemProperties().use {
    // 테스트 코드
}
```

#### 키 명명 규칙 표준

| 서버 | namespace | 주요 키 |
|------|-----------|--------|
| PostgreSQLServer | `postgresql` | `driver.class.name`, `jdbc.url`, `username`, `password`, `database` |
| MySQL8Server | `mysql8` | (동일) |
| KafkaServer | `kafka` | `bootstrap.servers`, `bound.port.numbers` |
| RedisServer | `redis` | (기본 host/port/url만) |
| Neo4jServer | `neo4j` | `bolt.port`, `http.port` |
| InfluxDBServer | `influxdb` | `organization`, `bucket`, `admin.token`, `username` |
| KeycloakServer | `keycloak` | `auth.url`, `realm`, `admin.username`, `admin.password` |

**프로퍼티 접근 예시**:

```kotlin
System.getProperty("testcontainers.postgresql.jdbc.url")
System.getProperty("testcontainers.kafka.bootstrap.servers")
```

#### Launcher 패턴 (singleton vs fresh instance)

```kotlin
// ✅ 표준 패턴
object Launcher {
    /** JVM 전역 공유 인스턴스 — immutable, ShutdownQueue 자동 관리 */
    val shared: PostgreSQLServer by lazy {
        PostgreSQLServer().apply {
            start()
            ShutdownQueue.register(this)
        }
    }

    // 하위 호환 deprecated 유지
    @Deprecated("Use shared instead", ReplaceWith("shared"))
    val postgres: PostgreSQLServer get() = shared
}

// fresh instance는 companion object invoke() 패턴으로
val freshServer = PostgreSQLServer()   // invoke() operator
freshServer.start()
// ...
freshServer.stop()
```

### Contract Test 패턴

```kotlin
class PropertyExportingServerContractTest {

    // Docker 없이 계약 검증 (propertyKeys()는 start() 전 호출 가능)
    @TestFactory
    fun `property keys follow dot-separated lowercase`(): List<DynamicTest> {
        val keyPattern = Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")
        return serverFactories.map { (name, factory) ->
            DynamicTest.dynamicTest(name) {
                val server = factory()
                server.propertyKeys().forEach { key ->
                    key shouldMatch keyPattern
                }
            }
        }
    }

    @Test
    fun `registerSystemProperties restores previous values on close`() {
        val key = "testcontainers.redis.host"
        System.setProperty(key, "original-value")

        val server = RedisServer.Launcher.shared
        val handle = server.registerSystemProperties()

        System.getProperty(key) shouldNotBe "original-value"  // 덮어씌워짐
        handle.close()
        System.getProperty(key) shouldBe "original-value"     // 복원됨
    }
}
```

### Testcontainers 신규 서버 표준 패턴

```kotlin
class XxxServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
) : SomeContainer(imageName), GenericServer {

    companion object : KLogging() {
        const val IMAGE = "image/name"
        const val TAG = "x.y.z"
        const val NAME = "xxx"
        const val PORT = 1234

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): XxxServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return XxxServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    init {
        addExposedPorts(PORT)
        withReuse(reuse)
        if (useDefaultPort) exposeCustomPorts(PORT)
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME, mapOf(/* extra props */))
    }

    object Launcher {
        val shared: XxxServer by lazy {
            XxxServer().apply { start(); ShutdownQueue.register(this) }
        }
    }
}
```

**패키지 분류 기준**:

| 패키지 | 대상 |
|--------|------|
| `database/` | JDBC 기반 SQL DB (PostgreSQL, MySQL, Trino) |
| `storage/` | 비 JDBC 저장소 (Redis, Elasticsearch, InfluxDB) |
| `graphdb/` | 그래프 DB (Neo4j, Memgraph, PostgreSQLAge) |
| `infra/` | 인프라 서비스 (Vault, Consul, Keycloak, Toxiproxy) |
| `http/` | HTTP 서버 (Nginx, Httpbin, WireMock) |
| `mq/` | 메시지 큐 (Kafka, RabbitMQ, Pulsar, NATS) |

### Spring 헬퍼 분리 패턴

Spring 의존성이 있는 헬퍼는 별도 파일로 분리:

```
mq/
  KafkaServer.kt              -- Spring 의존성 없음
  KafkaServerSpringSupport.kt -- Spring Kafka extension functions만

storage/
  ElasticsearchServer.kt
  ElasticsearchServerSpringSupport.kt
```

```kotlin
// KafkaServerSpringSupport.kt
fun KafkaServer.Launcher.getStringProducerFactory(
    kafkaServer: KafkaServer = shared,
): ProducerFactory<String, String> { ... }
```

모듈 분리가 아닌 **파일 분리**로 충분 (이미 `compileOnly`로 선언됨).

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 채택하지 않은 이유 |
|------|-----------------|
| `@sample` KDoc 태그 | 별도 파일 참조 필요. 인라인 예제보다 유지보수 비용 높음 |
| PropertyExportingServer를 GenericServer에 직접 추가 | 40+ 서버 일괄 수정 부담. 점진적 마이그레이션 선호 |
| 어노테이션 기반 프로퍼티 선언 | 런타임 리플렉션 오버헤드, 타입 안전성 약화 |
| `System.setProperty` 영구 등록 | 병렬 테스트 격리 불가. AutoCloseable 복원 패턴으로 대체 |
| Phase 1에서 구 키 즉시 제거 | 기존 테스트 실패 위험. 양쪽 키 동시 등록 → Phase 2에서 구 키 제거 |

## 관련 페이지

- [module-decisions.md](module-decisions.md) — 모듈 구조 및 패턴
- [infrastructure-patterns.md](infrastructure-patterns.md) — Testcontainers 서버 상세
- [spring-boot-integration.md](spring-boot-integration.md) — Spring 통합 테스트 패턴
