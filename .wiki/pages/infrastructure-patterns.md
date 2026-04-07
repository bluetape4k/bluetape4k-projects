# Infrastructure Patterns

> 마지막 업데이트: 2026-04-07 | 관련 specs: 2개

## 개요

bluetape4k 인프라 레이어의 패턴. AWS 클라이언트 팩토리 패턴, Testcontainers 서버 설계, Hibernate Lettuce NearCache 구조를 다룬다.

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| aws-kotlin: `xxxClientOf` / `withXxxClient` 패턴 통일 | DSL 스타일로 일관성 확보. 리소스 정리 자동화 | 2026-03-31 | (MEMORY.md) |
| aws-kotlin: CRT 엔진 권장 | AWS CRT(C Runtime)는 성능 최적화된 HTTP 클라이언트. 네이티브 async/HTTP2 지원 | 2026-03-31 | (MEMORY.md) |
| aws-kotlin vs aws-java-sdk-v2 선택 | Kotlin native suspend vs `.await()` 래핑 차이 | 2026-03-31 | (MEMORY.md) |
| hibernate-lettuce: Caffeine L1 + Redis L2 구조 | 로컬 캐시(Caffeine)로 Redis 네트워크 비용 절감. 캐시 miss 시 Redis L2 조회 | 2026-03-28 | hibernate-cache-lettuce-migration-design |
| Testcontainers: dot-separated lowercase 키 표준 | 기존 kebab-case/camelCase/dot-case 혼재 해소 | 2026-04-03 | testcontainers-design |
| GraphDB 신규 서버: `graphdb/` 패키지 | database/storage와 분리. Neo4j, Memgraph, PostgreSQLAge 그룹화 | 2026-04-02 | testcontainers-new-servers-design |

## 패턴 & 사용법

### aws-kotlin: `xxxClientOf` / `withXxxClient` 패턴

```kotlin
// 클라이언트 생성 팩토리
val s3Client = s3ClientOf {
    region = "ap-northeast-2"
    httpClient = CrtHttpEngine()  // CRT 엔진 권장
}

// use 블록 — 자동 리소스 정리
s3ClientOf {
    region = "ap-northeast-2"
}.use { client ->
    client.listBuckets()
}

// withXxxClient DSL
withS3Client(region = "ap-northeast-2") { client ->
    client.putObject { ... }
}
```

**CRT 엔진 설정** (권장):

```kotlin
val client = dynamoDbClientOf {
    region = "ap-northeast-2"
    httpClient = CrtHttpEngine {
        maxConnections = 100
        connectionTimeout = 5.seconds
    }
}
```

### aws-kotlin vs aws-java-sdk-v2 선택 기준

| 기준 | `bluetape4k-aws` (Java SDK v2) | `bluetape4k-aws-kotlin` (Kotlin SDK) |
|------|-------------------------------|--------------------------------------|
| 실행 모델 | `CompletableFuture` + `.await()` 래핑 | native `suspend` 함수 |
| 클라이언트 타입 | `S3AsyncClient` 래핑 | `S3Client` (Kotlin native) |
| 코드 량 | 래퍼 boilerplate 있음 | 최소화 |
| 의존성 | `aws.sdk.kotlin.services.*` 불필요 | Kotlin AWS SDK 필요 |
| 선택 기준 | Java/Kotlin 혼용 프로젝트 | Kotlin 전용 신규 프로젝트 |

**추천**: 신규 프로젝트는 `bluetape4k-aws-kotlin` 사용.

### Testcontainers: PropertyExportingServer 계약

모든 새 서버 구현 시 `PropertyExportingServer` 인터페이스를 구현해야 한다.

```kotlin
class Neo4jServer private constructor(...) : Neo4jContainer(imageName), PropertyExportingServer {

    override val propertyNamespace: String = "neo4j"  // dot-separated lowercase

    override fun propertyKeys(): Set<String> = setOf(
        "bolt.port",
        "http.port"
    )

    override fun properties(): Map<String, String> = mapOf(
        "bolt.port" to getMappedPort(BOLT_PORT).toString(),
        "http.port" to getMappedPort(HTTP_PORT).toString()
    )
}
```

**전체 프로퍼티 접근 경로**: `testcontainers.{namespace}.{key}`

```kotlin
System.getProperty("testcontainers.neo4j.bolt.port")
System.getProperty("testcontainers.neo4j.http.port")
System.getProperty("testcontainers.neo4j.host")
System.getProperty("testcontainers.neo4j.port")   // bolt port (기본)
System.getProperty("testcontainers.neo4j.url")    // bolt://host:port
```

### Testcontainers 신규 서버 목록 (2026-04-02 추가)

| 서버 | 패키지 | 이미지 | namespace |
|------|--------|--------|-----------|
| `Neo4jServer` | `graphdb/` | `neo4j:5.28.0` | `neo4j` |
| `MemgraphServer` | `graphdb/` | `memgraph/memgraph:3.2.1` | `memgraph` |
| `PostgreSQLAgeServer` | `graphdb/` | `apache/age:PG17_latest` | `postgresql.age` |
| `ToxiproxyServer` | `infra/` | `ghcr.io/shopify/toxiproxy:2.9.0` | `toxiproxy` |
| `TrinoServer` | `database/` | `trinodb/trino:475` | `trino` |
| `WireMockServer` | `http/` | `wiremock/wiremock:3.13.2` | `wiremock` |
| `KeycloakServer` | `infra/` | `quay.io/keycloak/keycloak:26.2` | `keycloak` |
| `InfluxDBServer` | `storage/` | `influxdb:2.7` | `influxdb` |

#### Neo4jServer 사용 예시

```kotlin
class Neo4jTest {
    companion object {
        val neo4j = Neo4jServer.Launcher.shared
    }

    @Test
    fun `Bolt 연결 확인`() {
        GraphDatabase.driver(neo4j.boltUrl).use { driver ->
            driver.verifyConnectivity()
        }
    }

    @Test
    fun `Cypher 쿼리 실행`() {
        GraphDatabase.driver(neo4j.boltUrl).use { driver ->
            driver.session().use { session ->
                val result = session.run("RETURN 1 AS num")
                result.single()["num"].asInt() shouldBe 1
            }
        }
    }
}
```

#### ToxiproxyServer — Chaos 테스트 패턴

```kotlin
class ChaosTest {
    companion object {
        val toxiproxy = ToxiproxyServer.Launcher.shared
    }

    @Test
    fun `네트워크 지연 테스트`() {
        val proxy = toxiproxy.getProxy("redis-proxy", "redis:6379")

        // LATENCY toxic 추가
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 500)

        // 서비스 호출 — 500ms 지연 발생
        val elapsed = measureTimeMillis { callService() }
        elapsed shouldBeGreaterThan 500

        // toxic 제거 후 정상 복구
        proxy.toxics()["latency"].remove()
        val fast = measureTimeMillis { callService() }
        fast shouldBeLessThan 100
    }
}
```

#### KeycloakServer 주의사항

```kotlin
// ✅ Keycloak 17+ (Quarkus 기반): context path는 "/"
val authUrl = keycloak.authServerUrl  // "http://host:port"

// Admin REST API 경로
// ✅ 올바른 경로
GET /admin/realms
// ❌ 잘못된 경로 (Keycloak 16 이하 레거시)
GET /auth/admin/realms

// "/auth" context path는 명시적으로 설정한 경우만
keycloak.withContextPath("/auth")
```

#### InfluxDBServer (InfluxDB 2.x)

```kotlin
val influxdb = InfluxDBServer.Launcher.shared

// InfluxDB 2.x: organization/bucket/adminToken 모델
val org = influxdb.organization      // "bluetape4k"
val bucket = influxdb.bucket         // "test-bucket"
val token = influxdb.adminToken      // "test-token"

// HTTP API 접근
val url = influxdb.url               // "http://host:port"
```

### hibernate-lettuce NearCache: Caffeine L1 + Redis L2

**구조**:

```
애플리케이션
     ↓ 조회
[L1: Caffeine (메모리)]
     ↓ miss
[L2: Redis (Lettuce)]
     ↓ miss
[DB: PostgreSQL / MySQL]
```

**LettuceNearCacheRegionFactory** 동작:
1. Hibernate 2nd Level Cache 요청 → `RegionFactoryTemplate.getFromRegion()`
2. L1(Caffeine) 조회 → hit 즉시 반환
3. L1 miss → L2(Redis/Lettuce) 조회
4. L2 miss → DB 쿼리 후 L1/L2 동시 저장

**설정 예시**:

```yaml
# application.yml
bluetape4k:
  cache:
    lettuce:
      redis-uri: redis://localhost:6379
      default-ttl: 3600         # 초 단위 기본 TTL
      l1-max-size: 10000        # Caffeine 최대 엔트리 수
      regions:
        "io.example.Product":
          ttl: 1800              # region별 TTL 오버라이드
        "io.example.Order":
          ttl: 300
```

**Hibernate 설정**:

```properties
# hibernate.properties
hibernate.cache.use_second_level_cache=true
hibernate.cache.use_query_cache=true
hibernate.cache.region.factory_class=io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
```

**지원 코덱 (15가지)**:
- 직렬화: Java serialization, Kryo, Fory, Protobuf
- 압축: LZ4, Zstd, Snappy 조합

**Micrometer 메트릭**:

```kotlin
// 자동 등록되는 게이지
cache.nearcache.active_regions   // 활성 region 수
cache.nearcache.l1.size          // L1 Caffeine 엔트리 수
cache.nearcache.l2.size          // L2 Redis 엔트리 수
```

**Actuator 엔드포인트**:

```
GET /actuator/nearcache
{
  "regions": ["io.example.Product", "io.example.Order"],
  "l1Stats": { "hitCount": 1000, "missCount": 50 },
  "l2Stats": { "hitCount": 50, "missCount": 5 }
}
```

### Spring Boot 자동구성 (3/4 동시 지원)

**Spring Boot 3**:

```kotlin
// LettuceNearCacheHibernateAutoConfiguration.kt
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer

@AutoConfiguration
class LettuceNearCacheHibernateAutoConfiguration {
    @Bean
    fun hibernatePropertiesCustomizer(
        properties: LettuceNearCacheSpringProperties
    ): HibernatePropertiesCustomizer = HibernatePropertiesCustomizer { props ->
        props["hibernate.cache.region.factory_class"] =
            LettuceNearCacheRegionFactory::class.qualifiedName
        // ... 추가 설정
    }
}
```

**Spring Boot 4** (패키지 변경):

```kotlin
// Spring Boot 4: 별도 의존성 필요
// compileOnly(Libs.springBoot("hibernate"))
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
```

**AUTO_CONFIGURATION 등록**:

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheHibernateAutoConfiguration
io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheMetricsAutoConfiguration
io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheActuatorAutoConfiguration
```

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 채택하지 않은 이유 |
|------|-----------------|
| aws-java-sdk-v2를 primary로 | Kotlin 프로젝트에서 `.await()` 래핑 boilerplate. Kotlin SDK가 더 자연스러움 |
| Hazelcast/Ehcache as L1 | Caffeine이 고성능 로컬 캐시로 더 적합. JCache 표준 API 지원 |
| Redis 단독 캐시 (L1 없음) | 네트워크 I/O 비용. L1 Caffeine으로 hot data 로컬 캐싱 |
| WireMock GenericContainer 직접 | `WireMockContainer`의 편의 메서드(`withMappingFromJSON`) 포기. WireMock 공식 모듈 사용 권장 |
| InfluxDB 1.x API | 2.x organization/bucket/token 모델이 현대적. 1.x는 `database/username/password` 레거시 |

## 관련 페이지

- [kotlin-testing-patterns.md](kotlin-testing-patterns.md) — PropertyExportingServer 계약 상세
- [spring-boot-integration.md](spring-boot-integration.md) — hibernate-lettuce Spring Boot 통합
- [cache-architecture.md](cache-architecture.md) — NearCache 통일 인터페이스
