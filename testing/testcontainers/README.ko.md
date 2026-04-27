# Module bluetape4k-testcontainers

[English](./README.md) | 한국어

Testcontainers `2.0.3` 기반 통합 테스트를 빠르게 구성하기 위한 서버 래퍼/유틸 라이브러리입니다.

## 아키텍처

### 컨테이너 생명주기 다이어그램

```mermaid
sequenceDiagram
        participant TEST as 테스트 클래스
        participant SERVER as GenericServer (래퍼)
        participant TC as Testcontainers
        participant DOCKER as Docker 컨테이너
        participant SPRING as Spring Boot

    TEST->>SERVER: start()
    SERVER->>TC: 컨테이너 시작 요청
    TC->>DOCKER: Docker 이미지 pull & run
    DOCKER-->>TC: 컨테이너 준비 완료
    TC-->>SERVER: 포트 매핑 정보 반환
    SERVER->>SERVER: writeToSystemProperties()<br/>testcontainers.{name}.host/port/url 등록
    SERVER-->>TEST: 시작 완료

    TEST->>SPRING: 테스트 실행 시작
    SPRING->>SPRING: application-test.yml 로드<br/>${testcontainers.mysql.jdbc-url} 치환
    SPRING-->>TEST: ApplicationContext 준비 완료

    TEST->>TEST: 테스트 로직 실행

    TEST->>SERVER: stop() (또는 @AfterAll)
    SERVER->>DOCKER: 컨테이너 종료 & 제거
```

### 지원 컨테이너 클래스 다이어그램

```mermaid
classDiagram
    class GenericServer {
        <<abstract>>
        +useDefaultPort: Boolean
        +start()
        +stop()
        +writeToSystemProperties(name)
        +exposeCustomPorts(vararg ports)
    }
    class PostgreSQLServer {
        +withExtensions(vararg names) PostgreSQLServer
    }
    class PostgisServer {
        +withExtensions(vararg names) PostgisServer
    }
    class PgvectorServer {
        +withExtensions(vararg names) PgvectorServer
    }
    class MySQL8Server {
        +getDataSource() DataSource
    }
    class RedisServer {
        +host: String
        +port: Int
    }
    class KafkaServer {
        +bootstrapServers: String
    }
    class AwsEmulatorServer {
        <<interface>>
        +awsEndpoint: URI
        +awsAccessKey: String
        +awsSecretKey: String
        +regionName: String
        +withServices(vararg services) AwsEmulatorServer
    }
    class LocalStackServer {
        +awsEndpoint: URI
        @Deprecated
    }
    class FlociServer {
        +awsEndpoint: URI
        +withServices() FlociServer (no-op)
        @Deprecated
    }
    class MiniStackServer {
        +awsEndpoint: URI
        +withServices() MiniStackServer (no-op)
    }
    class ElasticMqServer {
        +sqsEndpoint: URI
        +host: String
        +port: Int
    }
    class MailpitServer {
        +smtpPort: Int
        +uiPort: Int
        +uiUrl: String
    }
    class BluetapeHttpServer {
        +url: String
        +httpbinUrl: String
        +jsonplaceholderUrl: String
        +webUrl: String
    }
    class BluetapeWebfluxServer {
        +url: String
        +httpbinUrl: String
        +jsonplaceholderUrl: String
        +webUrl: String
    }

    GenericServer <|-- PostgreSQLServer
    GenericServer <|-- PostgisServer
    GenericServer <|-- PgvectorServer
    GenericServer <|-- MySQL8Server
    GenericServer <|-- RedisServer
    GenericServer <|-- KafkaServer
    GenericServer <|-- LocalStackServer
    GenericServer <|-- FlociServer
    GenericServer <|-- MiniStackServer
    GenericServer <|-- MailpitServer
    GenericServer <|-- BluetapeHttpServer
    GenericServer <|-- BluetapeWebfluxServer
    PostgreSQLServer <|-- PostgisServer
    PostgreSQLServer <|-- PgvectorServer
    AwsEmulatorServer <|.. LocalStackServer
    AwsEmulatorServer <|.. FlociServer
    AwsEmulatorServer <|.. MiniStackServer

    style GenericServer fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style AwsEmulatorServer fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style PostgreSQLServer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style PostgisServer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style PgvectorServer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style MySQL8Server fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style RedisServer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style KafkaServer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalStackServer fill:#FFF8E1,stroke:#FFCC80,color:#E65100
    style FlociServer fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style ElasticMqServer fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style MailpitServer fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style BluetapeHttpServer fill:#FFF9C4,stroke:#F9A825,color:#F57F17
    style BluetapeWebfluxServer fill:#FFF9C4,stroke:#F9A825,color:#F57F17
```

### 지원 컨테이너 구조

```mermaid
flowchart TD
    GS["GenericServer\n(공통 기반)"]

    subgraph 데이터베이스
        MY5["MySQL5Server"]
        MY8["MySQL8Server"]
        MA["MariaDBServer"]
        PG["PostgreSQLServer\n(withExtensions 지원)"]
        PGS["PostgisServer\n(postgis 자동 활성화)"]
        PGV["PgvectorServer\n(vector 자동 활성화)"]
        CR["CockroachServer"]
        CH["ClickHouseServer"]
    end

    subgraph 스토리지
        RD["RedisServer"]
        RDC["RedisClusterServer"]
        MGO["MongoDBServer"]
        CS["CassandraServer"]
        ES["ElasticsearchServer"]
        ESO["ElasticsearchOssServer\n(7.x OSS)"]
        OS["OpenSearchServer"]
        MN["MinIOServer"]
        IFL["InfluxDBServer"]
        HZ["HazelcastServer"]
        IG2["Ignite2Server"]
        IG3["Ignite3Server"]
    end

    subgraph 그래프DB
        NJ["Neo4jServer"]
        MG["MemgraphServer"]
        FK["FalkorDBServer\n(Redis 프로토콜)"]
        PA["PostgreSQLAgeServer"]
    end

    subgraph 메시지큐
        KF["KafkaServer"]
        RB["RabbitMQServer"]
        PL["PulsarServer"]
        NT["NatsServer"]
        RP["RedpandaServer"]
    end

    subgraph 인프라
        CN["ConsulServer"]
        VT["VaultServer"]
        PR["PrometheusServer"]
        ZK["ZooKeeperServer"]
        TX["ToxiproxyServer"]
        KC["KeycloakServer"]
        ZP["ZipkinServer"]
    end

    subgraph 분산쿼리
        TR["TrinoServer"]
    end

    subgraph HTTPMock
        WM["WireMockServer"]
        NG["NginxServer"]
        BHS["BluetapeHttpServer\n(httpbin+jsonplaceholder+web)"]
        BWS["BluetapeWebfluxServer\n(WebFlux+Coroutines)"]
    end

    subgraph LLM
        CDB["ChromaDBServer\n(벡터 DB, 포트 8000)"]
        OL["OllamaServer\n(로컬 LLM, 포트 11434)"]
    end

    subgraph AWS
        LS["LocalStackServer\n(deprecated)"]
        FC["FlociServer\n(S3, DynamoDB, SQS 등)"]
        MS["MiniStackServer\n(31+ 서비스, 권장)"]
        EMQ["ElasticMqServer\n(임베디드 SQS, JVM)"]
    end

    subgraph 메일
        MP["MailpitServer\n(SMTP + Web UI)"]
    end

    GS --> 데이터베이스
    GS --> 스토리지
    GS --> 그래프DB
    GS --> 메시지큐
    GS --> 인프라
    GS --> 분산쿼리
    GS --> HTTPMock
    GS --> AWS
    GS --> 메일
    GS --> LLM

    classDef baseStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0,font-weight:bold
    classDef dbStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef storageStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef graphStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef mqStyle fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    classDef infraStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef sqlStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef mockStyle fill:#F57F17,stroke:#E65100,color:#000000
    classDef awsStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef deprecatedStyle fill:#FFF8E1,stroke:#FFCC80,color:#E65100
    classDef mailStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef llmStyle fill:#E8EAF6,stroke:#9FA8DA,color:#283593

    class GS baseStyle
    class MY5,MY8,MA,PG,PGS,PGV,CR,CH dbStyle
    class RD,RDC,MGO,CS,ES,ESO,OS,MN,IFL,HZ,IG2,IG3 storageStyle
    class NJ,MG,FK,PA graphStyle
    class KF,RB,PL,NT,RP mqStyle
    class CN,VT,PR,ZK,TX,KC,ZP infraStyle
    class TR sqlStyle
    class WM,NG,BHS,BWS mockStyle
    class LS deprecatedStyle
    class FC deprecatedStyle
    class MS,EMQ awsStyle
    class MP mailStyle
    class CDB,OL llmStyle
```

## 주요 기능

- **DB 서버 지원**: MySQL, MariaDB, PostgreSQL, PostGIS, pgvector, Cockroach, ClickHouse
- **Graph DB 서버 지원**: Neo4j, Memgraph, FalkorDB, PostgreSQL + Apache AGE
- **Storage 서버 지원**: Redis/Redis Cluster, MongoDB, Cassandra, Elasticsearch/OSS/OpenSearch, MinIO, InfluxDB
- **분산 캐시/그리드**: `HazelcastServer` (5.x slim), `Ignite2Server`, `Ignite3Server` (클러스터 자동 초기화)
- **MQ 서버 지원**: Kafka, RabbitMQ, Pulsar, Nats, Redpanda
- **Infra 서버 지원**: Consul, Vault, Prometheus, Jaeger, Zipkin, ZooKeeper, Toxiproxy, Keycloak
- **분산 SQL 엔진**: Trino
- **HTTP Mock 지원**: WireMock, NginxServer
- **LLM 지원**: `ChromaDBServer` (벡터 DB, 포트 8000), `OllamaServer` (로컬 LLM 추론, 포트 11434)
- **AWS 에뮬레이터**: `AwsEmulatorServer` 공통 인터페이스; `FlociServer`(GraalVM Native, 권장), `LocalStackServer`(@Deprecated)
- **임베디드 SQS**: `ElasticMqServer` — Docker 없이 JVM 내 SQS 서버 실행
- **메일 테스트**: `MailpitServer` — SMTP + Web UI로 이메일 통합 테스트 지원
- **관측성**: `ZipkinServer` — 분산 추적 (`openzipkin/zipkin-slim:2.23`)
- **고정 포트 매핑 옵션**: `useDefaultPort=true` 설정 시 기본 포트로 바인딩
- **시스템 프로퍼티 자동 등록**: 컨테이너 시작 시 연결 정보 자동 등록
- **Spring Boot 설정 단순화**: `${testcontainers...}` placeholder로 연결 정보 주입
- **PostgreSQL 확장 자동 활성화**: `PostgisServer`(postgis), `PgvectorServer`(vector)
- **`withExtensions()` API**: 추가 PostgreSQL 확장을 선언적으로 활성화

## 시스템 프로퍼티 Export (PropertyExportingServer)

모든 서버 클래스는 `PropertyExportingServer` 인터페이스를 구현하여, `start()` 시 연결 정보를 시스템 프로퍼티로 자동 등록합니다.

### 키 명명 규칙

모든 프로퍼티 키는 **kebab-case 소문자**를 사용합니다.

시스템 프로퍼티 형식: `testcontainers.{namespace}.{kebab-case-key}`

### 서버별 export 키

| 서버                  | namespace       | 주요 키                                                                                |
|---------------------|-----------------|-------------------------------------------------------------------------------------|
| PostgreSQLServer    | `postgresql`    | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| PostgisServer       | `postgis`       | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| PgvectorServer      | `pgvector`      | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| MySQL8Server        | `mysql`         | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| MariaDBServer       | `mariadb`       | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| CockroachServer     | `cockroach`     | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| ClickHouseServer    | `clickhouse`    | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| TrinoServer         | `trino`         | `jdbc-url`, `username`                                                              |
| RedisServer         | `redis`         | `host`, `port`, `url`                                                               |
| MongoDBServer       | `mongo`         | `host`, `port`, `url`                                                               |
| ElasticsearchServer | `elasticsearch` | `host`, `port`, `url`                                                               |
| KafkaServer         | `kafka`         | `host`, `port`, `url`, `bootstrap-servers`, `bound-port-numbers`                    |
| RedpandaServer      | `redpanda`      | `host`, `port`, `url`, `admin-port`, `schema-registry-port`, `rest-proxy-port`      |
| NatsServer          | `nats`          | `host`, `port`, `url`, `cluster-port`, `monitor-port`                               |
| PulsarServer        | `pulsar`        | `host`, `port`, `url`, `broker-url`, `broker-port`, `broker-http-port`              |
| RabbitMQServer      | `rabbitmq`      | `host`, `port`, `url`, `amqp-url`, `amqp-port`, `amqps-port`, `management-url`      |
| LocalStackServer    | `localstack`    | `host`, `port`, `url`, `awsEndpoint`, `awsAccessKey`, `awsSecretKey`, `regionName` |
| FlociServer         | `floci`         | `host`, `port`, `url`, `awsEndpoint`, `awsAccessKey`, `awsSecretKey`, `regionName` |
| MiniStackServer     | `ministack`     | `host`, `port`, `url`, `awsEndpoint`, `awsAccessKey`, `awsSecretKey`, `regionName` |
| ElasticMqServer     | `elasticmq`     | `host`, `port`, `url`, `sqsEndpoint`                                                |
| MailpitServer       | `mailpit`       | `host`, `port`, `url`, `smtpPort`, `uiPort`, `uiUrl`                               |
| PrometheusServer    | `prometheus`    | `host`, `port`, `url`, `server-port`, `pushgateway-port`, `graphite-exporter-port`  |
| ConsulServer        | `consul`        | `host`, `port`, `url`, `dns-port`, `http-port`, `rpc-port`                          |
| JaegerServer          | `jaeger`           | `host`, `port`, `url`, `frontend-port`, `zipkin-port`, `config-port`, `thrift-port` |
| ElasticsearchOssServer| `elasticsearch-oss`| `host`, `port`, `url`                                                               |
| HazelcastServer       | `hazelcast`        | `host`, `port`, `url`                                                               |
| Ignite2Server         | `ignite2`          | `host`, `port`, `url`                                                               |
| Ignite3Server         | `ignite3`          | `host`, `port`, `url`, `rest-port`                                                  |
| ZipkinServer          | `zipkin`           | `host`, `port`, `url`                                                               |
| NginxServer           | `nginx`            | `host`, `port`, `url`                                                               |
| ChromaDBServer        | `chromadb`         | `host`, `port`, `url`                                                               |
| OllamaServer          | `ollama`           | `host`, `port`, `url`                                                               |
| BluetapeHttpServer    | `bluetape-http`    | `host`, `port`, `url`, `httpbinUrl`, `jsonplaceholderUrl`, `webUrl`, `https-port`, `https-url`, `https-httpbin-url`, `https-jsonplaceholder-url`, `https-web-url` |
| BluetapeWebfluxServer | `bluetape-webflux` | `host`, `port`, `url`, `httpbin-url`, `jsonplaceholder-url`, `web-url`, `https-port`, `https-url`, `https-httpbin-url`, `https-jsonplaceholder-url`, `https-web-url` |

## 사용 예

### 데이터베이스

```kotlin
val mysql = MySQL8Server(useDefaultPort = true).apply { start() }
val ds = mysql.getDataSource()
```

### PostgreSQL 확장 서버

```kotlin
// PostGIS — postgis 확장 자동 활성화
val server = PostgisServer.Launcher.postgis

// pgvector — vector 확장 자동 활성화
val server = PgvectorServer.Launcher.pgvector

// 추가 확장 withExtensions()으로 선언
PostgisServer()
    .withExtensions("postgis_topology")
    .apply { start() }

PostgreSQLServer()
    .withExtensions("uuid-ossp", "hstore", "pg_trgm")
    .apply { start() }

// 확장 포함 싱글턴 직접 생성
val server = PostgreSQLServer.Launcher.withExtensions("uuid-ossp", "hstore")
```

### Graph DB 서버

| 서버 클래스             | Docker 이미지            | 기본 태그               | 프로토콜   | 기본 포트    |
|------------------------|--------------------------|------------------------|-----------|-------------|
| `Neo4jServer`          | `neo4j`                  | `5.26.24`              | Bolt/HTTP | 7687 / 7474 |
| `MemgraphServer`       | `memgraph/memgraph`      | `3.9.0`                | Bolt      | 7687        |
| `FalkorDBServer`       | `falkordb/falkordb`      | `v4.18.1`              | Redis     | 6379        |
| `PostgreSQLAgeServer`  | `apache/age`             | `release_PG17_1.6.0`  | JDBC      | 5432        |

```kotlin
// Neo4j 서버
val neo4j = Neo4jServer.Launcher.neo4j
val driver = GraphDatabase.driver(neo4j.boltUrl, AuthTokens.basic(neo4j.username, neo4j.password))

// Memgraph 서버 (Neo4j Bolt 호환)
val memgraph = MemgraphServer.Launcher.memgraph
val driver = GraphDatabase.driver(memgraph.boltUrl, AuthTokens.none())

// PostgreSQL with Apache AGE
val age = PostgreSQLAgeServer.Launcher.postgresqlAge
val conn = DriverManager.getConnection(age.jdbcUrl, age.username, age.password)
```

### HTTP Mock 서버

```kotlin
val wireMock = WireMockServer.Launcher.wireMock

// 스텁 정의
wireMock.stubFor(
    get("/hello")
        .willReturn(ok("Hello!"))
)

// 검증
verify(getRequestedFor(urlEqualTo("/hello")))
```

### BluetapeHttpServer (httpbin + jsonplaceholder + web)

`BluetapeHttpServer`는 `bluetape4k/mock-web-server` Docker 이미지를 실행합니다.
httpbin, jsonplaceholder, web 컨텐츠 엔드포인트를 하나의 컨테이너에서 제공합니다.

```kotlin
// 싱글턴 — 모든 테스트에서 공유
val server = BluetapeHttpServer.Launcher.bluetapeHttpServer

// 미리 구성된 URL 헬퍼
val baseUrl             = server.url                // http://host:<port>
val httpbinUrl          = server.httpbinUrl         // http://host:<port>/httpbin
val jsonplaceholderUrl  = server.jsonplaceholderUrl // http://host:<port>/jsonplaceholder
val webUrl              = server.webUrl             // http://host:<port>/web
```

#### 자동 등록 시스템 프로퍼티

`start()` 이후 아래 시스템 프로퍼티가 자동으로 등록됩니다:

| 프로퍼티 키                                            | 예시 값                                    |
|---------------------------------------------------|-----------------------------------------|
| `testcontainers.bluetape-http.host`               | `localhost`                             |
| `testcontainers.bluetape-http.port`               | `<동적>`                                  |
| `testcontainers.bluetape-http.url`                | `http://localhost:<동적>`                 |
| `testcontainers.bluetape-http.httpbinUrl`         | `http://localhost:<동적>/httpbin`         |
| `testcontainers.bluetape-http.jsonplaceholderUrl` | `http://localhost:<동적>/jsonplaceholder` |
| `testcontainers.bluetape-http.webUrl`             | `http://localhost:<동적>/web`             |

#### Spring Boot `application-test.yml`

```yaml
mock:
  server:
    url: ${testcontainers.bluetape-http.url}
    httpbin-url: ${testcontainers.bluetape-http.httpbinUrl}
    jsonplaceholder-url: ${testcontainers.bluetape-http.jsonplaceholderUrl}
```

#### 수동 인스턴스 (싱글턴 미사용)

```kotlin
// 동적 포트 (기본값)
val server = BluetapeHttpServer().apply { start() }

// 포트 80 고정 바인딩 (컨테이너 내부 포트)
val server = BluetapeHttpServer(useDefaultPort = true).apply { start() }
```

### BluetapeWebfluxServer (Spring WebFlux + Coroutines)

`BluetapeWebfluxServer`는 `bluetape4k/mock-webflux-server` Docker 이미지를 실행합니다.
Spring Boot 4 WebFlux + Coroutines 기반으로, httpbin/jsonplaceholder/web 엔드포인트를 동일하게 제공합니다.

```kotlin
// 싱글턴 — 모든 테스트에서 공유
val server = BluetapeWebfluxServer.Launcher.bluetapeWebfluxServer

// 미리 구성된 URL 헬퍼
val baseUrl             = server.url                // http://host:<port>
val httpbinUrl          = server.httpbinUrl         // http://host:<port>/httpbin
val jsonplaceholderUrl  = server.jsonplaceholderUrl // http://host:<port>/jsonplaceholder
val webUrl              = server.webUrl             // http://host:<port>/web
```

#### 자동 등록 시스템 프로퍼티

`start()` 이후 아래 시스템 프로퍼티가 자동으로 등록됩니다:

| 프로퍼티 키                                                  | 예시 값                                    |
|----------------------------------------------------------|-----------------------------------------|
| `testcontainers.bluetape-webflux.host`                   | `localhost`                             |
| `testcontainers.bluetape-webflux.port`                   | `<동적>`                                  |
| `testcontainers.bluetape-webflux.url`                    | `http://localhost:<동적>`                 |
| `testcontainers.bluetape-webflux.httpbin-url`            | `http://localhost:<동적>/httpbin`         |
| `testcontainers.bluetape-webflux.jsonplaceholder-url`    | `http://localhost:<동적>/jsonplaceholder` |
| `testcontainers.bluetape-webflux.web-url`                | `http://localhost:<동적>/web`             |
| `testcontainers.bluetape-webflux.https-port`             | `<동적>`                                  |
| `testcontainers.bluetape-webflux.https-url`              | `https://localhost:<동적>`                |

#### Spring Boot `application-test.yml`

```yaml
mock:
  webflux:
    url: ${testcontainers.bluetape-webflux.url}
    httpbin-url: ${testcontainers.bluetape-webflux.httpbinUrl}
    jsonplaceholder-url: ${testcontainers.bluetape-webflux.jsonplaceholderUrl}
```

#### 수동 인스턴스 (싱글턴 미사용)

```kotlin
// 동적 포트 (기본값)
val server = BluetapeWebfluxServer().apply { start() }

// 포트 80 고정 바인딩
val server = BluetapeWebfluxServer(useDefaultPort = true).apply { start() }
```

### 인증 서버

```kotlin
val keycloak = KeycloakServer.Launcher.keycloak
// Keycloak 17+ (Quarkus 기반): context path = "/"
println("Auth Server URL: ${keycloak.getAuthServerUrl()}")
println("Admin Username: ${keycloak.getAdminUsername()}")
println("Admin Password: ${keycloak.getAdminPassword()}")
```

### 시계열 DB

```kotlin
val influxDB = InfluxDBServer.Launcher.influxDB
println("URL: ${influxDB.url}")
println("Admin Token: ${influxDB.adminToken}")
println("Bucket: ${influxDB.bucket}")
println("Organization: ${influxDB.organization}")
```

### 카오스 테스트 (Toxiproxy)

```mermaid
sequenceDiagram
    autonumber
        participant TEST as 테스트 코드
        participant REDIS as RedisServer
        participant TOXI as ToxiproxyServer
        participant API as ToxiproxyClient
        participant LETTUCE as Lettuce Client

    TEST->>REDIS: start() withNetwork(network)
    TEST->>TOXI: start() withNetwork(network)
    TEST->>API: createProxy("redis-primary", "0.0.0.0:8666", "redis:6379")
    API-->>TOXI: 프록시 생성 및 listen/upstream 구성

    TEST->>LETTUCE: connect(toxiproxy.host, toxiproxy.getMappedPort(8666))
    LETTUCE->>TOXI: PING / SET / GET 요청
    TOXI->>REDIS: redis:6379 로 요청 전달
    REDIS-->>TOXI: 응답 반환
    TOXI-->>LETTUCE: 프록시 응답 반환

    TEST->>API: proxy.toxics().latency(..., DOWNSTREAM, 250)
    API-->>TOXI: downstream latency toxic 추가
    LETTUCE->>TOXI: GET 요청
    TOXI-->>LETTUCE: 지연 후 응답 반환

    TEST->>API: latency.remove()
    API-->>TOXI: toxic 제거
    LETTUCE->>TOXI: GET 요청
    TOXI-->>LETTUCE: 정상 속도로 응답 반환
```

- `RedisServer`는 실제 Upstream 서버입니다.
- `ToxiproxyServer`는 프록시 컨테이너입니다. Control API 포트(`8474`)와 프록시 포트 범위(`8666~8697`)를 노출합니다.
- `ToxiproxyClient`는 Control API에 붙어서 프록시를 만들고 toxic을 추가/삭제하는 관리용 클라이언트입니다.
- `DOWNSTREAM latency`는 Upstream 응답이 클라이언트로 돌아오는 구간을 늦춥니다.

### LLM (ChromaDB + Ollama)

```kotlin
// ChromaDB — 임베딩 검색용 벡터 저장소
val chromaDb = ChromaDBServer.Launcher.chromaDb
println("ChromaDB URL: ${chromaDb.url}")   // http://host:8000

// Ollama — 로컬 LLM 추론 (소형 모델은 GPU 불필요)
val ollama = OllamaServer.Launcher.ollama
println("Ollama URL: ${ollama.url}")       // http://host:11434
```

### 분산 캐시 / 그리드

```kotlin
// Hazelcast 5.x
val hazelcast = HazelcastServer.Launcher.hazelcast
val client = HazelcastClient.newHazelcastClient(
    ClientConfig().apply { networkConfig.addAddress("${hazelcast.host}:${hazelcast.port}") }
)

// Apache Ignite 2.x — 씬 클라이언트 포트 10800
val ignite2 = Ignite2Server.Launcher.ignite2
val client = IgniteClient.start(ClientConfiguration().apply {
    setAddresses("${ignite2.host}:${ignite2.port}")
})

// Apache Ignite 3.x — 클러스터 자동 초기화, 씬 클라이언트 포트 10800
val ignite3 = Ignite3Server.Launcher.ignite3
val client = IgniteClient.builder()
    .addresses("${ignite3.host}:${ignite3.port}")
    .build()
```

### AWS 에뮬레이터

`AwsEmulatorServer`는 로컬 AWS 에뮬레이터 공통 인터페이스입니다.

```kotlin
// MiniStackServer — MIT 라이선스, 31+ 서비스, 권장
val miniStack = MiniStackServer.Launcher.miniStack
val s3Client = S3Client.builder()
    .endpointOverride(miniStack.awsEndpoint)
    .credentialsProvider(miniStack.getCredentialProvider())
    .region(Region.of(miniStack.regionName))
    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
    .build()

// MiniStack은 모든 서비스가 항상 활성화 — withServices() 불필요
val kmsClient = KmsClient.builder()
    .endpointOverride(miniStack.awsEndpoint)
    .credentialsProvider(miniStack.getCredentialProvider())
    .region(Region.of(miniStack.regionName))
    .build()

// FlociServer — GraalVM Native (@Deprecated)
val floci = FlociServer.Launcher.floci
val sqsClient = SqsClient.builder()
    .endpointOverride(floci.awsEndpoint)
    .credentialsProvider(floci.getCredentialProvider())
    .region(Region.of(floci.regionName))
    .build()

// ElasticMqServer — 임베디드 JVM SQS, Docker 불필요
val elasticMq = ElasticMqServer.Launcher.elasticMq
val sqsClient = SqsClient.builder()
    .endpointOverride(elasticMq.sqsEndpoint)
    .region(Region.of("us-east-1"))
    .credentialsProvider(AnonymousCredentialsProvider.create())
    .build()

// MailpitServer — SMTP + Web UI
val mailpit = MailpitServer.Launcher.mailpit
println("SMTP 포트: ${mailpit.smtpPort}")
println("Web UI: ${mailpit.uiUrl}")
```

### 분산 SQL 쿼리 엔진

```kotlin
val trino = TrinoServer.Launcher.trino
val conn = DriverManager.getConnection(
    "jdbc:trino://${trino.host}:${trino.port}/memory",
    "test",
    null
)
val stmt = conn.createStatement()
val rs = stmt.executeQuery("SELECT 1 as num")
```

### 시스템 프로퍼티 조회

```kotlin
// 예제 1: start() 후 시스템 프로퍼티 직접 조회
val postgresUrl = System.getProperty("testcontainers.postgresql.jdbc-url")
val kafkaServers = System.getProperty("testcontainers.kafka.bootstrap-servers")

// 예제 2: registerSystemProperties() — 테스트 후 자동 복원
@BeforeEach
fun setup() {
    registration = PostgreSQLServer.Launcher.postgres.registerSystemProperties()
}

@AfterEach
fun cleanup() {
    registration.close()
}
```

## Spring Boot 환경설정

```kotlin
class MyRepositoryTest {
    companion object {
        private val mysql = MySQL8Server(useDefaultPort = true)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mysql.start()  // 내부에서 testcontainers.mysql.* 시스템 프로퍼티 등록
        }
    }
}
```

```yaml
spring:
  datasource:
    driver-class-name: ${testcontainers.mysql.driver-class-name}
    url: ${testcontainers.mysql.jdbc-url}
    username: ${testcontainers.mysql.username}
    password: ${testcontainers.mysql.password}

  data:
    redis:
      host: ${testcontainers.redis.host}
      port: ${testcontainers.redis.port}

  kafka:
    bootstrap-servers: ${testcontainers.kafka.bootstrap-servers}
```

직접 Testcontainers 를 사용할 때 필요한 `@DynamicPropertySource` 등록 코드를, 이 모듈에서는 시스템 프로퍼티 자동 등록으로 단순화할 수 있습니다.

## 최근 안정성 개선

- `GenericContainer.exposeCustomPorts(...)`가 `hostConfig`가 비어 있는 경우에도 포트 바인딩을 생성하도록 보강되었습니다.
- `GenericServer.writeToSystemProperties(...)`는 기본/추가 속성을 일관된 순서로 구성하여 일괄 등록합니다.
- `KafkaServer.Launcher`의 문자열 producer/consumer 생성 시 serializer/deserializer 인스턴스를 호출마다 새로 생성해
  `close()` 이후 재사용 이슈를 방지합니다.
- `TiDBServer`는 Testcontainers 2.x 미지원으로 deprecated 처리되었으며, 신규 테스트에서는 `MySQL8Server` 사용을 권장합니다.

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers:${version}")
}
```

## 참고

- [Testcontainers](https://www.testcontainers.org/)
- [Floci](https://github.com/atlassian-labs/floci) — GraalVM Native AWS 에뮬레이터 (권장)
- [ElasticMQ](https://github.com/softwaremill/elasticmq) — 임베디드 SQS 서버
- [Mailpit](https://github.com/axllent/mailpit) — SMTP 이메일 테스트 도구
- [LocalStack](https://www.localstack.cloud/) — @Deprecated: Community edition 2026-03-23 archived

## Colima + LocalStack 문제해결

Colima 환경에서 `LocalStackContainer` 실행 시 Docker 소켓 관련 오류가 나는 경우:

```bash
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
```

문제가 계속되면 Colima 소켓을 정리 후 재시작:

```bash
brew services stop colima
colima stop
rm -f ~/.colima/docker.sock
brew services start colima
```

Ryuk 컨테이너가 문제를 일으키면 아래 설정을 임시로 사용할 수 있습니다:

```bash
export TESTCONTAINERS_RYUK_DISABLED=true
```

> **주의**: `TESTCONTAINERS_RYUK_DISABLED=true`는 리소스 자동 정리에 영향을 줄 수 있으므로 CI/공용 환경에서는 신중히 사용하세요.
