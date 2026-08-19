# Module bluetape4k-testcontainers

[English](./README.md) | 한국어

Testcontainers `2.0.3` 기반 통합 테스트를 빠르게 구성하기 위한 서버 래퍼/유틸 라이브러리입니다.

## 아키텍처

### 컨테이너 생명주기 다이어그램

![testcontainers Sequence Flow diagram](../../docs/images/readme-diagrams/testing-testcontainers-sequence-01.png)

### 지원 컨테이너 클래스 다이어그램

![Testcontainers Core Contract Class Diagram](../../docs/images/readme-diagrams/testing-testcontainers-diagram-01.png)

### 지원 컨테이너 구조

![Testcontainers Supported Container Structure](../../docs/images/readme-diagrams/testing-testcontainers-diagram-02.png)

## 주요 기능

- **DB 서버 지원**: MySQL, MariaDB, PostgreSQL, PostGIS, pgvector, Cockroach, ClickHouse
- **Graph DB 서버 지원**: Neo4j, Memgraph, FalkorDB, PostgreSQL + Apache AGE
- **Storage 서버 지원**: Redis/Redis Cluster, MongoDB, Cassandra, Elasticsearch/OSS/OpenSearch, MinIO, InfluxDB
- `MinIOServer`는 명시적인 MinIO 호환성 테스트용으로 유지하며, 신규 AWS/S3 에뮬레이터 테스트는 `FlociServer` 또는 `MiniStackServer`를 사용하세요.
- **분산 캐시/그리드**: `HazelcastServer` (5.x slim), `Ignite2Server`, `Ignite3Server` (클러스터 자동 초기화)
- **MQ 서버 지원**: Kafka, RabbitMQ, Pulsar, Nats, Redpanda
- **Infra 서버 지원**: Consul, Vault, Prometheus, Jaeger, Zipkin, ZooKeeper, Toxiproxy, Keycloak
- **분산 SQL 엔진**: Trino
- **HTTP Mock 지원**: WireMock, NginxServer
- **LLM 지원**: `ChromaDBServer` (벡터 DB, 포트 8000), `OllamaServer` (로컬 LLM 추론, 포트 11434)
- **AWS
  에뮬레이터**: `AwsEmulatorServer` 공통 인터페이스; `MiniStackServer`(31+ 서비스, MIT 라이선스, 권장), `FlociServer`(LocalStack 대체 OSS 경로), `LocalStackServer`(@Deprecated)
- **임베디드 SQS**: `ElasticMqServer` — Docker 없이 JVM 내 SQS 서버 실행
- **메일 테스트**: `MailpitServer` — SMTP + Web UI로 이메일 통합 테스트 지원
-

**관측성**: `ZipkinServer` — 분산 추적 (`openzipkin/zipkin-slim:2.23`), `GrafanaServer` — 대시보드 + 데이터소스 프로비저닝 (`grafana/grafana:13.1.3`), `K3sServer` — 경량 Kubernetes 클러스터 (`rancher/k3s`; `--privileged` Docker 모드 필요)
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

| 서버                   | namespace           | 주요 키                                                                                                                                                              |
|------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PostgreSQLServer       | `postgresql`        | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`                                                                                             |
| PostgisServer          | `postgis`           | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`                                                                                             |
| PgvectorServer         | `pgvector`          | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`                                                                                             |
| MySQL8Server           | `mysql`             | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`                                                                                             |
| MariaDBServer          | `mariadb`           | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`                                                                                             |
| CockroachServer        | `cockroach`         | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`                                                                                             |
| ClickHouseServer       | `clickhouse`        | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`                                                                                             |
| TrinoServer            | `trino`             | `jdbc-url`, `username`                                                                                                                                               |
| RedisServer            | `redis`             | `host`, `port`, `url`                                                                                                                                                |
| MongoDBServer          | `mongo`             | `host`, `port`, `url`                                                                                                                                                |
| ElasticsearchServer    | `elasticsearch`     | `host`, `port`, `url`                                                                                                                                                |
| KafkaServer            | `kafka`             | `host`, `port`, `url`, `bootstrap-servers`, `bound-port-numbers`                                                                                                     |
| RedpandaServer         | `redpanda`          | `host`, `port`, `url`, `admin-port`, `schema-registry-port`, `rest-proxy-port`                                                                                       |
| NatsServer             | `nats`              | `host`, `port`, `url`, `cluster-port`, `monitor-port`                                                                                                                |
| PulsarServer           | `pulsar`            | `host`, `port`, `url`, `broker-url`, `broker-port`, `broker-http-port`                                                                                               |
| RabbitMQServer         | `rabbitmq`          | `host`, `port`, `url`, `amqp-url`, `amqp-port`, `amqps-port`, `management-url`                                                                                       |
| LocalStackServer       | `localstack`        | `host`, `port`, `url`, `awsEndpoint`, `awsAccessKey`, `awsSecretKey`, `regionName`                                                                                   |
| FlociServer            | `floci`             | `host`, `port`, `url`, `awsEndpoint`, `awsAccessKey`, `awsSecretKey`, `regionName`                                                                                   |
| MiniStackServer        | `ministack`         | `host`, `port`, `url`, `awsEndpoint`, `awsAccessKey`, `awsSecretKey`, `regionName`                                                                                   |
| ElasticMqServer        | `elasticmq`         | `host`, `port`, `url`, `sqsEndpoint`                                                                                                                                 |
| MailpitServer          | `mailpit`           | `host`, `port`, `url`, `smtpPort`, `uiPort`, `uiUrl`                                                                                                                 |
| PrometheusServer       | `prometheus`        | `host`, `port`, `url`, `server-port`, `pushgateway-port`, `graphite-exporter-port`                                                                                   |
| GrafanaServer          | `grafana`           | `host`, `port`, `url`                                                                                                                                                |
| K3sServer              | `k3s`               | `host`, `port`, `url`                                                                                                                                                |
| ConsulServer           | `consul`            | `host`, `port`, `url`, `dns-port`, `http-port`, `rpc-port`                                                                                                           |
| JaegerServer           | `jaeger`            | `host`, `port`, `url`, `frontend-port`, `zipkin-port`, `config-port`, `thrift-port`                                                                                  |
| ElasticsearchOssServer | `elasticsearch-oss` | `host`, `port`, `url`                                                                                                                                                |
| HazelcastServer        | `hazelcast`         | `host`, `port`, `url`                                                                                                                                                |
| Ignite2Server          | `ignite2`           | `host`, `port`, `url`                                                                                                                                                |
| Ignite3Server          | `ignite3`           | `host`, `port`, `url`, `rest-port`                                                                                                                                   |
| ZipkinServer           | `zipkin`            | `host`, `port`, `url`                                                                                                                                                |
| NginxServer            | `nginx`             | `host`, `port`, `url`                                                                                                                                                |
| ChromaDBServer         | `chromadb`          | `host`, `port`, `url`                                                                                                                                                |
| OllamaServer           | `ollama`            | `host`, `port`, `url`                                                                                                                                                |
| BluetapeHttpServer     | `bluetape-http`     | `host`, `port`, `url`, `httpbin-url`, `jsonplaceholder-url`, `web-url`, `https-port`, `https-url`, `https-httpbin-url`, `https-jsonplaceholder-url`, `https-web-url` |
| BluetapeWebfluxServer  | `bluetape-webflux`  | `host`, `port`, `url`, `httpbin-url`, `jsonplaceholder-url`, `web-url`, `https-port`, `https-url`, `https-httpbin-url`, `https-jsonplaceholder-url`, `https-web-url` |

## 기본 Docker 이미지 태그

아래 기본값은 2026-08-10에 확인한 최신 안정 이미지 태그를 고정한 것입니다.
재현 가능한 로컬·CI 실행을 위해 변경 가능한 `latest`, major-only, rolling minor
태그는 사용하지 않습니다.

| 그룹 | 서버 | 이미지 | 기본 태그 |
|---|---|---|---|
| AWS | `DynamoDbLocalServer` | `amazon/dynamodb-local` | `3.3.1` |
| AWS | `FlociServer` | `floci/floci` | `1.6.0` |
| AWS | `LocalStackServer` | `localstack/localstack` | `4` (deprecated wrapper) |
| AWS | `MiniStackServer` | `ministackorg/ministack` | `1.4.14` |
| Database | `ClickHouseServer` | `clickhouse/clickhouse-server` | `26.7.3.19` |
| Database | `CockroachServer` | `cockroachdb/cockroach` | `v25.4.14` |
| Database | `MariaDBServer` | `mariadb` | `12.3.2` |
| Database | `MySQL5Server` | `biarms/mysql` | `5` |
| Database | `MySQL8Server` | `mysql` | `8.4.11` |
| Database | `PgvectorServer` | `pgvector/pgvector` | `0.8.6-pg16` |
| Database | `PostgisServer` | `postgis/postgis` | `16-3.5` |
| Database | `PostgreSQLServer` | `postgres` | `18.4-alpine` |
| Database | `TrinoServer` | `trinodb/trino` | `483` |
| Graph DB | `FalkorDBServer` | `falkordb/falkordb` | `v4.20.2` |
| Graph DB | `MemgraphServer` | `memgraph/memgraph` | `3.12.0` |
| Graph DB | `Neo4jServer` | `neo4j` | `5.26.29` |
| Graph DB | `PostgreSQLAgeServer` | `apache/age` | `release_PG18_1.7.0` |
| HTTP | `BluetapeHttpServer` | `bluetape4k/mock-web-server` | `2.0.0` |
| HTTP | `BluetapeWebfluxServer` | `bluetape4k/mock-webflux-server` | `2.0.0` |
| HTTP | `NginxServer` | `nginx` | `1.30.4-alpine` |
| HTTP | `WireMockServer` | `wiremock/wiremock` | `3.13.2` |
| Infrastructure | `ConsulServer` | `hashicorp/consul` | `1.22.7` |
| Infrastructure | `EtcdServer` | `gcr.io/etcd-development/etcd` | `v3.6.14` |
| Infrastructure | `GrafanaServer` | `grafana/grafana` | `13.1.3` |
| Infrastructure | `JaegerServer` | `jaegertracing/all-in-one` | `1.76.0` |
| Infrastructure | `K3sServer` | `rancher/k3s` | `v1.36.3-k3s1` |
| Infrastructure | `KeycloakServer` | `quay.io/keycloak/keycloak` | `26.7.1` |
| Infrastructure | `PrometheusServer` | `prom/prometheus` | `v3.13.2` |
| Infrastructure | `ToxiproxyServer` | `ghcr.io/shopify/toxiproxy` | `2.9.0` |
| Infrastructure | `VaultServer` | `hashicorp/vault` | `1.20.4` |
| Infrastructure | `ZipkinServer` | `openzipkin/zipkin-slim` | `2.23` |
| Infrastructure | `ZooKeeperServer` | `zookeeper` | `3.9.5` |
| LLM | `ChromaDBServer` | `chromadb/chroma` | `0.5.23` |
| LLM | `OllamaServer` | `ollama/ollama` | `0.32.6` |
| Mail | `MailpitServer` | `axllent/mailpit` | `v1.30.7` |
| Messaging | `KafkaServer` | `confluentinc/cp-kafka` | `7.5.16` |
| Messaging | `NatsServer` | `nats` | `2.14.4` |
| Messaging | `PulsarServer` | `apachepulsar/pulsar` | `3.3.9` (Java 17+ 제약) |
| Messaging | `RabbitMQServer` | `rabbitmq` | `3.13` (4.x wrapper 실패) |
| Messaging | `RedpandaServer` | `docker.redpanda.com/redpandadata/redpanda` | `v26.2.1` |
| Storage | `CassandraServer` | `cassandra` | `5.0.8` |
| Storage | `ElasticsearchOssServer` | `docker.elastic.co/elasticsearch/elasticsearch-oss` | `7.10.2` |
| Storage | `ElasticsearchServer` | `docker.elastic.co/elasticsearch/elasticsearch` | `9.5.0` |
| Storage | `HazelcastServer` | `hazelcast/hazelcast` | `5.7.0-slim-jdk25` |
| Storage | `Ignite2Server` | `apacheignite/ignite` | `2.18.0` (aarch64는 `-arm64`) |
| Storage | `Ignite3Server` | `apacheignite/ignite` | `3.1.0` |
| Storage | `InfluxDBServer` | `influxdb` | `2.9.1` |
| Storage | `MinIOServer` | `minio/minio` | `RELEASE.2025-07-23T15-54-02Z` (호환성 fixture) |
| Storage | `MongoDBServer` | `mongo` | `8.0.28` |
| Storage | `OpenSearchServer` | `opensearchproject/opensearch` | `3.8.0` |
| Storage | `RedisClusterServer` | `tommy351/redis-cluster` | `6.2` (호환성 fixture) |
| Storage | `RedisServer` | `redis` | `8.8.1` |

예외는 의도적으로 유지합니다. `MySQL5Server`는 ARM을 지원하는
`biarms/mysql:5` 별칭을 사용하고, `ElasticsearchOssServer`는 legacy OSS 이미지에
고정합니다. `ChromaDBServer`는 개발 버전이 아닌 마지막 안정 태그를 사용하며,
`ZipkinServer`는 최신 이미지에서 현재 계약이 실패하므로 `2.23`을 유지합니다.
`PulsarServer`와 `RabbitMQServer`는 wrapper와 호환되는 major 버전을 유지하고,
`MinIOServer`와 `RedisClusterServer`는 명시적 호환성 fixture로 남깁니다.
`LocalStackServer`는 deprecated이므로 새 AWS 테스트에는 `FlociServer` 또는
`MiniStackServer`를 사용하세요.

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

| 서버 클래스           | Docker 이미지       | 기본 태그            | 프로토콜  | 기본 포트   |
|-----------------------|---------------------|----------------------|-----------|-------------|
| `Neo4jServer`         | `neo4j`             | `5.26.29`            | Bolt/HTTP | 7687 / 7474 |
| `MemgraphServer`      | `memgraph/memgraph` | `3.12.0`             | Bolt      | 7687        |
| `FalkorDBServer`      | `falkordb/falkordb` | `v4.20.2`            | Redis     | 6379        |
| `PostgreSQLAgeServer` | `apache/age`        | `release_PG18_1.7.0` | JDBC      | 5432        |

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

`BluetapeHttpServer`는 `bluetape4k/mock-web-server` Docker 이미지를 실행합니다. httpbin, jsonplaceholder, web 컨텐츠 엔드포인트를 하나의 컨테이너에서 제공합니다.

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

| 프로퍼티 키                                        | 예시 값                                   |
|----------------------------------------------------|-------------------------------------------|
| `testcontainers.bluetape-http.host`                | `localhost`                               |
| `testcontainers.bluetape-http.port`                | `<동적>`                                  |
| `testcontainers.bluetape-http.url`                 | `http://localhost:<동적>`                 |
| `testcontainers.bluetape-http.httpbin-url`         | `http://localhost:<동적>/httpbin`         |
| `testcontainers.bluetape-http.jsonplaceholder-url` | `http://localhost:<동적>/jsonplaceholder` |
| `testcontainers.bluetape-http.web-url`             | `http://localhost:<동적>/web`             |

#### Spring Boot `application-test.yml`

```yaml
mock:
  server:
    url: ${testcontainers.bluetape-http.url}
    httpbin-url: ${testcontainers.bluetape-http.httpbin-url}
    jsonplaceholder-url: ${testcontainers.bluetape-http.jsonplaceholder-url}
```

#### 수동 인스턴스 (싱글턴 미사용)

```kotlin
// 동적 포트 (기본값)
val server = BluetapeHttpServer().apply { start() }

// 포트 80 고정 바인딩 (컨테이너 내부 포트)
val server = BluetapeHttpServer(useDefaultPort = true).apply { start() }
```

### BluetapeWebfluxServer (Spring WebFlux + Coroutines)

`BluetapeWebfluxServer`는 `bluetape4k/mock-webflux-server` Docker 이미지를 실행합니다. Spring Boot 4 WebFlux + Coroutines 기반으로, httpbin/jsonplaceholder/web 엔드포인트를 동일하게 제공합니다.

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

| 프로퍼티 키                                           | 예시 값                                   |
|-------------------------------------------------------|-------------------------------------------|
| `testcontainers.bluetape-webflux.host`                | `localhost`                               |
| `testcontainers.bluetape-webflux.port`                | `<동적>`                                  |
| `testcontainers.bluetape-webflux.url`                 | `http://localhost:<동적>`                 |
| `testcontainers.bluetape-webflux.httpbin-url`         | `http://localhost:<동적>/httpbin`         |
| `testcontainers.bluetape-webflux.jsonplaceholder-url` | `http://localhost:<동적>/jsonplaceholder` |
| `testcontainers.bluetape-webflux.web-url`             | `http://localhost:<동적>/web`             |
| `testcontainers.bluetape-webflux.https-port`          | `<동적>`                                  |
| `testcontainers.bluetape-webflux.https-url`           | `https://localhost:<동적>`                |

#### Spring Boot `application-test.yml`

```yaml
mock:
  webflux:
    url: ${testcontainers.bluetape-webflux.url}
    httpbin-url: ${testcontainers.bluetape-webflux.httpbin-url}
    jsonplaceholder-url: ${testcontainers.bluetape-webflux.jsonplaceholder-url}
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

### Grafana

```kotlin
// 싱글턴 런처
val grafana = GrafanaServer.Launcher.grafana

// 시작 후 Prometheus 데이터소스 등록
grafana.withPrometheusDataSource("http://prometheus:9090")

// 대시보드 JSON 으로 대시보드 등록
// grafana.withDashboard(dashboardJsonString)

println("Grafana URL: ${grafana.url}")  // http://host:<port>
// 기본 자격증명: admin / admin
```

### K3s (Kubernetes)

```kotlin
// 싱글턴 런처 — K3s는 시작이 느리므로 테스트 간 싱글턴 사용 권장
val k3s = K3sServer.Launcher.k3s

// fabric8 Kubernetes 클라이언트 빌드
val client = k3s.kubernetesClient()
client.use {
    println(it.namespaces().list().items)
}
```

> **주의**: K3s는 `--privileged` Docker 모드가 필요합니다. `K3sServer`를 사용하는 테스트에는
> `@Tag("k8s")`를 붙여 권한 있는 런너를 사용하는 nightly CI 에서만 실행되도록 제한하세요.
> 테스트 런타임 클래스패스에 `io.fabric8:kubernetes-client` 의존성을 추가해야 합니다.

### 카오스 테스트 (Toxiproxy)

![(Toxiproxy) diagram](../../docs/images/readme-diagrams/testing-testcontainers-sequence-02.png)

- `RedisServer`는 실제 Upstream 서버입니다.
- `ToxiproxyServer`는 프록시 컨테이너입니다. Control API 포트 (`8474`)와 프록시 포트 범위 (`8666~8697`)를 노출합니다.
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

// FlociServer — GraalVM Native 기반 LocalStack 대체 OSS 경로
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
- `TiDBServer` 지원은 Testcontainers 2.x 미지원으로 제거되었으며, 신규 테스트에서는 `MySQL8Server` 사용을 권장합니다.

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers:${version}")
}
```

## 참고

- [Testcontainers](https://www.testcontainers.org/)
- [Floci](https://github.com/floci-io/floci) — GraalVM Native AWS 에뮬레이터, LocalStack 대체 OSS 경로
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
