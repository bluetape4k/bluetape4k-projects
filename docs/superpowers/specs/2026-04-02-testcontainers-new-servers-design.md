# testcontainers 신규 Server 추가 설계

**날짜**: 2026-04-02  
**대상 모듈**: `bluetape4k-projects/testing/testcontainers`  
**작성자**: Claude (Architect 분석 기반)

---

## 1. 개요

### 목적

`bluetape4k-projects/testing/testcontainers` 모듈에 8개의 신규 Server 클래스를 추가한다.

### 배경

- `bluetape4k-graph/graph/graph-servers`에 Neo4j, Memgraph, PostgreSQLAge Server가 단순 패턴으로 구현되어 있음
- Graph 프로젝트 외에도 일반 프로젝트에서 Graph DB 테스트 수요 증가
- Toxiproxy(Chaos), Trino(분산 SQL), WireMock(HTTP 목), Keycloak(Auth), InfluxDB(시계열) 등 테스트 인프라 수요

### bluetape4k-graph 재사용 계획

1. `bluetape4k-projects/testing/testcontainers`에 canonical 패턴으로 Server 구현 후 배포
2. `bluetape4k-graph`의 `graph-servers` 모듈은 향후 `bluetape4k-testcontainers`를 의존하도록 교체
3. 현재 `graph-servers`의 단순 singleton 패턴 → canonical 패턴으로 대체

---

## 2. 패키지 구조

```
testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/
├── graphdb/                      # 신규 패키지 (Graph DB)
│   ├── Neo4jServer.kt
│   ├── MemgraphServer.kt
│   └── PostgreSQLAgeServer.kt
├── infra/
│   ├── ToxiproxyServer.kt        # 신규
│   └── KeycloakServer.kt         # 신규
├── database/
│   └── TrinoServer.kt            # 신규 (JdbcDatabaseContainer 상속 → JdbcServer 구현 가능)
├── storage/
│   └── InfluxDBServer.kt         # 신규 (JDBC 아닌 HTTP API DB → storage/ 패키지)
└── http/
    └── WireMockServer.kt         # 신규
```

> **패키지 분류 기준**:
> - `database/` — JDBC 기반 SQL DB (PostgreSQL, MySQL, Trino 등)
> - `storage/` — 비 JDBC 저장소 (Redis, Hazelcast, Elasticsearch, InfluxDB 등)
> - `graphdb/` — 그래프 DB (Neo4j, Memgraph, PostgreSQLAge)
> - `infra/` — 인프라 서비스 (Vault, Consul, Keycloak, Toxiproxy 등)
> - `http/` — HTTP 서버 (Nginx, Httpbin, WireMock 등)

---

## 3. 기존 Server 표준 패턴

모든 신규 서버는 다음 패턴을 엄격히 따른다.

```kotlin
class XxxServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): SomeContainer(imageName), GenericServer {

    companion object: KLogging() {
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

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): XxxServer = XxxServer(imageName, useDefaultPort, reuse)
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
        writeToSystemProperties(NAME, mapOf(...))
    }

    object Launcher {
        val xxx: XxxServer by lazy {
            XxxServer().apply { start(); ShutdownQueue.register(this) }
        }
    }
}
```

---

## 4. 각 서버 상세 설계

### 4.1 Neo4jServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.graphdb` |
| 베이스 클래스 | `Neo4jContainer` (TC 공식: `org.testcontainers:neo4j`) |
| Docker 이미지 | `neo4j:5.28.0` |
| 포트 | `7474` (HTTP), `7687` (Bolt) |

**특수 설정**:
- 기본값: `withoutAuthentication()` (테스트 편의)
- 인증 필요 시: `withAdminPassword(password)` 옵션 파라미터 제공
- `override val port` → `getMappedPort(7687)` (Bolt 기준)
- `override val url` → `"bolt://$host:$port"`

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "bolt.port" to getMappedPort(BOLT_PORT),
    "http.port" to getMappedPort(HTTP_PORT),
    "bolt.url" to boltUrl,
    "http.url" to httpUrl,
)
```

**Launcher 헬퍼**:
```kotlin
object Launcher {
    val neo4j: Neo4jServer by lazy { Neo4jServer().apply { start(); ShutdownQueue.register(this) } }
}
```

**추가 의존성 필요**:
- `compileOnly(Libs.testcontainers_neo4j)`
- `compileOnly(Libs.neo4j_java_driver)` — `org.neo4j.driver:neo4j-java-driver:5.28.4`

---

### 4.2 MemgraphServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.graphdb` |
| 베이스 클래스 | `GenericContainer<MemgraphServer>` |
| Docker 이미지 | `memgraph/memgraph:3.2.1` |
| 포트 | `7687` (Bolt), `7444` (Log/Monitoring) |

**특수 설정**:
- `addEnv("MEMGRAPH", "--telemetry-enabled=false")` — 텔레메트리 비활성화
- `override val port` → `getMappedPort(7687)` (Bolt 기준)
- `override val url` → `"bolt://$host:$port"`

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "bolt.port" to getMappedPort(BOLT_PORT),
    "log.port" to getMappedPort(LOG_PORT),
    "bolt.url" to "bolt://$host:${getMappedPort(BOLT_PORT)}",
)
```

**추가 의존성 필요**:
- `compileOnly(Libs.neo4j_java_driver)` — Bolt 드라이버 공유 (Neo4j 호환)

---

### 4.3 PostgreSQLAgeServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.graphdb` |
| 베이스 클래스 | `PostgreSQLContainer<PostgreSQLAgeServer>`, `JdbcServer` 구현 |
| Docker 이미지 | `apache/age:PG17_latest` |
| 포트 | `5432` (PostgreSQL) |

**특수 설정**:
- `asCompatibleSubstituteFor("postgres")` 설정
- `start()` 내 AGE extension 초기화:
  ```kotlin
  override fun start() {
      super.start()
      createConnection("").use { conn ->
          conn.createStatement().execute("CREATE EXTENSION IF NOT EXISTS age")
          conn.createStatement().execute("LOAD 'age'")
          // Kotlin 이스케이프 주의: SQL의 "$user"는 PostgreSQL 예약어 (현재 DB 사용자)
          // Kotlin 문자열에서 $ 이스케이프: "\$user" 또는 raw string """ SET search_path = ag_catalog, "${'$'}user", public """
          conn.createStatement().execute("""SET search_path = ag_catalog, "${'$'}user", public""")
      }
      writeToSystemProperties(NAME, ...)
  }
  ```
  > `ag_catalog`를 search_path에 추가하는 이유: AGE의 Cypher 함수(`cypher()`, `ag_graph` 테이블 등)가 `ag_catalog` 스키마에 있으므로 명시적 스키마 지정 없이 접근하려면 search_path 설정이 필요.
- DB 기본값: `database = "test"`, `username = "test"`, `password = "test"`

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "jdbc.url" to jdbcUrl,
    "username" to username,
    "password" to password,
    "database" to databaseName,
)
```

**추가 의존성**:
- `compileOnly(Libs.testcontainers_postgresql)` (이미 있음)
- `testRuntimeOnly(Libs.postgresql_driver)` (이미 있음)

---

### 4.4 ToxiproxyServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.infra` |
| 베이스 클래스 | `ToxiproxyContainer` (TC 공식: `org.testcontainers:toxiproxy`) |
| Docker 이미지 | `ghcr.io/shopify/toxiproxy:2.9.0` |
| 포트 | `8474` (Control API) |

**특수 설정**:
- `ToxiproxyContainer`는 control port(8474) 외 proxied port를 동적 추가하는 구조
- `Launcher`에 `createProxy(name, upstream)` 헬퍼 제공:

```kotlin
object Launcher {
    val toxiproxy: ToxiproxyServer by lazy { ... }
    
    fun createProxy(name: String, upstream: String): ToxiproxyContainer.ContainerProxy =
        toxiproxy.getProxy(name, upstream)
}
```

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "control.port" to getMappedPort(CONTROL_PORT),
    "control.url" to "http://$host:${getMappedPort(CONTROL_PORT)}",
)
```

**추가 의존성 필요**:
- `compileOnly(Libs.testcontainers_toxiproxy)` — `testcontainersModule("toxiproxy")`

---

### 4.5 TrinoServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.database` |
| 베이스 클래스 | `TrinoContainer` (TC 공식: `org.testcontainers:trino`), `JdbcServer` 구현 |
| Docker 이미지 | `trinodb/trino:475` |
| 포트 | `8080` (HTTP/JDBC) |

> **확인**: TC의 `TrinoContainer`는 `JdbcDatabaseContainer`를 상속하므로 `JdbcServer` 인터페이스 구현 가능.

**특수 설정**:
- `override val url` → `"http://$host:$port"`
- JDBC URL: `"jdbc:trino://$host:$port/memory"` (기본 메모리 카탈로그)
- 사용자: `test` (기본값)

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "jdbc.url" to "jdbc:trino://$host:$port/memory",
    "username" to username,
)
```

**추가 의존성 필요**:
- `compileOnly(Libs.testcontainers_trino)` — `testcontainersModule("trino")`
- `testRuntimeOnly(Libs.trino_jdbc)` — `io.trino:trino-jdbc:475`

---

### 4.6 WireMockServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.http` |
| 베이스 클래스 | `WireMockContainer` (`org.wiremock:wiremock-testcontainers-module`) |
| Docker 이미지 | `wiremock/wiremock:3.13.2` |
| 포트 | `8080` (HTTP), `8443` (HTTPS) |

**특수 설정**:
- `withMappingFromJSON(...)` / `withMappingFromResource(...)` 헬퍼 제공 가능
- `override val url` → `"http://$host:$port"`
- `httpsUrl` 추가 프로퍼티 제공

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "http.port" to getMappedPort(HTTP_PORT),
    "https.port" to getMappedPort(HTTPS_PORT),
    "base.url" to baseUrl,
)
```

**추가 의존성 필요**:
- `compileOnly(Libs.wiremock_testcontainers)` — `org.wiremock:wiremock-testcontainers-module:1.0-alpha-15`
- `compileOnly(Libs.wiremock)` — (이미 있음)

> **주의**: `wiremock-testcontainers-module` 검증 버전: `1.0-alpha-15` (2026-03-30 배포). alpha 버전이 부담스러울 경우 대안: `GenericContainer("wiremock/wiremock:3.x")` 직접 사용 + WireMock Admin API HTTP 호출 방식. 단, `WireMockContainer`의 편의 메서드(`withMappingFromJSON` 등)를 포기해야 함. → **alpha 모듈 사용을 권장** (WireMock 공식 유지보수).

---

### 4.7 KeycloakServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.infra` |
| 베이스 클래스 | `KeycloakContainer` (`com.github.dasniko:testcontainers-keycloak:3.7.0`) |
| Docker 이미지 | `quay.io/keycloak/keycloak:26.2` |
| 포트 | `8080` (HTTP) |

**특수 설정**:
- Admin user: `admin` / `admin` (기본값)
- `withRealmImportFile(...)` 옵션 지원
- `withFeaturesEnabled(...)` — 필요한 Keycloak feature flag 활성화
- `authServerUrl` 프로퍼티 제공

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "auth.url" to authServerUrl,
    "admin.username" to adminUsername,
    "admin.password" to adminPassword,
)
```

> **Context Path 주의**: dasniko `KeycloakContainer`의 기본 context path는 `/` (Keycloak 17+ Quarkus 기반). `authServerUrl`은 `http://$host:$port` 형태. Admin API 경로는 `/admin/realms` (not `/auth/admin/realms`). `/auth` context path는 `.withContextPath("/auth")`를 명시했을 때만 사용.

**Launcher 헬퍼**:
```kotlin
object Launcher {
    val keycloak: KeycloakServer by lazy { ... }
}
```

**추가 의존성 필요**:
- `compileOnly(Libs.keycloak_testcontainers)` — `com.github.dasniko:testcontainers-keycloak:3.7.0`

---

### 4.8 InfluxDBServer

| 항목 | 값 |
|------|-----|
| 패키지 | `io.bluetape4k.testcontainers.storage` |
| 베이스 클래스 | `InfluxDBContainer` (TC 공식: `org.testcontainers:influxdb`) |
| Docker 이미지 | `influxdb:2.7` |
| 포트 | `8086` (HTTP API) |

**특수 설정** (InfluxDB 2.x 기준):
- InfluxDB 2.x는 `organization / bucket / adminToken` 모델 사용 (`database/username/password` 는 1.x 모델)
- 기본 설정:
  - `organization = "bluetape4k"`
  - `bucket = "test-bucket"`
  - `adminToken = "test-token"`
  - `username = "admin"`, `password = "password"` (초기 setup용)
- `InfluxDBContainer.withAdminToken(...)` / `withOrganization(...)` / `withBucket(...)` 사용
- `override val url` → `"http://$host:$port"`

> **클라이언트 라이브러리**: `org.influxdb:influxdb-java` (`Libs.influxdb_java`)는 1.x API. 2.x API는 `com.influxdb:influxdb-client-java`가 별도. 테스트에서는 HTTP API 직접 호출 또는 `influxdb-client-java` 의존성 추가 중 선택. 현재는 `influxdb-java`(1.x) 만 있으므로 TC의 `InfluxDBContainer` 자체 헬퍼(`getAdminToken()`, `getBucket()` 등)를 활용.

**writeToSystemProperties extraProps**:
```kotlin
mapOf(
    "organization" to organization,
    "bucket" to bucket,
    "admin.token" to adminToken,
    "username" to username,
)
```

**추가 의존성**:
- `compileOnly(Libs.testcontainers_influxdb)` — `Libs.kt:1373`에 이미 존재, `build.gradle.kts`에만 추가
- `compileOnly(Libs.influxdb_java)` — `Libs.kt:1033`에 이미 존재, `build.gradle.kts`에만 추가

---

## 5. buildSrc/Libs.kt 추가 필요 상수

> **중요**: 아래 상수들은 현재 `Libs.kt`에 존재하지 않는다. 구현 단계 1에서 반드시 먼저 추가해야 한다.
> `testcontainers_influxdb`(`:1373`)와 `influxdb_java`(`:1033`)는 이미 있으므로 추가 불필요.

```kotlin
// Graph DB
val testcontainers_neo4j = testcontainersModule("neo4j")
val neo4j_java_driver = "org.neo4j.driver:neo4j-java-driver:5.28.4"

// Distributed SQL
val testcontainers_trino = testcontainersModule("trino")
val trino_jdbc = "io.trino:trino-jdbc:475"

// Chaos Testing
val testcontainers_toxiproxy = testcontainersModule("toxiproxy")

// WireMock Testcontainers (third-party, WireMock 공식 유지보수)
// 버전은 "최신"이 아닌 "검증한 버전"으로 고정. 현재 검증 버전: 1.0-alpha-15 (2026-03-30 배포)
val wiremock_testcontainers = "org.wiremock:wiremock-testcontainers-module:1.0-alpha-15"

// Keycloak Testcontainers (dasniko)
val keycloak_testcontainers = "com.github.dasniko:testcontainers-keycloak:3.7.0"
```

---

## 6. build.gradle.kts 변경사항

`testing/testcontainers/build.gradle.kts`에 추가:

```kotlin
// Graph DB
compileOnly(Libs.testcontainers_neo4j)
compileOnly(Libs.neo4j_java_driver)

// Toxiproxy
compileOnly(Libs.testcontainers_toxiproxy)

// Trino
compileOnly(Libs.testcontainers_trino)
testRuntimeOnly(Libs.trino_jdbc)

// WireMock (testcontainers module)
compileOnly(Libs.wiremock_testcontainers)
// wiremock 이미 있음

// Keycloak
compileOnly(Libs.keycloak_testcontainers)

// InfluxDB (Libs.kt에 이미 있으나 build.gradle.kts에 미반영)
compileOnly(Libs.testcontainers_influxdb)
compileOnly(Libs.influxdb_java)
```

---

## 7. 기존 서버 리팩토링 범위

완료 후 전체 서버의 일관성 검토:

### 우선순위 높음
1. **KDoc 누락** — 일부 Server에 클래스/메서드 KDoc(한국어) 없음
2. **`ZookeeperServerSupport.kt`** — 파일명 오탈자(Zookeeper → ZooKeeper) 및 패턴 일관성 확인
3. **InfluxDB / testcontainers_influxdb** — Libs.kt에 있으나 build.gradle.kts 미반영

### 우선순위 중간
4. **`writeToSystemProperties` extraProps 일관성** — 서버마다 키 이름 규칙 통일 (`bolt.port` vs `boltPort` 혼재 여부 확인)
5. **Launcher 헬퍼 메서드** — KafkaServer.Launcher에 풍부한 헬퍼가 있으나 다른 서버들은 단순 lazy val만 있음 → 일관성 조정

### 우선순위 낮음
6. **TAG 버전 최신화** — 오래된 이미지 TAG가 있는지 점검
7. **`testcontaiiners_nginx` 오탈자** — Libs.kt:1378에 double 'i' (`testcontaiiners_nginx`) 수정

---

## 8. 구현 순서

| 단계 | 작업 | 복잡도 |
|------|------|--------|
| 1 | `buildSrc/Libs.kt` 상수 추가 | low |
| 2 | `build.gradle.kts` 의존성 추가 | low |
| 3 | `Neo4jServer` 구현 + 테스트 | medium |
| 4 | `MemgraphServer` 구현 + 테스트 | medium |
| 5 | `PostgreSQLAgeServer` 구현 + 테스트 | high |
| 6 | `ToxiproxyServer` 구현 + 테스트 | medium |
| 7 | `TrinoServer` 구현 + 테스트 | low |
| 8 | `WireMockServer` 구현 + 테스트 | medium |
| 9 | `KeycloakServer` 구현 + 테스트 | medium |
| 10 | `InfluxDBServer` 구현 + 테스트 | low |
| 11 | 전체 리팩토링 (일관성 + KDoc) | medium |
| 12 | 빌드 검증 + README 업데이트 | low |

---

## 9. 테스트 전략

### 공통 검증 (모든 서버)

> **패턴 구분**:
> - **강제 표준**: `Launcher.xxx` singleton 사용, `isRunning`, 포트/시스템 프로퍼티 검증
> - **권장 패턴**: `@TestInstance(PER_CLASS)` (기존 코드에 없는 경우도 있음 — `HttpbinServerTest` 등은 직접 start/stop 방식 사용)

- `XxxServerTest.kt` — `Launcher.xxx` 사용 (**강제**)
- `server.isRunning shouldBe true`
- `server.port shouldBeGreaterThan 0`
- `System.getProperty("testcontainers.xxx.host") shouldNotBeNull ...`

### 서버별 고유 검증 시나리오

| 서버 | 고유 검증 |
|------|---------|
| **Neo4jServer** | `Driver.verifyConnectivity()` Bolt 연결 성공; `session.run("RETURN 1")` 쿼리 결과 확인 |
| **MemgraphServer** | Bolt 연결 성공; `CALL mg.procedures() YIELD *` 프로시저 조회 |
| **PostgreSQLAgeServer** | `SELECT * FROM ag_catalog.ag_graph` 쿼리 성공; AGE 그래프 생성/조회 |
| **ToxiproxyServer** | upstream proxy 생성; `LATENCY` toxic 추가 후 응답 지연 확인; toxic 제거 후 정상 복구 |
| **TrinoServer** | `SELECT 1` JDBC 쿼리 성공; `information_schema.tables` 조회 |
| **WireMockServer** | stub 등록 후 HTTP GET 요청 → 기대 응답 반환 확인 |
| **KeycloakServer** | Admin REST API `GET /admin/realms` 200 응답 (기본 context path는 `/`, not `/auth`); master realm 존재 확인 |
| **InfluxDBServer** | `InfluxDBContainer.getAdminToken()` / `getBucket()` 헬퍼로 연결 확인; TC HTTP API로 포인트 write 후 query 조회 |

---

## 10. README 업데이트 계획

`testing/testcontainers/README.md`에 다음 섹션 추가/갱신:

1. **Graph DB 서버** 섹션 신설 — Neo4j, Memgraph, PostgreSQLAge 사용 예시
2. **HTTP Mock** 섹션 — WireMock 스텁 등록 예시
3. **Auth 서버** 섹션 — Keycloak Launcher 사용 + Spring Security 연동 예시
4. **시계열 DB** 섹션 — InfluxDB write/query 예시
5. **카오스 테스트** 섹션 — Toxiproxy + Resilience4j 연동 패턴
6. **분산 SQL** 섹션 — Trino JDBC 사용 예시
7. **전체 서버 목록 테이블** 갱신

---

## 11. 미결 사항

1. **PostgreSQLAge 이미지 선택**: `apache/age:PG17_latest` vs `bitnami/postgresql:17`에 AGE 수동 설치 — 공식 AGE 이미지 사용 권장
2. **Keycloak realm import**: `withRealmImportFile` 지원 여부 — KeycloakServer 생성자 파라미터로 받을지 여부 결정 필요 (기본은 파라미터 없이 시작, 사용자가 별도 설정)
