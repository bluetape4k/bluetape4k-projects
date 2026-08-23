# Module bluetape4k-testcontainers

English | [한국어](./README.ko.md)

A server wrapper and utility library for building integration tests quickly on top of Testcontainers `2.0.3`.

## Architecture

### Container Lifecycle

![Container Lifecycle diagram](../../docs/images/readme-diagrams/testing-testcontainers-sequence-01.png)

### Supported Container Class Diagram

![Testcontainers Core Contract Class Diagram](../../docs/images/readme-diagrams/testing-testcontainers-diagram-01.png)

### Supported Container Structure

![Testcontainers Supported Container Structure](../../docs/images/readme-diagrams/testing-testcontainers-diagram-02.png)

## Key Features

- Wrappers for database, graph DB, storage, messaging, infrastructure, distributed SQL, and LLM services
- `MinIOServer` remains available for explicit MinIO compatibility tests; use `FlociServer` or `MiniStackServer` for new AWS/S3 emulator tests
- HTTP mocking through WireMock and NginxServer
- **AWS Emulator
  support**: `AwsEmulatorServer` common interface; `MiniStackServer` (31+ services, MIT license, recommended), `FlociServer` (open-source LocalStack replacement), `LocalStackServer` (@Deprecated)
- **Embedded SQS**: `ElasticMqServer` runs an in-process SQS server — no Docker needed
- **Mail testing**: `MailpitServer` provides SMTP + Web UI for email integration tests
- **LLM support**: `ChromaDBServer` (vector DB, port 8000), `OllamaServer` (local LLM inference, port 11434)
- **Distributed cache/grid**: `HazelcastServer` (5.x slim), `Ignite2Server`, `Ignite3Server` (auto cluster-init)
-

**Observability**: `ZipkinServer` (distributed tracing, `openzipkin/zipkin-slim:2.23`), `GrafanaServer` (dashboards + datasource provisioning, `grafana/grafana:13.1.3`), `K3sServer` (lightweight Kubernetes cluster, `rancher/k3s`; requires `--privileged` Docker mode)
- Shared `GenericServer` / `GenericContainer` utilities
- Automatic PostgreSQL extension activation for PostGIS and pgvector
- Declarative activation of extra PostgreSQL extensions through `withExtensions()`
- Optional fixed-port mapping with `useDefaultPort=true`
- Automatic export of connection details as system properties at `start()` time
- Simplified Spring Boot wiring through `${testcontainers...}` placeholders

## System Property Export (`PropertyExportingServer`)

Every server implements
`PropertyExportingServer`, which automatically registers connection details as system properties at `start()` time.

- Property keys use lowercase kebab-case
- Format: `testcontainers.{namespace}.{kebab-case-key}`
- Examples: `testcontainers.postgresql.jdbc-url`, `testcontainers.kafka.bootstrap-servers`

### Exported Keys by Server

| Server                 | namespace           | Key properties                                                                                                                                                       |
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

## Default Docker Image Tags

The defaults below are pinned to the latest stable image tag verified on
2026-08-10. Mutable `latest`, major-only, and rolling minor tags are avoided so
that Testcontainers runs remain reproducible across local machines and CI.

| Group | Server | Image | Default tag |
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
| Messaging | `PulsarServer` | `apachepulsar/pulsar` | `3.3.9` (Java 17+ constraint) |
| Messaging | `RabbitMQServer` | `rabbitmq` | `3.13` (4.x wrapper failure) |
| Messaging | `RedpandaServer` | `docker.redpanda.com/redpandadata/redpanda` | `v26.2.1` |
| Storage | `CassandraServer` | `cassandra` | `5.0.8` |
| Storage | `ElasticsearchOssServer` | `docker.elastic.co/elasticsearch/elasticsearch-oss` | `7.10.2` |
| Storage | `ElasticsearchServer` | `docker.elastic.co/elasticsearch/elasticsearch` | `9.5.0` |
| Storage | `HazelcastServer` | `hazelcast/hazelcast` | `5.7.0-slim-jdk25` |
| Storage | `Ignite2Server` | `apacheignite/ignite` | `2.18.0` (`-arm64` on aarch64) |
| Storage | `Ignite3Server` | `apacheignite/ignite` | `3.1.0` |
| Storage | `InfluxDBServer` | `influxdb` | `2.9.1` |
| Storage | `MinIOServer` | `minio/minio` | `RELEASE.2025-07-23T15-54-02Z` (compatibility fixture) |
| Storage | `MongoDBServer` | `mongo` | `8.0.28` |
| Storage | `OpenSearchServer` | `opensearchproject/opensearch` | `3.8.0` |
| Storage | `RedisClusterServer` | `tommy351/redis-cluster` | `6.2` (compatibility fixture) |
| Storage | `RedisServer` | `redis` | `8.8.1` |

Compatibility exceptions are intentional: `MySQL5Server` keeps the ARM-capable
`biarms/mysql:5` alias, `ElasticsearchOssServer` remains on the legacy OSS image,
`ChromaDBServer` remains on its last stable non-development tag, `ZipkinServer`
stays on `2.23` because newer images fail its current contract, `PulsarServer`
and `RabbitMQServer` remain on the newest wrapper-compatible major versions,
`MinIOServer` and `RedisClusterServer` remain explicit compatibility fixtures,
and `PostgreSQLAgeServer` stays on the latest stable PG18 image rather than the
`1.8.0-rc0` release candidate. `LocalStackServer` is deprecated; new AWS tests
should use `FlociServer` or `MiniStackServer`.

## Image Family Startup and Workload Gate

The 52 Docker-backed server families are declared in
[`scripts/testcontainers_image_gate_manifest.json`](../../scripts/testcontainers_image_gate_manifest.json).
The manifest links each pinned image/tag to its Kotlin wrapper, representative
test class, readiness contract, workload evidence, and diagnostic commands.

The same runner is used by the full Nightly and stable release workflows. Pull
requests keep their low-cost JVM and module checks; the Docker-backed family
gate is intentionally reserved for the full Nightly/release path. The runner
executes the selected families sequentially (`max-parallel: 1`),
records `success`, `product_failure`, `infrastructure_failure`, or `blocked`,
and writes `summary.json`, `summary.md`, and one JSON file per family. Stable
publication requires `52/52`, `release_gate=true`, and zero failure-classification
counts. Docker Hub authentication and mirror settings are supplied through
environment variables or CI secrets; credentials are redacted from evidence.

<!-- TESTCONTAINERS_IMAGE_GATE_COMMAND_START -->
Run a targeted local family gate from the repository root when investigating a
specific change:

```bash
python3 scripts/run_testcontainers_image_gate.py \
  --scope changed \
  --report-dir build/reports/testcontainers-image-gate \
  --max-attempts 2 \
  --timeout-minutes 30
```

Run the complete release-equivalent gate:

```bash
./gradlew :bluetape4k-mock-web-server:jibDockerBuild \
  :bluetape4k-mock-webflux-server:jibDockerBuild \
  --no-configuration-cache
python3 scripts/run_testcontainers_image_gate.py \
  --scope full \
  --report-dir build/reports/testcontainers-image-gate \
  --max-attempts 2 \
  --timeout-minutes 30
```
<!-- TESTCONTAINERS_IMAGE_GATE_COMMAND_END -->

When a family fails, inspect its JSON artifact and the bounded Docker/K3s
diagnostics before retrying. Registry rate limits, daemon failures, and
readiness timeouts remain infrastructure evidence; they are not a reason to
skip the gate or publish a release.

## Usage Examples

### Database

```kotlin
val mysql = MySQL8Server(useDefaultPort = true).apply { start() }
val ds = mysql.getDataSource()
```

### PostgreSQL Extensions

```kotlin
// PostGIS — auto-activates postgis extension
val server = PostgisServer.Launcher.postgis

// pgvector — auto-activates vector extension
val server = PgvectorServer.Launcher.pgvector

// Extra extensions via withExtensions()
PostgisServer()
    .withExtensions("postgis_topology")
    .apply { start() }

PostgreSQLServer()
    .withExtensions("uuid-ossp", "hstore", "pg_trgm")
    .apply { start() }

// Singleton with extensions
val server = PostgreSQLServer.Launcher.withExtensions("uuid-ossp", "hstore")
```

### Graph DB

| Server                | Docker Image        | Default Tag          | Protocol  | Default Port |
|-----------------------|---------------------|----------------------|-----------|--------------|
| `Neo4jServer`         | `neo4j`             | `5.26.29`            | Bolt/HTTP | 7687 / 7474  |
| `MemgraphServer`      | `memgraph/memgraph` | `3.12.0`             | Bolt      | 7687         |
| `FalkorDBServer`      | `falkordb/falkordb` | `v4.20.2`            | Redis     | 6379         |
| `PostgreSQLAgeServer` | `apache/age`        | `release_PG18_1.7.0` | JDBC      | 5432         |

```kotlin
// Neo4j
val neo4j = Neo4jServer.Launcher.neo4j
val driver = GraphDatabase.driver(neo4j.boltUrl, AuthTokens.basic(neo4j.username, neo4j.password))

// Memgraph (Neo4j Bolt-compatible)
val memgraph = MemgraphServer.Launcher.memgraph
val driver = GraphDatabase.driver(memgraph.boltUrl, AuthTokens.none())

// PostgreSQL with Apache AGE
val age = PostgreSQLAgeServer.Launcher.postgresqlAge
val conn = DriverManager.getConnection(age.jdbcUrl, age.username, age.password)
```

### HTTP Mock Server

```kotlin
val wireMock = WireMockServer.Launcher.wireMock

wireMock.stubFor(
    get("/hello")
        .willReturn(ok("Hello!"))
)

verify(getRequestedFor(urlEqualTo("/hello")))
```

### BluetapeHttpServer (httpbin + jsonplaceholder + web)

`BluetapeHttpServer` runs the `bluetape4k/mock-web-server` Docker image, which provides httpbin, jsonplaceholder, and web-content endpoints in a single container.

```kotlin
// Singleton — starts once, shared across all tests
val server = BluetapeHttpServer.Launcher.bluetapeHttpServer

// Pre-built URL helpers
val baseUrl             = server.url                // http://host:<port>
val httpbinUrl          = server.httpbinUrl         // http://host:<port>/httpbin
val jsonplaceholderUrl  = server.jsonplaceholderUrl // http://host:<port>/jsonplaceholder
val webUrl              = server.webUrl             // http://host:<port>/web
```

#### Exported System Properties

After `start()`, the following system properties are registered automatically:

| Property Key                                       | Example Value                                |
|----------------------------------------------------|----------------------------------------------|
| `testcontainers.bluetape-http.host`                | `localhost`                                  |
| `testcontainers.bluetape-http.port`                | `<dynamic>`                                  |
| `testcontainers.bluetape-http.url`                 | `http://localhost:<dynamic>`                 |
| `testcontainers.bluetape-http.httpbin-url`         | `http://localhost:<dynamic>/httpbin`         |
| `testcontainers.bluetape-http.jsonplaceholder-url` | `http://localhost:<dynamic>/jsonplaceholder` |
| `testcontainers.bluetape-http.web-url`             | `http://localhost:<dynamic>/web`             |

#### Spring Boot `application-test.yml`

```yaml
mock:
  server:
    url: ${testcontainers.bluetape-http.url}
    httpbin-url: ${testcontainers.bluetape-http.httpbin-url}
    jsonplaceholder-url: ${testcontainers.bluetape-http.jsonplaceholder-url}
```

#### Manual instance (non-singleton)

```kotlin
// Dynamic port (default)
val server = BluetapeHttpServer().apply { start() }

// Fixed port 80 (container's internal port)
val server = BluetapeHttpServer(useDefaultPort = true).apply { start() }
```

### BluetapeWebfluxServer (Spring WebFlux + Coroutines)

`BluetapeWebfluxServer` runs the `bluetape4k/mock-webflux-server` Docker image — a Spring Boot 4 WebFlux + Coroutines variant with the same httpbin/jsonplaceholder/web endpoints.

```kotlin
// Singleton — starts once, shared across all tests
val server = BluetapeWebfluxServer.Launcher.bluetapeWebfluxServer

// Pre-built URL helpers
val baseUrl             = server.url                // http://host:<port>
val httpbinUrl          = server.httpbinUrl         // http://host:<port>/httpbin
val jsonplaceholderUrl  = server.jsonplaceholderUrl // http://host:<port>/jsonplaceholder
val webUrl              = server.webUrl             // http://host:<port>/web
```

#### Exported System Properties

After `start()`, the following system properties are registered automatically:

| Property Key                                          | Example Value                                |
|-------------------------------------------------------|----------------------------------------------|
| `testcontainers.bluetape-webflux.host`                | `localhost`                                  |
| `testcontainers.bluetape-webflux.port`                | `<dynamic>`                                  |
| `testcontainers.bluetape-webflux.url`                 | `http://localhost:<dynamic>`                 |
| `testcontainers.bluetape-webflux.httpbin-url`         | `http://localhost:<dynamic>/httpbin`         |
| `testcontainers.bluetape-webflux.jsonplaceholder-url` | `http://localhost:<dynamic>/jsonplaceholder` |
| `testcontainers.bluetape-webflux.web-url`             | `http://localhost:<dynamic>/web`             |
| `testcontainers.bluetape-webflux.https-port`          | `<dynamic>`                                  |
| `testcontainers.bluetape-webflux.https-url`           | `https://localhost:<dynamic>`                |

#### Spring Boot `application-test.yml`

```yaml
mock:
  webflux:
    url: ${testcontainers.bluetape-webflux.url}
    httpbin-url: ${testcontainers.bluetape-webflux.httpbin-url}
    jsonplaceholder-url: ${testcontainers.bluetape-webflux.jsonplaceholder-url}
```

#### Manual instance (non-singleton)

```kotlin
// Dynamic port (default)
val server = BluetapeWebfluxServer().apply { start() }

// Fixed port 80
val server = BluetapeWebfluxServer(useDefaultPort = true).apply { start() }
```

### Keycloak (Auth Server)

```kotlin
val keycloak = KeycloakServer.Launcher.keycloak
println("Auth Server URL: ${keycloak.getAuthServerUrl()}")
println("Admin Username: ${keycloak.getAdminUsername()}")
println("Admin Password: ${keycloak.getAdminPassword()}")
```

### InfluxDB (Time-series)

```kotlin
val influxDB = InfluxDBServer.Launcher.influxDB
println("URL: ${influxDB.url}")
println("Admin Token: ${influxDB.adminToken}")
println("Bucket: ${influxDB.bucket}")
println("Organization: ${influxDB.organization}")
```

### Grafana

```kotlin
// Singleton launcher
val grafana = GrafanaServer.Launcher.grafana

// Provision a Prometheus datasource after start
grafana.withPrometheusDataSource("http://prometheus:9090")

// Provision a dashboard from JSON
// grafana.withDashboard(dashboardJsonString)

println("Grafana URL: ${grafana.url}")  // http://host:<port>
// Default credentials: admin / admin
```

### K3s (Kubernetes)

```kotlin
// Singleton launcher — K3s is slow to start; prefer the singleton across tests
val k3s = K3sServer.Launcher.k3s

// Build a fabric8 Kubernetes client
val client = k3s.kubernetesClient()
client.use {
    println(it.namespaces().list().items)
}
```

> **Note**: K3s requires `--privileged` Docker mode. Tag tests that use `K3sServer`
> with `@Tag("k8s")` so they can be confined to nightly CI runs where a privileged
> runner is available. Add `io.fabric8:kubernetes-client` to the test runtime classpath.

### Toxiproxy (Chaos Testing)

![Toxiproxy (Chaos Testing) diagram](../../docs/images/readme-diagrams/testing-testcontainers-sequence-02.png)

### AWS Emulators

`AwsEmulatorServer` is the common interface for local AWS emulators.

```kotlin
// MiniStackServer — MIT license, 31+ services, recommended
val miniStack = MiniStackServer.Launcher.miniStack
val s3Client = S3Client.builder()
    .endpointOverride(miniStack.awsEndpoint)
    .credentialsProvider(miniStack.getCredentialProvider())
    .region(Region.of(miniStack.regionName))
    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
    .build()

// MiniStack supports all services by default — no withServices() needed
val kmsClient = KmsClient.builder()
    .endpointOverride(miniStack.awsEndpoint)
    .credentialsProvider(miniStack.getCredentialProvider())
    .region(Region.of(miniStack.regionName))
    .build()

// The pinned ministackorg/ministack:1.4.14 image supports the core KMS operations used above.
// CreateGrant, ListGrants, and RevokeGrant are not supported by this tag, so those tests remain disabled.

// FlociServer — GraalVM Native image and open-source LocalStack replacement
val floci = FlociServer.Launcher.floci
val sqsClient = SqsClient.builder()
    .endpointOverride(floci.awsEndpoint)
    .credentialsProvider(floci.getCredentialProvider())
    .region(Region.of(floci.regionName))
    .build()

// ElasticMqServer — embedded JVM SQS, no Docker required
val elasticMq = ElasticMqServer.Launcher.elasticMq
val sqsClient = SqsClient.builder()
    .endpointOverride(elasticMq.sqsEndpoint)
    .region(Region.of("us-east-1"))
    .credentialsProvider(AnonymousCredentialsProvider.create())
    .build()

// MailpitServer — SMTP + Web UI
val mailpit = MailpitServer.Launcher.mailpit
println("SMTP port: ${mailpit.smtpPort}")
println("Web UI: ${mailpit.uiUrl}")
```

### LLM (ChromaDB + Ollama)

```kotlin
// ChromaDB — vector store for embedding search
val chromaDb = ChromaDBServer.Launcher.chromaDb
println("ChromaDB URL: ${chromaDb.url}")   // http://host:8000

// Ollama — local LLM inference (no GPU required for small models)
val ollama = OllamaServer.Launcher.ollama
println("Ollama URL: ${ollama.url}")       // http://host:11434
```

### Distributed Cache / Grid

```kotlin
// Hazelcast 5.x
val hazelcast = HazelcastServer.Launcher.hazelcast
val client = HazelcastClient.newHazelcastClient(
    ClientConfig().apply { networkConfig.addAddress("${hazelcast.host}:${hazelcast.port}") }
)

// Apache Ignite 2.x — thin client port 10800
val ignite2 = Ignite2Server.Launcher.ignite2
val client = IgniteClient.start(ClientConfiguration().apply {
    setAddresses("${ignite2.host}:${ignite2.port}")
})

// Apache Ignite 3.x — auto cluster-init, thin client port 10800
val ignite3 = Ignite3Server.Launcher.ignite3
val client = IgniteClient.builder()
    .addresses("${ignite3.host}:${ignite3.port}")
    .build()
```

### Distributed SQL

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

### System Property Access

```kotlin
// After start() — read system properties directly
val postgresUrl = System.getProperty("testcontainers.postgresql.jdbc-url")
val kafkaServers = System.getProperty("testcontainers.kafka.bootstrap-servers")

// Register with auto-restore after test
@BeforeEach
fun setup() {
    registration = PostgreSQLServer.Launcher.postgres.registerSystemProperties()
}

@AfterEach
fun cleanup() {
    registration.close()
}
```

## Spring Boot Configuration

Start containers in `@BeforeAll`, then reference properties in `application-test.yml`:

```kotlin
class MyRepositoryTest {
    companion object {
        private val mysql = MySQL8Server(useDefaultPort = true)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mysql.start()  // registers testcontainers.mysql.* system properties
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

## Recent Stability Improvements

- `GenericContainer.exposeCustomPorts(...)` now creates port bindings even when `hostConfig` starts empty.
-

`GenericServer.writeToSystemProperties(...)` registers default and additional properties in a stable, consistent order.

- `KafkaServer.Launcher` creates fresh serializer/deserializer instances per use to avoid reuse after `close()`.
- `TiDBServer` support was removed because Testcontainers 2.x does not support it reliably. Use `MySQL8Server` instead.

## Adding the Dependency

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers:${version}")
}
```

## References

- [Testcontainers](https://www.testcontainers.org/)
- [Floci](https://github.com/floci-io/floci) — GraalVM Native AWS emulator and open-source LocalStack replacement
- [ElasticMQ](https://github.com/softwaremill/elasticmq) — In-memory SQS server
- [Mailpit](https://github.com/axllent/mailpit) — SMTP email testing tool
- [LocalStack](https://www.localstack.cloud/) — @Deprecated: Community edition archived 2026-03-23

## Colima + LocalStack Troubleshooting

When running under Colima, set:

```bash
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
```

If issues persist, restart Colima:

```bash
brew services stop colima
colima stop
rm -f ~/.colima/docker.sock
brew services start colima
```

If Ryuk causes problems (temporary workaround only):

```bash
export TESTCONTAINERS_RYUK_DISABLED=true
```

> **Note**:
> `TESTCONTAINERS_RYUK_DISABLED=true` affects automatic resource cleanup. Use with caution in CI/shared environments.
